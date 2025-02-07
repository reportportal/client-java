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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author <a href="mailto:vadzim_hushchanskou@epam.com">Vadzim Hushchanskou</a>
 */
public class WaiterTest {
	private String description;
	private Waiter waiter;

	@BeforeEach
	public void createWaiter() {
		description = UUID.randomUUID().toString();
		waiter = new Waiter(description).duration(5, TimeUnit.MILLISECONDS).pollingEvery(1, TimeUnit.MILLISECONDS);
	}

	@Test
	public void test_waiter_does_not_fail_by_default() {
		waiter.till((Callable<Boolean>) () -> null);
	}

	@Test
	public void test_waiter_fails_if_requested() {
		assertThrows(InternalReportPortalClientException.class, () -> waiter.timeoutFail().till((Callable<Boolean>) () -> null));
	}

	@Test
	public void test_waiter_fail_description() {
		InternalReportPortalClientException exc = assertThrows(
				InternalReportPortalClientException.class,
				() -> waiter.timeoutFail().till((Callable<Boolean>) () -> null)
		);
		assertThat(exc.getMessage(), equalTo(description + " timed out"));
	}

	@Test
	public void test_waiter_fails_on_unknown_exception() {
		final String errorMessage = "Just a dummy message";
		InternalReportPortalClientException exc = assertThrows(
				InternalReportPortalClientException.class,
				() -> waiter.ignore(IllegalAccessException.class).till((Callable<Boolean>) () -> {
					throw new IllegalArgumentException(errorMessage);
				})
		);
		assertThat(exc.getMessage(), equalTo(errorMessage));
	}

	@Test
	public void test_waiter_does_not_fail_on_known_exception() {
		final String errorMessage = "Just a dummy message 2";
		waiter.ignore(IllegalArgumentException.class).till((Callable<Boolean>) () -> {
			throw new IllegalArgumentException(errorMessage);
		});
	}

	@Test
	public void test_waiter_tries_number() {
		final AtomicInteger counter = new AtomicInteger();
		waiter.duration(6, TimeUnit.MILLISECONDS).pollingEvery(1, TimeUnit.MILLISECONDS).till((Callable<Boolean>) () -> {
			counter.incrementAndGet();
			return null;
		});

		assertThat(counter.get(), is(6));
	}
}
