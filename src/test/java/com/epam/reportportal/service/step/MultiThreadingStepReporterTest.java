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

package com.epam.reportportal.service.step;

import com.epam.reportportal.annotations.Step;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import org.apache.commons.lang3.tuple.Pair;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SuppressWarnings("ReactiveStreamsUnusedPublisher")
public class MultiThreadingStepReporterTest {

	private static final ListenerParameters PARAMS = standardParameters();

	static {
		PARAMS.setBatchLogsSize(5);
	}

	@Mock
	private ReportPortalClient rpClient;

	private final ExecutorService clientExecutorService = Executors.newFixedThreadPool(5);
	// Copy-paste from TestNG executor configuration to reproduce the issue
	private final ExecutorService testExecutorService = new ThreadPoolExecutor(5,
			5,
			10,
			TimeUnit.SECONDS,
			new LinkedBlockingQueue<>(),
			new TestThreadFactory("test_logging_context")
	);

	private ReportPortal rp;

	@BeforeEach
	public void prepare() {
		simulateStartLaunchResponse(rpClient);
		simulateStartTestItemResponse(rpClient);
		simulateStartChildTestItemResponse(rpClient);
		simulateFinishTestItemResponse(rpClient);
		rp = ReportPortal.create(rpClient, PARAMS, clientExecutorService);
	}

	@AfterEach
	public void tearDown() {
		shutdownExecutorService(clientExecutorService);
		shutdownExecutorService(testExecutorService);
	}

	@SuppressWarnings("unused")
	@Step("Step for test `{testNumber}` case `{tryNumber}`")
	public static void step(int testNumber, int tryNumber) {
	}

	private static class UnitTest implements Callable<String> {
		private final Launch launch;
		private final Maybe<String> suiteRs;
		private final int num;

		public UnitTest(Launch l, Maybe<String> s, int testNumber) {
			launch = l;
			suiteRs = s;
			num = testNumber;
		}

		@Override
		public String call() {
			final String itemId = UUID.randomUUID().toString();
			StartTestItemRQ rq = standardStartStepRequest();
			rq.setUuid(itemId);
			rq.setName(rq.getName() + "_" + num);
			Maybe<String> id = launch.startTestItem(suiteRs, rq);
			IntStream.rangeClosed(1, 5).forEach(i -> step(num, ++i));
			launch.finishTestItem(id, positiveFinishRequest());
			return itemId;
		}
	}

	private static class OtherMainTest implements Callable<Maybe<String>> {
		public Launch launch;
		private final ReportPortal rp;

		public OtherMainTest(ReportPortal reportPortal) {
			rp = reportPortal;
		}

		@Override
		public Maybe<String> call() {
			launch = rp.newLaunch(standardLaunchRequest(PARAMS));
			launch.start();
			return launch.startTestItem(standardStartTestRequest());
		}
	}

	public static class TestThreadFactory implements ThreadFactory {

		private final AtomicInteger threadNumber = new AtomicInteger(1);
		private final String name;

		public TestThreadFactory(String name) {
			this.name = "UnitTest" + "-" + name + "-";
		}

		@Override
		public Thread newThread(@Nonnull Runnable r) {
			return new Thread(r, name + threadNumber.getAndIncrement());
		}
	}

	@Test
	public void test_main_and_other_threads_tack_contexts() throws InterruptedException {
		// Main thread starts launch and suite
		final Launch launch = rp.newLaunch(standardLaunchRequest(PARAMS));
		launch.start();
		Maybe<String> suiteRs = launch.startTestItem(standardStartSuiteRequest());
		String suiteId = suiteRs.blockingGet();

		// Other threads start their items and log data
		List<UnitTest> tests = IntStream.range(0, 5).mapToObj(i -> new UnitTest(launch, suiteRs, ++i)).collect(Collectors.toList());
		final List<Future<String>> results = testExecutorService.invokeAll(tests);

		Awaitility.await("Wait until test finish")
				.until(() -> results.stream().filter(Future::isDone).collect(Collectors.toList()), hasSize(5));

		// Verify all item start requests passed
		verify(rpClient, times(1)).startLaunch(any());
		verify(rpClient, times(1)).startTestItem(any());
		ArgumentCaptor<String> parentCapture = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<StartTestItemRQ> stepsCapture = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(rpClient, times(30)).startTestItem(parentCapture.capture(), stepsCapture.capture());

		List<String> parentIds = parentCapture.getAllValues();
		List<StartTestItemRQ> steps = stepsCapture.getAllValues();

		Map<Boolean, List<Pair<String, StartTestItemRQ>>> split = IntStream.range(0, steps.size())
				.mapToObj(i -> Pair.of(parentIds.get(i), steps.get(i)))
				.collect(Collectors.partitioningBy(i -> suiteId.equals(i.getKey())));
		List<Pair<String, StartTestItemRQ>> mainSteps = split.get(Boolean.TRUE);
		List<Pair<String, StartTestItemRQ>> childSteps = split.get(Boolean.FALSE);

		assertThat(mainSteps, hasSize(5));
		assertThat(childSteps, hasSize(25));

		Map<String, List<Pair<String, StartTestItemRQ>>> stepGroups = childSteps.stream().collect(Collectors.groupingBy(Pair::getKey));
		stepGroups.values().forEach(group -> assertThat(group, hasSize(5)));
	}

	@Test
	public void test_only_executor_threads_tack_contexts() throws InterruptedException, ExecutionException {
		// A thread starts launch and suite
		OtherMainTest mainThread = new OtherMainTest(rp);
		Future<Maybe<String>> result = testExecutorService.submit(mainThread);
		Maybe<String> suiteRs = result.get();
		String suiteId = suiteRs.blockingGet();
		final Launch launch = mainThread.launch;

		// Other threads start their items and log data
		List<UnitTest> tests = IntStream.range(0, 5).mapToObj(i -> new UnitTest(launch, suiteRs, ++i)).collect(Collectors.toList());
		final List<Future<String>> results = testExecutorService.invokeAll(tests);

		Awaitility.await("Wait until test finish")
				.until(() -> results.stream().filter(Future::isDone).collect(Collectors.toList()), hasSize(5));

		// Verify all item start requests passed
		verify(rpClient, times(1)).startLaunch(any());
		verify(rpClient, times(1)).startTestItem(any());
		ArgumentCaptor<String> parentCapture = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<StartTestItemRQ> stepsCapture = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(rpClient, times(30)).startTestItem(parentCapture.capture(), stepsCapture.capture());

		List<String> parentIds = parentCapture.getAllValues();
		List<StartTestItemRQ> steps = stepsCapture.getAllValues();

		Map<Boolean, List<Pair<String, StartTestItemRQ>>> split = IntStream.range(0, steps.size())
				.mapToObj(i -> Pair.of(parentIds.get(i), steps.get(i)))
				.collect(Collectors.partitioningBy(i -> suiteId.equals(i.getKey())));
		List<Pair<String, StartTestItemRQ>> mainSteps = split.get(Boolean.TRUE);
		List<Pair<String, StartTestItemRQ>> childSteps = split.get(Boolean.FALSE);

		assertThat(mainSteps, hasSize(5));
		assertThat(childSteps, hasSize(25));

		Map<String, List<Pair<String, StartTestItemRQ>>> stepGroups = childSteps.stream().collect(Collectors.groupingBy(Pair::getKey));
		stepGroups.values().forEach(group -> assertThat(group, hasSize(5)));
	}
}
