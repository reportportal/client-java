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
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Maybe;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static com.epam.reportportal.test.TestUtils.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class ItemLoggingContextMultiThreadTest {

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();

	@Mock
	ReportPortalClient rpClient;

	private ExecutorService clientExecutorService = Executors.newFixedThreadPool(2);
	private ListenerParameters params;
	private ReportPortal rp;

	@Parameterized.Parameters
	public static Object[][] data() {
		return new Object[10][0];
	}

	@Before
	public void prepare() {
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

	@After
	public void cleanUp() {

	}

	private static class TestNgTest extends Thread {
		private Queue<String> itemIds;
		private Launch launch;
		private Maybe<String> suiteRs;

		public TestNgTest(Queue<String> idQueue, Launch l, Maybe<String> s){
			itemIds = idQueue;
			launch = l;
			suiteRs = s;
		}

		public void run() {
			final String itemId = launch.startTestItem(suiteRs, standardStartTestRequest()).blockingGet();
			IntStream.range(1, 11).forEach(i-> {
				ReportPortal.emitLog("Thread message " + i + " log " + itemId, "INFO", new Date());
				try {
					Thread.sleep(ThreadLocalRandom.current().nextInt(10));
				} catch (InterruptedException ignore) {
				}
			});
			itemIds.add(itemId);
		}
	}

	/**
	 * TestNG and other frameworks executes the very first startTestItem call from main thread (start root suite).
	 * Since all other threads are children of the main thread it leads to a situation when all threads share one LoggingContext.
	 * This test is here to ensure that will never happen again: https://github.com/reportportal/agent-java-testNG/issues/76
	 * The test is failing if there is a {@link InheritableThreadLocal} is used in {@link LoggingContext} class.
	 */
	@Test
	public void test_main_and_other_threads_have_different_logging_contexts() {
		// Main thread starts launch and suite
		final Launch launch = rp.newLaunch(standardLaunchRequest(params));
		launch.start();
		Maybe<String> suiteRs = launch.startTestItem(standardStartSuiteRequest());
		String suiteId = suiteRs.blockingGet();

		Queue<String> ids = new ArrayBlockingQueue<>(2);

		// First and second threads start their items and log data
		TestNgTest t1 = new TestNgTest(ids, launch, suiteRs);
		TestNgTest t2 = new TestNgTest(ids, launch, suiteRs);
		t1.start();
		t2.start();

		Awaitility.await("Wait until test finish").until(ids::size, equalTo(2));
		// Verify all item start requests passed
		verify(rpClient, times(1)).startLaunch(any());
		verify(rpClient, times(1)).startTestItem(any());
		verify(rpClient, times(2)).startTestItem(eq(suiteId), any());

		// Verify 2 log are logged and save their requests
		ArgumentCaptor<MultiPartRequest> obtainLogs = ArgumentCaptor.forClass(MultiPartRequest.class);
		verify(rpClient, times(10)).log(obtainLogs.capture());
		obtainLogs.getAllValues().forEach( rq -> {
			((List<SaveLogRQ>) rq.getSerializedRQs().get(0).getRequest()).forEach(log ->{
				String logItemId = log.getItemUuid();
				String logMessage = log.getMessage();
				assertThat("First logItemUUID equals to first test UUID", logMessage, Matchers.endsWith(logItemId));
			});
		});
	}
}
