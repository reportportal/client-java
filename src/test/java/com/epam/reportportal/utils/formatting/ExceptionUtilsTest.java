package com.epam.reportportal.utils.formatting;

import com.epam.reportportal.util.test.CommonUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ExceptionUtilsTest {

	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	public static void test() {
		throw new IllegalStateException("This test is expected to fail");
	}

	public static void test2() {
		throw new IllegalStateException("This test is expected to fail");
	}

	@AfterEach
	public void tearDown() {
		CommonUtils.shutdownExecutorService(executor);
	}

	@Test
	public void test_get_stack_trace() {
		Future<?> task1 = executor.submit(ExceptionUtilsTest::test);
		Future<?> task2 = executor.submit(ExceptionUtilsTest::test2);
		try {
			task1.get();
		} catch (IllegalStateException | InterruptedException | ExecutionException e) {
			try {
				task2.get();
			} catch (IllegalStateException | InterruptedException | ExecutionException e2) {
				String customStackTrace = ExceptionUtils.getStackTrace(e, e2);
				String apacheStackTrace = org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e);

				String[] customFrames = customStackTrace.split("\n");
				String[] apacheFrames = apacheStackTrace.split("\n");

				assertThat(customFrames, arrayWithSize(lessThan(apacheFrames.length)));
				assertThat(customFrames[0], equalTo(apacheFrames[0]));
				assertThat(
						Arrays.stream(customFrames).skip(1).collect(Collectors.toList()),
						everyItem(startsWith(ExceptionUtils.SKIP_TRACE_MARKER))
				);
			}
		}
	}

	@Test
	public void test_get_stack_trace_with_caused_by() {
		Future<?> task1 = executor.submit(ExceptionUtilsTest::test);
		Future<?> task2 = executor.submit(ExceptionUtilsTest::test2);
		try {
			task1.get();
		} catch (IllegalStateException | InterruptedException | ExecutionException e) {
			try {
				task2.get();
			} catch (IllegalStateException | InterruptedException | ExecutionException e2) {
				String customStackTrace = ExceptionUtils.getStackTrace(e, e2, true);
				String apacheStackTrace = org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e);

				String[] customFrames = customStackTrace.split("\n");
				String[] apacheFrames = apacheStackTrace.split("\n");

				assertThat(customFrames, arrayWithSize(lessThan(apacheFrames.length)));
				assertThat(customFrames[0], equalTo(apacheFrames[0]));
				assertThat(customStackTrace, containsString("Caused by:"));
			}
		}
	}
}
