/*
 *  Copyright 2021 EPAM Systems
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.epam.reportportal.service.launch.lock;

import com.epam.reportportal.service.LaunchIdLock;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class LockTestUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(LockTestUtil.class);

	public static final long LOCK_TIMEOUT = TimeUnit.SECONDS.toMillis(10);

	public static final String WELCOME_MESSAGE = "Lock ready, press any key to continue...";

	public static final Predicate<String> WELCOME_MESSAGE_PREDICATE = WELCOME_MESSAGE::equals;
	public static final Predicate<String> ANY_STRING_PREDICATE = StringUtils::isNotBlank;

	public static Callable<String> getObtainLaunchUuidReadCallable(final String selfUuid,
			final LaunchIdLock launchIdLock) {
		return () -> launchIdLock.obtainLaunchUuid(selfUuid);
	}

	public static final class GetFutureResults<T> implements Function<Future<T>, T> {
		@Override
		public T apply(Future<T> input) {
			try {
				return input.get();
			} catch (InterruptedException e) {
				LOGGER.error("Interrupted: ", e);
			} catch (ExecutionException e) {
				LOGGER.error("Failed: ", e);
			}
			return null;
		}
	}

	public static <T extends LaunchIdLock> Map<String, Callable<String>> getLaunchUuidReadCallables(int num,
			Supplier<T> serviceProvider) {
		Map<String, Callable<String>> results = new HashMap<>();
		for (int i = 0; i < num; i++) {
			String uuid = UUID.randomUUID().toString();
			Callable<String> task = getObtainLaunchUuidReadCallable(uuid, serviceProvider.get());
			results.put(uuid, task);
		}
		return results;
	}

	public static <T> Supplier<T> singletonSupplier(final T value) {
		return () -> value;
	}

	public static ExecutorService testExecutor(final int threadNum) {
		return Executors.newFixedThreadPool(threadNum, r -> {
			Thread t = Executors.defaultThreadFactory().newThread(r);
			t.setDaemon(true);
			return t;
		});
	}

	public static <T> Supplier<T> iterableSupplier(final Iterable<T> instanceIterable) {
		return new Supplier<T>() {
			private final Iterator<T> instanceIterator = instanceIterable.iterator();

			@Override
			public T get() {
				return instanceIterator.next();
			}
		};
	}
}
