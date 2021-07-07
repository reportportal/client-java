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
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.join;
import static org.hamcrest.Matchers.notNullValue;

public class LockTestUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(LockTestUtil.class);

	public static final long LOCK_TIMEOUT = TimeUnit.SECONDS.toMillis(10);

	public static final String WELCOME_MESSAGE = "Lock ready, press any key to continue...";

	public static final Predicate<String> WELCOME_MESSAGE_PREDICATE = WELCOME_MESSAGE::equals;
	public static final Predicate<String> ANY_STRING_PREDICATE = input -> !isEmpty(input);

	public static Triple<OutputStreamWriter, BufferedReader, BufferedReader> getProcessIos(Process process) {
		return ImmutableTriple.of(
				new OutputStreamWriter(process.getOutputStream()),
				new BufferedReader(new InputStreamReader(process.getInputStream())),
				new BufferedReader(new InputStreamReader(process.getErrorStream()))
		);
	}

	public static void closeIos(Triple<OutputStreamWriter, BufferedReader, BufferedReader> io) {
		try {
			io.getLeft().close();
			io.getMiddle().close();
			io.getRight().close();
		} catch (IOException ignore) {

		}
	}

	@SuppressWarnings("unchecked")
	public static String waitForLine(final BufferedReader reader, final BufferedReader errorReader, final Predicate<String> linePredicate)
			throws IOException {
		try {
			return Awaitility.await("Waiting for a line")
					.timeout(8, TimeUnit.SECONDS)
					.pollInterval(100, TimeUnit.MILLISECONDS)
					.until(() -> {
						if (!reader.ready()) {
							return null;
						}
						String line;
						while ((line = reader.readLine()) != null) {
							if (linePredicate.test(line)) {
								return line;
							}
						}
						return null;
					}, notNullValue());
		} catch (ConditionTimeoutException e) {
			List<String> errorLines = Collections.EMPTY_LIST;
			if (errorReader.ready()) {
				errorLines = IOUtils.readLines(errorReader);
			}
			String lineSeparator = System.getProperty("line.separator");
			throw new IllegalStateException("Unable to run test class: " + join(errorLines, lineSeparator));
		}
	}

	public static Callable<String> getObtainLaunchUuidReadCallable(final String selfUuid, final LaunchIdLock launchIdLock) {
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

	public static <T extends LaunchIdLock> Map<String, Callable<String>> getLaunchUuidReadCallables(int num, Supplier<T> serviceProvider) {
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
