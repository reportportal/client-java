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
import com.epam.reportportal.restendpoint.http.MultiPartRequest;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Maybe;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.epam.reportportal.test.TestUtils.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ItemLoggingContextMultiThreadTest {

	@Mock
	private ReportPortalClient rpClient;
	private ExecutorService clientExecutorService;
	private ListenerParameters params;
	private ReportPortal rp;

	@BeforeEach
	public void prepare() {
		clientExecutorService = Executors.newFixedThreadPool(2);
		params = new ListenerParameters();
		params.setEnable(Boolean.TRUE);
		params.setClientJoin(false);
		params.setBatchLogsSize(2);
		simulateStartLaunchResponse(rpClient);
		simulateStartTestItemResponse(rpClient);
		simulateStartChildTestItemResponse(rpClient);
		simulateBatchLogResponse(rpClient);
		rp = new ReportPortal(rpClient, clientExecutorService, params, null);
	}

	@AfterEach
	public void tearDown() {
		clientExecutorService.shutdownNow();
	}

	private static class TestNgTest implements Callable<String> {
		private Launch launch;
		private Maybe<String> suiteRs;

		public TestNgTest(Launch l, Maybe<String> s) {
			launch = l;
			suiteRs = s;
		}

		@Override
		public String call() {
			final String itemId = UUID.randomUUID().toString();
			StartTestItemRQ rq = standardStartTestRequest();
			rq.setUuid(itemId);
			launch.startTestItem(suiteRs, rq);
			IntStream.rangeClosed(1, 10).forEach(i -> {
				ReportPortal.emitLog("[" + i + "] log " + itemId, "INFO", new Date());
				try {
					Thread.sleep(ThreadLocalRandom.current().nextInt(10));
				} catch (InterruptedException ignore) {
				}
			});
			return itemId;
		}
	}

	public static class TestNGThreadFactory implements ThreadFactory {

		private final AtomicInteger threadNumber = new AtomicInteger(1);
		private final String name;

		public TestNGThreadFactory(String name) {
			this.name = "UnitTest" + "-" + name + "-";
		}

		@Override
		public Thread newThread(Runnable r) {
			return new Thread(r, name + threadNumber.getAndIncrement());
		}
	}

	/**
	 * TestNG and other frameworks executes the very first startTestItem call from main thread (start root suite).
	 * Since all other threads are children of the main thread it leads to a situation when all threads share one LoggingContext.
	 * This test is here to ensure that will never happen again: https://github.com/reportportal/agent-java-testNG/issues/76
	 * The test is failing if there is a {@link InheritableThreadLocal} is used in {@link LoggingContext} class.
	 */
	@Test
	public void test_main_and_other_threads_have_different_logging_contexts() throws InterruptedException {
		// Main thread starts launch and suite
		final Launch launch = rp.newLaunch(standardLaunchRequest(params));
		launch.start();
		Maybe<String> suiteRs = launch.startTestItem(standardStartSuiteRequest());

		// Copy-paste from TestNG to reproduce the issue
		ExecutorService pooledExecutor = new ThreadPoolExecutor(2,
				2,
				10,
				TimeUnit.SECONDS,
				new LinkedBlockingQueue<>(),
				new TestNGThreadFactory("test_logging_context")
		);



		// First and second threads start their items and log data
		TestNgTest t1 = new TestNgTest(launch, suiteRs);
		TestNgTest t2 = new TestNgTest(launch, suiteRs);
		final List<Future<String>> results = pooledExecutor.invokeAll(Arrays.asList(t1, t2));

		Awaitility.await("Wait until test finish").until(() -> results.stream().filter(Future::isDone).collect(Collectors.toList()), hasSize(2));

		// Verify all item start requests passed
		verify(rpClient, times(1)).startLaunch(any());
		verify(rpClient, times(1)).startTestItem(any());
		verify(rpClient, times(2)).startTestItem(anyString(), any());

		// Verify 10 log are logged and save their requests
		ArgumentCaptor<MultiPartRequest> obtainLogs = ArgumentCaptor.forClass(MultiPartRequest.class);
		verify(rpClient, times(10)).log(obtainLogs.capture());
		obtainLogs.getAllValues().forEach(rq -> {
			rq.getSerializedRQs().forEach(rqm -> ((List<SaveLogRQ>) rqm.getRequest()).forEach(log -> {
				String logItemId = log.getItemUuid();
				String logMessage = log.getMessage();
				assertThat("First logItemUUID equals to first test UUID", logMessage, Matchers.endsWith(logItemId));
			}));
		});
	}
}
