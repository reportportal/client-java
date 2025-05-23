/*
 * Copyright 2025 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.reportportal.service;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.listeners.LogLevel;
import com.epam.reportportal.test.TestUtils;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.BatchSaveOperatingRS;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Maybe;
import okhttp3.MultipartBody;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class LaunchLogCompletionTest {

	private ExecutorService executor;
	private ListenerParameters parameters;

	@BeforeEach
	public void setUp() {
		parameters = TestUtils.standardParameters();
		parameters.setBatchLogsSize(3); // Small batch size for testing
		executor = Executors.newSingleThreadExecutor();
	}

	@AfterEach
	public void tearDown() {
		CommonUtils.shutdownExecutorService(executor);
	}

	@Test
	@Timeout(value = 30, unit = TimeUnit.SECONDS)
	@SuppressWarnings("unchecked")
	public void test_launch_finish_waits_for_log_completion() throws InterruptedException {
		ReportPortalClient client = mock(ReportPortalClient.class);
		TestUtils.mockLaunch(client, "launchUuid");

		// Track the order of operations
		AtomicInteger operationOrder = new AtomicInteger(0);
		AtomicInteger logResponseTime = new AtomicInteger(-1);
		AtomicInteger finishStartTime = new AtomicInteger(-1);
		AtomicInteger finishEndTime = new AtomicInteger(-1);

		when(client.log(any(List.class))).thenAnswer(invocation -> {
			return Maybe.fromCallable(() -> {
				// Simulate network delay for log processing
				Thread.sleep(300); // Simulate network delay
				logResponseTime.set(operationOrder.incrementAndGet());
				return new BatchSaveOperatingRS();
			});
		});

		Launch launch = new LaunchImpl(client, parameters, TestUtils.standardLaunchRequest(parameters), executor);
		launch.start().blockingGet();

		// Emit logs that will create at least one batch
		for (int i = 0; i < 3; i++) {
			final int logIndex = i;
			launch.log(launchUuid -> {
				SaveLogRQ rq = new SaveLogRQ();
				rq.setLaunchUuid(launchUuid);
				rq.setLevel(LogLevel.INFO.name());
				rq.setLogTime(Calendar.getInstance().getTime());
				rq.setMessage("Test log " + logIndex);
				return rq;
			});
		}

		// Give some time for logs to be emitted
		Thread.sleep(100);

		// Record when finish starts
		finishStartTime.set(operationOrder.incrementAndGet());

		// Finish the launch - this should wait for all logs to be processed
		launch.finish(TestUtils.positiveFinishRequest());

		// Record when finish ends
		finishEndTime.set(operationOrder.incrementAndGet());

		// Verify that log response happened between finish start and finish end
		assertThat("Log response should have completed", logResponseTime.get(), greaterThan(0));
		assertThat("Log should have started before finish started", finishStartTime.get(), greaterThan(0));
		assertThat("Finish should have waited for log response", logResponseTime.get(), greaterThan(finishStartTime.get()));
		assertThat("Finish should have completed after log response", finishEndTime.get(), greaterThan(logResponseTime.get()));

		// Verify log call was made
		verify(client, atLeastOnce()).log(any(List.class));
	}

	@Test
	@Timeout(value = 30, unit = TimeUnit.SECONDS)
	@SuppressWarnings("unchecked")
	public void test_launch_finish_without_logs_completes_quickly() {
		ReportPortalClient client = mock(ReportPortalClient.class);
		TestUtils.mockLaunch(client, "launchUuid");

		Launch launch = new LaunchImpl(client, parameters, TestUtils.standardLaunchRequest(parameters), executor);
		launch.start().blockingGet();

		// Finish the launch without any logs
		long startTime = System.currentTimeMillis();
		launch.finish(TestUtils.positiveFinishRequest());
		long endTime = System.currentTimeMillis();

		// Verify that finish() completed quickly since there were no logs
		long duration = endTime - startTime;
		assertThat("Launch finish should complete quickly without logs", duration, lessThan(1000L));

		// Verify no log calls were made
		verify(client, never()).log(any(List.class));
	}

	@Test
	@Timeout(value = 30, unit = TimeUnit.SECONDS)
	@SuppressWarnings("unchecked")
	public void test_launch_finish_handles_log_errors_gracefully() throws InterruptedException {
		ReportPortalClient client = mock(ReportPortalClient.class);
		TestUtils.mockLaunch(client, "launchUuid");

		// Simulate log failure
		when(client.log(any(List.class))).thenReturn(Maybe.error(new RuntimeException("Network error")));

		Launch launch = new LaunchImpl(client, parameters, TestUtils.standardLaunchRequest(parameters), executor);
		launch.start().blockingGet();

		// Emit some logs
		for (int i = 0; i < 3; i++) {
			final int logIndex = i;
			launch.log(launchUuid -> {
				SaveLogRQ rq = new SaveLogRQ();
				rq.setLaunchUuid(launchUuid);
				rq.setLevel(LogLevel.INFO.name());
				rq.setLogTime(Calendar.getInstance().getTime());
				rq.setMessage("Test log " + logIndex);
				return rq;
			});
		}

		// Finish the launch - this should handle log errors gracefully
		long startTime = System.currentTimeMillis();
		launch.finish(TestUtils.positiveFinishRequest());
		long endTime = System.currentTimeMillis();

		// Verify that finish() completed (even with log errors)
		long duration = endTime - startTime;
		assertThat("Launch finish should complete despite log errors", duration, lessThan(5000L));

		// Verify log call was attempted
		verify(client, atLeastOnce()).log(any(List.class));
	}

	@Test
	@Timeout(value = 30, unit = TimeUnit.SECONDS)
	@SuppressWarnings("unchecked")
	public void test_virtual_item_logs_wait_for_completion() {
		ReportPortalClient client = mock(ReportPortalClient.class);
		TestUtils.mockLaunch(client, "launchUuid");
		TestUtils.mockStartTestItem(client, "itemUuid");

		// Track the order of operations for virtual item logs
		AtomicInteger operationOrder = new AtomicInteger(0);
		AtomicInteger logResponseTime = new AtomicInteger(-1);
		AtomicInteger finishStartTime = new AtomicInteger(-1);
		AtomicInteger finishEndTime = new AtomicInteger(-1);

		when(client.log(any(List.class))).thenAnswer(invocation -> {
			return Maybe.fromCallable(() -> {
				// Simulate network delay for log processing
				Thread.sleep(200); // Simulate network delay
				logResponseTime.set(operationOrder.incrementAndGet());
				return new BatchSaveOperatingRS();
			});
		});

		LaunchImpl launch = new LaunchImpl(client, parameters, TestUtils.standardLaunchRequest(parameters), executor);
		launch.start().blockingGet();

		// Create a virtual item
		Maybe<String> virtualItem = launch.createVirtualItem();

		// Log to the virtual item using ReportPortal.emitLog (this goes through LoggingContext)
		ReportPortal.emitLog(virtualItem, itemUuid -> {
			SaveLogRQ rq = new SaveLogRQ();
			rq.setItemUuid(itemUuid);
			rq.setLevel(LogLevel.INFO.name());
			rq.setLogTime(Calendar.getInstance().getTime());
			rq.setMessage("Virtual item log");
			return rq;
		});

		// Populate the virtual item
		launch.startVirtualTestItem(virtualItem, TestUtils.standardStartStepRequest());

		// Give some time for logs to be processed
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		// Record when finish starts
		finishStartTime.set(operationOrder.incrementAndGet());

		// Finish the launch - this should wait for virtual item logs to complete
		launch.finish(TestUtils.positiveFinishRequest());

		// Record when finish ends
		finishEndTime.set(operationOrder.incrementAndGet());

		// Verify that log response happened and finish waited for it
		assertThat("Log response should have completed", logResponseTime.get(), greaterThan(0));
		assertThat("Finish should have waited for virtual item log response", logResponseTime.get(), greaterThan(finishStartTime.get()));
		assertThat("Finish should have completed after log response", finishEndTime.get(), greaterThan(logResponseTime.get()));

		// Verify log call was made
		verify(client, atLeastOnce()).log(any(List.class));
	}
} 