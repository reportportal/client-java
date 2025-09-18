/*
 *  Copyright 2020 EPAM Systems
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

package com.epam.reportportal.service;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.test.TestUtils;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import jakarta.annotation.Nonnull;
import okhttp3.MultipartBody;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.epam.reportportal.test.TestUtils.*;
import static com.epam.reportportal.util.test.CommonUtils.shutdownExecutorService;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("ReactiveStreamsUnusedPublisher")
public class ItemLoggingContextMultiThreadTest {

	private static final ListenerParameters PARAMS = standardParameters();

	static {
		PARAMS.setBatchLogsSize(2);
	}

	@Mock
	private ReportPortalClient rpClient;
	private final ExecutorService clientExecutorService = Executors.newFixedThreadPool(2);
	// Copy-paste from TestNG executor configuration to reproduce the issue
	private final ExecutorService testExecutorService = new ThreadPoolExecutor(
			2,
			2,
			10,
			TimeUnit.SECONDS,
			new LinkedBlockingQueue<>(),
			new TestNGThreadFactory("test_logging_context")
	);

	private ReportPortal rp;

	@BeforeEach
	public void prepare() {
		simulateStartLaunchResponse(rpClient);
		simulateStartTestItemResponse(rpClient);
		simulateStartChildTestItemResponse(rpClient);
		simulateBatchLogResponse(rpClient);
		simulateFinishTestItemResponse(rpClient);
		rp = ReportPortal.create(rpClient, PARAMS, clientExecutorService);
	}

	@AfterEach
	public void tearDown() {
		shutdownExecutorService(clientExecutorService);
		shutdownExecutorService(testExecutorService);
	}

	private static class TestNgTest implements Callable<String> {

		private final Launch launch;
		private final Maybe<String> suiteRs;

		public TestNgTest(Launch l, Maybe<String> s) {
			launch = l;
			suiteRs = s;
		}

		@Override
		public String call() {
			final String itemId = UUID.randomUUID().toString();
			StartTestItemRQ rq = standardStartTestRequest();
			rq.setUuid(itemId);
			Maybe<String> id = launch.startTestItem(suiteRs, rq);
			work(itemId);
			launch.finishTestItem(id, positiveFinishRequest());
			return itemId;
		}

		public void work(String itemId) {
			IntStream.rangeClosed(1, 10).forEach(i -> {
				ReportPortal.emitLog("[" + i + "] log " + itemId, "INFO", new Date());
				try {
					Thread.sleep(ThreadLocalRandom.current().nextInt(10));
				} catch (InterruptedException ignore) {
				}
			});
		}
	}

	private static class TestNgTimeoutTest extends TestNgTest {

		public TestNgTimeoutTest(Launch l, Maybe<String> s) {
			super(l, s);
		}

		@Override
		public void work(String itemId) {
			ExecutorService timeoutThread = Executors.newSingleThreadExecutor();
			Future<?> timeoutFuture = timeoutThread.submit(() -> super.work(itemId));
			try {
				timeoutFuture.get();
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
			CommonUtils.shutdownExecutorService(timeoutThread);
		}
	}

	public static class TestNGThreadFactory implements ThreadFactory {

		private final AtomicInteger threadNumber = new AtomicInteger(1);
		private final String name;

		public TestNGThreadFactory(String name) {
			this.name = "UnitTest" + "-" + name + "-";
		}

		@Override
		public Thread newThread(@Nonnull Runnable r) {
			return new Thread(r, name + threadNumber.getAndIncrement());
		}
	}

	@SuppressWarnings("unchecked")
	private void performTest(List<TestNgTest> tests) throws InterruptedException {
		final List<Future<String>> results = testExecutorService.invokeAll(tests);

		Awaitility.await("Wait until test finish")
				.until(() -> results.stream().filter(Future::isDone).collect(Collectors.toList()), hasSize(2));

		// Verify all item start requests passed
		verify(rpClient, times(1)).startLaunch(any());
		verify(rpClient, times(1)).startTestItem(any());
		verify(rpClient, times(2)).startTestItem(anyString(), any());

		// Verify 10 log are logged and save their requests
		ArgumentCaptor<List<MultipartBody.Part>> obtainLogs = ArgumentCaptor.forClass(List.class);
		verify(rpClient, times(10)).log(obtainLogs.capture());
		obtainLogs.getAllValues().stream().flatMap(rq -> TestUtils.extractJsonParts(rq).stream()).forEach(log -> {
			String logItemId = log.getItemUuid();
			String logMessage = log.getMessage();
			assertThat("First logItemUUID equals to first test UUID", logMessage, Matchers.endsWith(logItemId));
		});
	}

	/**
	 * TestNG and other frameworks executes the very first startTestItem call from main thread (start root suite).
	 * Since all other threads are children of the main thread it leads to a situation when all threads share one LoggingContext.
	 * This test is here to ensure that the following issue will never happen again:
	 * <a href="https://github.com/reportportal/agent-java-testNG/issues/76">#76</a>
	 * The test is failing if there is a {@link InheritableThreadLocal} is used in {@link LoggingContext} class.
	 */
	@Test
	public void test_main_and_other_threads_have_different_logging_contexts() throws InterruptedException {
		// Main thread starts launch and suite
		final Launch launch = rp.newLaunch(standardLaunchRequest(PARAMS));
		launch.start();
		Maybe<String> suiteRs = launch.startTestItem(standardStartSuiteRequest());

		// First and second threads start their items and log data
		TestNgTest t1 = new TestNgTest(launch, suiteRs);
		TestNgTest t2 = new TestNgTest(launch, suiteRs);
		performTest(Arrays.asList(t1, t2));
	}

	@Test
	public void test_a_test_with_timeout_has_the_same_logging_context_as_parent() throws InterruptedException {
		// Main thread starts launch and suite
		final Launch launch = rp.newLaunch(standardLaunchRequest(PARAMS));
		launch.start();
		Maybe<String> suiteRs = launch.startTestItem(standardStartSuiteRequest());

		// First and second threads start their items and log data
		TestNgTest t1 = new TestNgTest(launch, suiteRs);
		TestNgTest t2 = new TestNgTimeoutTest(launch, suiteRs);
		performTest(Arrays.asList(t1, t2));
	}
}
