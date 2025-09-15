/*
 *  Copyright 2019 EPAM Systems
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

package com.epam.reportportal.utils;

import com.epam.reportportal.exception.InternalReportPortalClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * The simplest waiter, just to not include a new dependency into the project.
 *
 * @author <a href="mailto:vadzim_hushchanskou@epam.com">Vadzim Hushchanskou</a>
 */
public class Waiter {
	private static final Logger LOGGER = LoggerFactory.getLogger(Waiter.class);

	private static final ThreadLocal<Random> RANDOM = ThreadLocal.withInitial(Random::new);

	private final String waitDescription;

	private long durationNs = TimeUnit.MINUTES.toNanos(1);
	private long pollingNs = TimeUnit.MILLISECONDS.toNanos(100);
	private final List<Class<? extends Throwable>> ignoreExceptions = new ArrayList<>();
	private boolean useDiscrepancy = false;
	private float discrepancy = 0.0f;
	private double maxDiscrepancyNs = pollingNs * discrepancy;
	private boolean failOnTimeout = false;

	public Waiter(String description) {
		waitDescription = description;
	}

	public Waiter duration(final long duration, TimeUnit timeUnit) {
		assert duration >= 0;
		durationNs = timeUnit.toNanos(duration);
		return this;
	}

	public Waiter pollingEvery(final long duration, TimeUnit timeUnit) {
		assert duration > 0;
		pollingNs = timeUnit.toNanos(duration);
		return useDiscrepancy ? applyRandomDiscrepancy(discrepancy) : this;
	}

	public Waiter ignore(Class<? extends Throwable> exception) {
		assert exception != null;
		ignoreExceptions.add(exception);
		return this;
	}

	public Waiter applyRandomDiscrepancy(float maximumDiscrepancy) {
		assert maximumDiscrepancy <= 1.0f && maximumDiscrepancy >= 0.0f;
		discrepancy = maximumDiscrepancy;
		useDiscrepancy = maximumDiscrepancy > 0.0f;
		maxDiscrepancyNs = pollingNs * discrepancy;
		return this;
	}

	public Waiter timeoutFail() {
		failOnTimeout = true;
		return this;
	}

	private long getDiscrepancy() {
		if (!useDiscrepancy) {
			return 0;
		}
		Random random = RANDOM.get();
		double absoluteDiscrepancy = maxDiscrepancyNs * random.nextDouble();
		double discrepancy = random.nextBoolean() ? absoluteDiscrepancy : -absoluteDiscrepancy;
		return (long) discrepancy;
	}

	private boolean knownException(Exception e) {
		for (Class<? extends Throwable> known : ignoreExceptions) {
			if (known.isAssignableFrom(e.getClass())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Waits until the supplied callable returns a non-null result, polling at the configured interval,
	 * or until the wait times out or the thread is interrupted.
	 * <p>
	 * Important: {@code false} is still a non-null value for {@link Boolean} and will stop waiting
	 * as a successful result.
	 * <p>
	 * Exceptions matching those configured via {@link #ignore(Class)} are swallowed and the wait is retried;
	 * any other exception aborts the wait and is wrapped into {@link InternalReportPortalClientException}.
	 * <p>
	 * If the timeout elapses:
	 * <ul>
	 *   <li>when {@link #timeoutFail()} was configured, an {@link InternalReportPortalClientException} is thrown;</li>
	 *   <li>otherwise, {@code null} is returned.</li>
	 * </ul>
	 * If the thread is interrupted, a warning is logged and {@code null} is returned.
	 *
	 * @param waitFor a callable that is evaluated repeatedly until it returns a non-null result
	 * @param <T>     type of the result supplied by the callable
	 * @return the first non-null result returned by the callable; {@code null} on timeout/interruption when not configured to fail
	 * @throws InternalReportPortalClientException if an unexpected exception occurs in the callable
	 *                                             or if timeout occurs and {@link #timeoutFail()} was configured
	 */
	public <T> T till(Callable<T> waitFor) {
		long triesLong = durationNs / pollingNs;
		int tries = triesLong > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) triesLong;
		CountDownLatch countDown = new CountDownLatch(tries);
		try {
			do {
				try {
					T result = waitFor.call();
					if (result != null) {
						return result;
					}
				} catch (Exception e) {
					if (!knownException(e)) {
						LOGGER.error("An exception caught while waiting for a result: " + e.getLocalizedMessage(), e);
						throw new InternalReportPortalClientException(e.getLocalizedMessage(), e);
					}
					LOGGER.trace("A known exception caught while waiting for a result: " + e.getLocalizedMessage(), e);
				}
				countDown.countDown();
			} while (!countDown.await(pollingNs + getDiscrepancy(), TimeUnit.NANOSECONDS));
			// timeout happened
			if (failOnTimeout) {
				throw new InternalReportPortalClientException(waitDescription + " timed out");
			}
		} catch (InterruptedException ignored) {
			// someone just interrupted our thread, normally exit
			LOGGER.warn(waitDescription + " was interrupted");
		}
		return null;
	}
}
