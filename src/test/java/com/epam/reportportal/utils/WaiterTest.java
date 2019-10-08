package com.epam.reportportal.utils;

import com.epam.reportportal.exception.InternalReportPortalClientException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class WaiterTest {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	private String description;
	private Waiter waiter;

	@Before
	public void createWaiter() {
		description = UUID.randomUUID().toString();
		waiter = new Waiter(description).duration(5, TimeUnit.MILLISECONDS).pollingEvery(1, TimeUnit.MILLISECONDS);
	}

	@Test
	public void test_waiter_does_not_fail_by_default(){
		waiter.till(new Callable<Boolean>() {
			@Override
			public Boolean call() {
				return null;
			}
		});
	}

	@Test
	public void test_waiter_fails_if_requested(){
		exception.expect(InternalReportPortalClientException.class);
		waiter.timeoutFail().till(new Callable<Boolean>() {
			@Override
			public Boolean call() {
				return null;
			}
		});
	}

	@Test
	public void test_waiter_fail_description() {
		exception.expect(InternalReportPortalClientException.class);
		exception.expectMessage(description + " timed out");
		waiter.timeoutFail().till(new Callable<Boolean>() {
			@Override
			public Boolean call() {
				return null;
			}
		});
	}

	@Test
	public void test_waiter_fails_on_unknown_exception() {
		final String errorMessage = "Just a dummy message";
		exception.expect(InternalReportPortalClientException.class);
		exception.expectMessage(errorMessage);
		waiter.ignore(IllegalAccessException.class).till(new Callable<Boolean>() {
			@Override
			public Boolean call() {
				throw new IllegalArgumentException(errorMessage);
			}
		});

	}

	@Test
	public void test_waiter_does_not_fail_on_known_exception() {
		final String errorMessage = "Just a dummy message 2";
		waiter.ignore(IllegalArgumentException.class).till(new Callable<Boolean>() {
			@Override
			public Boolean call() {
				throw new IllegalArgumentException(errorMessage);
			}
		});
	}

	@Test
	public void test_waiter_tries_number() {
		final AtomicInteger counter = new AtomicInteger();
		waiter.duration(6, TimeUnit.MILLISECONDS).pollingEvery(1, TimeUnit.MILLISECONDS).till(new Callable<Boolean>() {
			@Override
			public Boolean call() {
				counter.incrementAndGet();
				return null;
			}
		});

		assertThat(counter.get(), is(6));
	}

	@Test
	public void test() {

	}
}
