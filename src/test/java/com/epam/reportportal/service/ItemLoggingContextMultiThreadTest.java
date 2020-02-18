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
import io.reactivex.Maybe;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.epam.reportportal.test.TestUtils.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ItemLoggingContextMultiThreadTest {

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();

	@Mock
	ReportPortalClient rpClient;

	private ExecutorService clientExecutorService = Executors.newSingleThreadExecutor();
	private ListenerParameters params;
	private ReportPortal rp;

	@Before
	public void prepare() {
		params = new ListenerParameters();
		params.setEnable(Boolean.TRUE);
		params.setClientJoin(false);
		simulateStartLaunchResponse(rpClient);
		simulateStartTestItemResponse(rpClient);
		simulateLogResponse(rpClient);
		rp = new ReportPortal(rpClient, clientExecutorService, params, null);
	}

	@After
	public void cleanUp() {

	}

	/**
	 * TestNG and other frameworks executes the very first startTestItem call from main thread (start root suite).
	 * Since all other threads are children of the main thread it leads to a situation when all threads share one LoggingContext.
	 * This test is here to ensure that will never happen again: https://github.com/reportportal/agent-java-testNG/issues/76
	 */
	@Test
	public void test_main_and_other_threads_have_different_logging_contexts() throws ExecutionException, InterruptedException {
		// Main thread starts launch and suite
		Launch launch = rp.newLaunch(standardLaunchRequest(params));
		Maybe<String> launchId = launch.start();
		Maybe<String> suiteRs = launch.startTestItem(standardStartSuiteRequest());
		String suiteId = suiteRs.blockingGet();

		// First thread starts its item and logs data
		ExecutorService firstThreadExecutor = Executors.newSingleThreadExecutor();
		Maybe<String> firstItemRs = firstThreadExecutor.submit(() -> launch.startTestItem(suiteRs, standardStartTestRequest())).get();
		firstThreadExecutor.submit(() -> ReportPortal.emitLog("First thread message", "INFO", new Date())).get();

		// Second thread starts its item and logs data
		ExecutorService secondThreadExecutor = Executors.newSingleThreadExecutor();
		Maybe<String> secondItemRs = secondThreadExecutor.submit(() -> launch.startTestItem(suiteRs, standardStartTestRequest())).get();
		secondThreadExecutor.submit(() -> ReportPortal.emitLog("Second thread message", "INFO", new Date())).get();

		verify(rpClient, times(1)).startLaunch(any());
		verify(rpClient, times(1)).startTestItem(any());
		verify(rpClient, times(2)).startTestItem(eq(suiteId), any());
	}
}
