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
import com.epam.reportportal.listeners.LogLevel;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.test.TestUtils;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import okhttp3.MultipartBody;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.opentest4j.AssertionFailedError;

import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.epam.reportportal.test.TestUtils.*;
import static com.epam.reportportal.util.test.CommonUtils.shutdownExecutorService;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
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
		String stepName = "test_sent_step_creates_nested_step";
		sr.sendStep(stepName);

		ArgumentCaptor<StartTestItemRQ> stepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, timeout(1000)).startTestItem(eq(testMethodUuid), stepCaptor.capture());
		verify(client, after(1000).times(0)).finishTestItem(eq(nestedSteps.get(0)), any());

		StartTestItemRQ nestedStep = stepCaptor.getValue();
		assertThat(nestedStep.getName(), equalTo(stepName));
		assertThat(nestedStep.isHasStats(), equalTo(Boolean.FALSE));
		launch.finishTestItem(testMethodUuidMaybe, positiveFinishRequest());
		launch.finishTestItem(testClassUuidMaybe, positiveFinishRequest());
		launch.finish(standardLaunchFinishRequest());
	}

	@Test
	public void verify_two_nested_steps_report_on_the_same_level() {
		mockNestedSteps(client, nestedStepPairs);
		String stepName = "verify_two_nested_steps_report_on_the_same_level1";
		sr.sendStep(stepName);

		String stepName2 = "verify_two_nested_steps_report_on_the_same_level2";
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
		launch.finishTestItem(testMethodUuidMaybe, positiveFinishRequest());
		launch.finishTestItem(testClassUuidMaybe, positiveFinishRequest());
		launch.finish(standardLaunchFinishRequest());
	}

	@Test
	public void verify_failed_nested_step_marks_parent_test_as_failed_parent_finish() {
		mockNestedSteps(client, nestedStepPairs.get(0));
		String stepName = "verify_failed_nested_step_marks_parent_test_as_failed_parent_finish";
		sr.sendStep(ItemStatus.FAILED, stepName);

		launch.finishTestItem(testMethodUuidMaybe, positiveFinishRequest());
		assertThat("StepReporter should save parent failures", sr.isFailed(testClassUuidMaybe), equalTo(Boolean.TRUE));

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
		String stepName = "verify_failed_nested_step_marks_parent_test_as_failed_nested_finish";
		sr.sendStep(ItemStatus.FAILED, stepName);

		verify(client, timeout(1000)).startTestItem(eq(testMethodUuid), any());
		sr.finishPreviousStep();

		ArgumentCaptor<FinishTestItemRQ> finishStepCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, timeout(1000)).finishTestItem(eq(nestedSteps.get(0)), finishStepCaptor.capture());

		assertThat(finishStepCaptor.getValue().getStatus(), equalTo(ItemStatus.FAILED.name()));
		assertThat("StepReporter should save parent failures", sr.isFailed(testClassUuidMaybe), equalTo(Boolean.TRUE));
		assertThat("StepReporter should save parent failures", sr.isFailed(testMethodUuidMaybe), equalTo(Boolean.TRUE));
		launch.finishTestItem(testMethodUuidMaybe, positiveFinishRequest());
		launch.finishTestItem(testClassUuidMaybe, positiveFinishRequest());
		launch.finish(standardLaunchFinishRequest());
	}

	@Test
	public void verify_failed_nested_finish_step_marks_parent_test_as_failed_nested_finish() {
		mockNestedSteps(client, nestedStepPairs.get(0));
		String stepName = "verify_failed_nested_finish_step_marks_parent_test_as_failed_nested_finish";
		sr.sendStep(stepName);

		verify(client, timeout(1000)).startTestItem(eq(testMethodUuid), any());
		sr.finishPreviousStep(ItemStatus.FAILED);

		ArgumentCaptor<FinishTestItemRQ> finishStepCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, timeout(1000)).finishTestItem(eq(nestedSteps.get(0)), finishStepCaptor.capture());

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
		mockBatchLogging(client);

		int logNumber = 3;

		String stepName = "verify_nested_step_with_a_batch_of_logs";
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

		Collection<Matcher<? super Pair<String, String>>> expectedItems =
				IntStream.range(0, logNumber).mapToObj(i -> equalTo(Pair.of("INFO", logs[i]))).collect(Collectors.toList());

		assertThat(logRequests, containsInAnyOrder(expectedItems));

		launch.finishTestItem(testMethodUuidMaybe, positiveFinishRequest());
		launch.finishTestItem(testClassUuidMaybe, positiveFinishRequest());
		launch.finish(standardLaunchFinishRequest());
	}

	@Test
	public void verify_single_name_nested_step() {
		mockNestedSteps(client, nestedStepPairs.get(0));
		String stepName = "verify_single_name_nested_step";
		sr.step(stepName);

		ArgumentCaptor<StartTestItemRQ> startStepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, timeout(1000)).startTestItem(eq(testMethodUuid), startStepCaptor.capture());
		ArgumentCaptor<FinishTestItemRQ> finishStepCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, timeout(1000)).finishTestItem(eq(nestedSteps.get(0)), finishStepCaptor.capture());

		StartTestItemRQ nestedStepStart = startStepCaptor.getValue();
		assertThat(nestedStepStart.getName(), equalTo(stepName));
		assertThat(nestedStepStart.isHasStats(), equalTo(Boolean.FALSE));

		FinishTestItemRQ nestedStepFinish = finishStepCaptor.getValue();
		assertThat(nestedStepFinish.getStatus(), equalTo(ItemStatus.PASSED.name()));
		assertThat(nestedStepFinish.getEndTime(), notNullValue());

		launch.finishTestItem(testMethodUuidMaybe, positiveFinishRequest());
		launch.finishTestItem(testClassUuidMaybe, positiveFinishRequest());
		launch.finish(standardLaunchFinishRequest());
	}

	@Test
	public void verify_name_and_status_nested_step() {
		mockNestedSteps(client, nestedStepPairs.get(0));
		String stepName = "verify_name_and_status_nested_step";
		sr.step(ItemStatus.SKIPPED, stepName);

		ArgumentCaptor<StartTestItemRQ> startStepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, timeout(1000)).startTestItem(eq(testMethodUuid), startStepCaptor.capture());
		ArgumentCaptor<FinishTestItemRQ> finishStepCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, timeout(1000)).finishTestItem(eq(nestedSteps.get(0)), finishStepCaptor.capture());

		StartTestItemRQ nestedStepStart = startStepCaptor.getValue();
		assertThat(nestedStepStart.getName(), equalTo(stepName));
		assertThat(nestedStepStart.isHasStats(), equalTo(Boolean.FALSE));

		FinishTestItemRQ nestedStepFinish = finishStepCaptor.getValue();
		assertThat(nestedStepFinish.getStatus(), equalTo(ItemStatus.SKIPPED.name()));
		assertThat(nestedStepFinish.getEndTime(), notNullValue());

		launch.finishTestItem(testMethodUuidMaybe, positiveFinishRequest());
		launch.finishTestItem(testClassUuidMaybe, positiveFinishRequest());
		launch.finish(standardLaunchFinishRequest());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void verify_passed_actions_nested_step() {
		mockNestedSteps(client, nestedStepPairs.get(0));
		mockBatchLogging(client);
		String stepName = "verify_passed_actions_nested_step";
		String returnValue = "return value";
		String logMessage = "Test message";
		String result = sr.step(stepName, () -> {
			ReportPortal.emitLog(logMessage, LogLevel.DEBUG.name(), Calendar.getInstance().getTime());
			return returnValue;
		});
		assertThat(result, equalTo(returnValue));

		ArgumentCaptor<StartTestItemRQ> startStepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, timeout(1000)).startTestItem(eq(testMethodUuid), startStepCaptor.capture());
		ArgumentCaptor<FinishTestItemRQ> finishStepCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, timeout(1000)).finishTestItem(eq(nestedSteps.get(0)), finishStepCaptor.capture());

		StartTestItemRQ nestedStepStart = startStepCaptor.getValue();
		assertThat(nestedStepStart.getName(), equalTo(stepName));
		assertThat(nestedStepStart.isHasStats(), equalTo(Boolean.FALSE));

		FinishTestItemRQ nestedStepFinish = finishStepCaptor.getValue();
		assertThat(nestedStepFinish.getStatus(), equalTo(ItemStatus.PASSED.name()));
		assertThat(nestedStepFinish.getEndTime(), notNullValue());

		ArgumentCaptor<List<MultipartBody.Part>> logCaptor = ArgumentCaptor.forClass(List.class);
		verify(client, timeout(1000).atLeastOnce()).log(logCaptor.capture());
		List<Pair<String, String>> logRequests = logCaptor.getAllValues()
				.stream()
				.flatMap(rq -> TestUtils.extractJsonParts(rq).stream())
				.map(e -> Pair.of(e.getLevel(), e.getMessage()))
				.collect(Collectors.toList());
		assertThat(logRequests, hasSize(1));
		Pair<String, String> log = logRequests.get(0);
		assertThat(log.getKey(), equalTo(LogLevel.DEBUG.name()));
		assertThat(log.getValue(), equalTo(logMessage));

		launch.finishTestItem(testMethodUuidMaybe, positiveFinishRequest());
		launch.finishTestItem(testClassUuidMaybe, positiveFinishRequest());
		launch.finish(standardLaunchFinishRequest());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void verify_passed_actions_nested_step_failure() {
		mockNestedSteps(client, nestedStepPairs.get(0));
		String stepName = "verify_passed_actions_nested_step_failure";
		String returnValue = "return value";
		String logMessage = "Test message";
		AssertionFailedError result = Assertions.assertThrows(AssertionFailedError.class, () -> sr.step(stepName, () -> {
			Assertions.fail(logMessage);
			return returnValue;
		}));
		assertThat(result.getMessage(), equalTo(logMessage));

		ArgumentCaptor<StartTestItemRQ> startStepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, timeout(1000)).startTestItem(eq(testMethodUuid), startStepCaptor.capture());
		ArgumentCaptor<FinishTestItemRQ> finishStepCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, timeout(1000)).finishTestItem(eq(nestedSteps.get(0)), finishStepCaptor.capture());

		StartTestItemRQ nestedStepStart = startStepCaptor.getValue();
		assertThat(nestedStepStart.getName(), equalTo(stepName));
		assertThat(nestedStepStart.isHasStats(), equalTo(Boolean.FALSE));

		FinishTestItemRQ nestedStepFinish = finishStepCaptor.getValue();
		assertThat(nestedStepFinish.getStatus(), equalTo(ItemStatus.FAILED.name()));
		assertThat(nestedStepFinish.getEndTime(), notNullValue());

		verify(client, times(0)).log(any(List.class)); // All exceptions should be logged on step level

		launch.finishTestItem(testMethodUuidMaybe, positiveFinishRequest());
		launch.finishTestItem(testClassUuidMaybe, positiveFinishRequest());
		launch.finish(standardLaunchFinishRequest());
	}

	@Test
	public void verify_nested_step_manual_failure_set() {
		mockNestedSteps(client, nestedStepPairs.get(0));
		mockBatchLogging(client);
		String stepName = "verify_passed_actions_nested_step_failure";
		String logMessage = "Test message";
		sr.sendStep(stepName, logMessage);
		sr.setStepStatus(ItemStatus.FAILED);
		sr.finishPreviousStep();

		verify(client, timeout(1000)).startTestItem(eq(testMethodUuid), any(StartTestItemRQ.class));
		ArgumentCaptor<FinishTestItemRQ> finishStepCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, timeout(1000)).finishTestItem(eq(nestedSteps.get(0)), finishStepCaptor.capture());

		FinishTestItemRQ nestedStepFinish = finishStepCaptor.getValue();
		assertThat(nestedStepFinish.getStatus(), equalTo(ItemStatus.FAILED.name()));

		launch.finishTestItem(testMethodUuidMaybe, positiveFinishRequest());
		launch.finishTestItem(testClassUuidMaybe, positiveFinishRequest());
		launch.finish(standardLaunchFinishRequest());
	}

	@Test
	public void verify_nested_step_manual_failure_set_overrides_any_other_status() {
		mockNestedSteps(client, nestedStepPairs.get(0));
		mockBatchLogging(client);
		String stepName = "verify_passed_actions_nested_step_failure";
		String logMessage = "Test message";
		sr.sendStep(ItemStatus.PASSED, stepName, logMessage);
		sr.setStepStatus(ItemStatus.PASSED);
		sr.finishPreviousStep(ItemStatus.FAILED);

		verify(client, timeout(1000)).startTestItem(eq(testMethodUuid), any(StartTestItemRQ.class));
		ArgumentCaptor<FinishTestItemRQ> finishStepCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, timeout(1000)).finishTestItem(eq(nestedSteps.get(0)), finishStepCaptor.capture());

		FinishTestItemRQ nestedStepFinish = finishStepCaptor.getValue();
		assertThat(nestedStepFinish.getStatus(), equalTo(ItemStatus.FAILED.name()));

		launch.finishTestItem(testMethodUuidMaybe, positiveFinishRequest());
		launch.finishTestItem(testClassUuidMaybe, positiveFinishRequest());
		launch.finish(standardLaunchFinishRequest());
	}

	@Test
	public void verify_nested_step_manual_failure_set_overrides_any_other_status_for_passed_actions() {
		mockNestedSteps(client, nestedStepPairs.get(0));
		String stepName = "verify_nested_step_manual_failure_set_overrides_any_other_status_for_passed_actions";
		String returnValue = "return value";
		String logMessage = "Test message";
		AssertionFailedError result = Assertions.assertThrows(AssertionFailedError.class, () -> sr.step(stepName, () -> {
			sr.setStepStatus(ItemStatus.PASSED);
			Assertions.fail(logMessage);
			return returnValue;
		}));
		assertThat(result.getMessage(), equalTo(logMessage));

		verify(client, timeout(1000)).startTestItem(eq(testMethodUuid), any(StartTestItemRQ.class));
		ArgumentCaptor<FinishTestItemRQ> finishStepCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, timeout(1000)).finishTestItem(eq(nestedSteps.get(0)), finishStepCaptor.capture());

		FinishTestItemRQ nestedStepFinish = finishStepCaptor.getValue();
		assertThat(nestedStepFinish.getStatus(), equalTo(ItemStatus.PASSED.name()));

		launch.finishTestItem(testMethodUuidMaybe, positiveFinishRequest());
		launch.finishTestItem(testClassUuidMaybe, positiveFinishRequest());
		launch.finish(standardLaunchFinishRequest());
	}

	@Test
	public void verify_manually_set_nested_step_status_marks_parent_test_as_failed() {
		mockNestedSteps(client, nestedStepPairs.get(0));
		String stepName = "verify_manually_set_nested_step_status_marks_parent_test_as_failed";
		String returnValue = "return value";
		String result = sr.step(stepName, () -> {
			sr.setStepStatus(ItemStatus.FAILED);
			return returnValue;
		});
		assertThat(result, equalTo(returnValue));

		verify(client, timeout(1000)).startTestItem(eq(testMethodUuid), any());
		ArgumentCaptor<FinishTestItemRQ> finishStepCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, timeout(1000)).finishTestItem(eq(nestedSteps.get(0)), finishStepCaptor.capture());

		assertThat(finishStepCaptor.getValue().getStatus(), equalTo(ItemStatus.FAILED.name()));
		assertThat("StepReporter should save parent failures", sr.isFailed(testClassUuidMaybe), equalTo(Boolean.TRUE));
		assertThat("StepReporter should save parent failures", sr.isFailed(testMethodUuidMaybe), equalTo(Boolean.TRUE));

		launch.finishTestItem(testMethodUuidMaybe, positiveFinishRequest());
		launch.finishTestItem(testClassUuidMaybe, positiveFinishRequest());
		launch.finish(standardLaunchFinishRequest());
		ArgumentCaptor<FinishTestItemRQ> finishTestCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, timeout(1000)).finishTestItem(eq(testClassUuid), finishTestCaptor.capture());

		assertThat(finishTestCaptor.getValue().getStatus(), equalTo(ItemStatus.FAILED.name()));
	}
}
