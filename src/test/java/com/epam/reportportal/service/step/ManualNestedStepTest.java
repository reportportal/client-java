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

package com.epam.reportportal.service.step;

import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.test.TestUtils;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.BatchSaveOperatingRS;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import okhttp3.MultipartBody;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.epam.reportportal.test.TestUtils.*;
import static com.epam.reportportal.utils.SubscriptionUtils.createConstantMaybe;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ManualNestedStepTest {

	private final String testLaunchUuid = CommonUtils.namedId("launch_");
	private final String testClassUuid = CommonUtils.namedId("class_");
	private final String testMethodUuid = CommonUtils.namedId("test_");
	private final List<String> nestedSteps = Stream.generate(() -> CommonUtils.namedId("nested_")).limit(2).collect(Collectors.toList());
	private final List<Pair<String, String>> nestedStepPairs = nestedSteps.stream()
			.map(s -> Pair.of(testMethodUuid, s))
			.collect(Collectors.toList());
	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	private ReportPortalClient client;
	private Launch launch;
	private Maybe<String> testClassUuidMaybe;
	private Maybe<String> testMethodUuidMaybe;
	private StepReporter sr;

	@BeforeEach
	public void initMocks() {
		client = mock(ReportPortalClient.class);
		mockLaunch(client, testLaunchUuid, testClassUuid, testMethodUuid);

		ReportPortal rp = ReportPortal.create(client, TestUtils.STANDARD_PARAMETERS, executor);
		launch = rp.newLaunch(TestUtils.standardLaunchRequest(TestUtils.standardParameters()));
		testClassUuidMaybe = launch.startTestItem(TestUtils.standardStartTestRequest());
		testMethodUuidMaybe = launch.startTestItem(testClassUuidMaybe, TestUtils.standardStartStepRequest());
		sr = launch.getStepReporter();
	}

	@AfterEach
	public void cleanup() {
		shutdownExecutorService(executor);
	}

	@Test
	public void test_sent_step_creates_nested_step() {
		mockNestedSteps(client, nestedStepPairs.get(0));
		String stepName = UUID.randomUUID().toString();
		sr.sendStep(stepName);

		ArgumentCaptor<StartTestItemRQ> stepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, timeout(1000)).startTestItem(eq(testMethodUuid), stepCaptor.capture());
		verify(client, after(1000).times(0)).finishTestItem(eq(testMethodUuid), any());

		StartTestItemRQ nestedStep = stepCaptor.getValue();
		assertThat(nestedStep.getName(), equalTo(stepName));
		assertThat(nestedStep.isHasStats(), equalTo(Boolean.FALSE));
		sr.finishPreviousStep();
		launch.finishTestItem(testMethodUuidMaybe, positiveFinishRequest());
		launch.finishTestItem(testClassUuidMaybe, positiveFinishRequest());
		launch.finish(standardLaunchFinishRequest());
	}

	@Test
	public void verify_two_nested_steps_report_on_the_same_level() {
		mockNestedSteps(client, nestedStepPairs);
		String stepName = UUID.randomUUID().toString();
		sr.sendStep(stepName);

		String stepName2 = UUID.randomUUID().toString();
		sr.sendStep(stepName2);

		ArgumentCaptor<StartTestItemRQ> stepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, after(1000).times(2)).startTestItem(eq(testMethodUuid), stepCaptor.capture());

		ArgumentCaptor<String> finishStepUuidCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<FinishTestItemRQ> finishStepCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, after(1000).times(1)).finishTestItem(finishStepUuidCaptor.capture(), finishStepCaptor.capture());

		List<StartTestItemRQ> nestedStepsRqs = stepCaptor.getAllValues().subList(0, 2);
		assertThat(nestedStepsRqs.get(0).getName(), equalTo(stepName));
		assertThat(nestedStepsRqs.get(1).getName(), equalTo(stepName2));

		String nestedStepFinishedUuid = finishStepUuidCaptor.getValue();
		assertThat(nestedStepFinishedUuid, equalTo(nestedSteps.get(0)));

		FinishTestItemRQ finishRq = finishStepCaptor.getValue();
		assertThat(finishRq.getStatus(), equalTo(ItemStatus.PASSED.name()));
		sr.finishPreviousStep();
		launch.finishTestItem(testMethodUuidMaybe, positiveFinishRequest());
		launch.finishTestItem(testClassUuidMaybe, positiveFinishRequest());
		launch.finish(standardLaunchFinishRequest());
	}

	@Test
	public void verify_failed_nested_step_marks_parent_test_as_failed_parent_finish() {
		mockNestedSteps(client, nestedStepPairs.get(0));
		String stepName = UUID.randomUUID().toString();
		sr.sendStep(ItemStatus.FAILED, stepName);
		sr.finishPreviousStep();

		assertThat("StepReporter should save parent failures", sr.isFailed(testClassUuidMaybe), equalTo(Boolean.TRUE));
		assertThat("StepReporter should save parent failures", sr.isFailed(testMethodUuidMaybe), equalTo(Boolean.TRUE));

		launch.finishTestItem(testMethodUuidMaybe, positiveFinishRequest());
		ArgumentCaptor<FinishTestItemRQ> finishStepCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, timeout(1000).times(1)).finishTestItem(eq(testMethodUuid), finishStepCaptor.capture());

		assertThat(
				"Parent test should fail if a nested step failed",
				finishStepCaptor.getValue().getStatus(),
				equalTo(ItemStatus.FAILED.name())
		);
		launch.finishTestItem(testClassUuidMaybe, positiveFinishRequest());
		launch.finish(standardLaunchFinishRequest());
	}

	@Test
	public void verify_failed_nested_step_marks_parent_test_as_failed_nested_finish() {
		mockNestedSteps(client, nestedStepPairs.get(0));
		String stepName = UUID.randomUUID().toString();
		sr.sendStep(ItemStatus.FAILED, stepName);

		verify(client, timeout(1000).times(1)).startTestItem(eq(testMethodUuid), any());
		sr.finishPreviousStep();

		ArgumentCaptor<FinishTestItemRQ> finishStepCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, timeout(1000).times(1)).finishTestItem(eq(nestedSteps.get(0)), finishStepCaptor.capture());

		assertThat(finishStepCaptor.getValue().getStatus(), equalTo(ItemStatus.FAILED.name()));
		assertThat("StepReporter should save parent failures", sr.isFailed(testClassUuidMaybe), equalTo(Boolean.TRUE));
		assertThat("StepReporter should save parent failures", sr.isFailed(testMethodUuidMaybe), equalTo(Boolean.TRUE));
		launch.finishTestItem(testMethodUuidMaybe, positiveFinishRequest());
		launch.finishTestItem(testClassUuidMaybe, positiveFinishRequest());
		launch.finish(standardLaunchFinishRequest());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void verify_nested_step_with_a_batch_of_logs() {
		mockNestedSteps(client, nestedStepPairs.get(0));
		when(client.log(any(List.class))).thenReturn(createConstantMaybe(new BatchSaveOperatingRS()));

		int logNumber = 3;

		String stepName = UUID.randomUUID().toString();
		String[] logs = IntStream.range(0, logNumber).mapToObj(i -> UUID.randomUUID().toString()).toArray(String[]::new);
		sr.sendStep(stepName, logs);

		ArgumentCaptor<StartTestItemRQ> stepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, timeout(1000).times(1)).startTestItem(eq(testMethodUuid), stepCaptor.capture());
		ArgumentCaptor<List<MultipartBody.Part>> logCaptor = ArgumentCaptor.forClass(List.class);
		verify(client, timeout(1000).times(logNumber)).log(logCaptor.capture());

		StartTestItemRQ nestedStep = stepCaptor.getValue();
		assertThat(nestedStep.getName(), equalTo(stepName));

		List<Pair<String, String>> logRequests = logCaptor.getAllValues()
				.stream()
				.flatMap(rq -> TestUtils.extractJsonParts(rq).stream())
				.map(e -> Pair.of(e.getLevel(), e.getMessage()))
				.collect(Collectors.toList());

		IntStream.range(0, logNumber).forEach(i -> {
			assertThat(logRequests.get(i).getKey(), equalTo("INFO"));
			assertThat(logRequests.get(i).getValue(), equalTo(logs[i]));
		});
		sr.finishNestedStep();
		launch.finishTestItem(testMethodUuidMaybe, positiveFinishRequest());
		launch.finishTestItem(testClassUuidMaybe, positiveFinishRequest());
		launch.finish(standardLaunchFinishRequest());
	}
}
