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
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.item.ItemCreatedRS;
import io.reactivex.Maybe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ManualNestedStepTest {

	private final String testLaunchUuid = "launch" + UUID.randomUUID().toString().substring(6);
	private final String testClassUuid = "class" + UUID.randomUUID().toString().substring(5);
	private final String testMethodUuid = "test" + UUID.randomUUID().toString().substring(4);
	private final Maybe<String> launchUuid = TestUtils.getConstantMaybe(testLaunchUuid);

	@Mock
	private ReportPortalClient client;

	private Launch launch;
	private Maybe<String> testMethodUuidMaybe;
	private StepReporter sr;
	private ExecutorService executor;

	private final List<Maybe<ItemCreatedRS>> createdStepsList = new ArrayList<>();
	private final Supplier<Maybe<ItemCreatedRS>> maybeSupplier = () -> {
		String uuid = UUID.randomUUID().toString();
		Maybe<ItemCreatedRS> maybe = TestUtils.getConstantMaybe(new ItemCreatedRS(uuid, uuid));
		createdStepsList.add(maybe);
		return maybe;
	};

	@BeforeEach
	public void initMocks() {
		Maybe<ItemCreatedRS> testMethodCreatedMaybe = TestUtils.getConstantMaybe(new ItemCreatedRS(testMethodUuid, testMethodUuid));
		when(client.startTestItem(eq(testClassUuid), any())).thenReturn(testMethodCreatedMaybe);

		// mock start nested steps
		when(client.startTestItem(eq(testMethodUuid), any())).thenAnswer((Answer<Maybe<ItemCreatedRS>>) invocation -> maybeSupplier.get());
		// mock finish nested steps
		when(client.finishTestItem(any(String.class),
				any(FinishTestItemRQ.class)
		)).thenAnswer((Answer<Maybe<OperationCompletionRS>>) invocation -> TestUtils.getConstantMaybe(new OperationCompletionRS()));

		executor = Executors.newSingleThreadExecutor();
		ReportPortal rp = ReportPortal.create(client, TestUtils.STANDARD_PARAMETERS, executor);
		launch = rp.withLaunch(launchUuid);
		testMethodUuidMaybe = launch.startTestItem(TestUtils.getConstantMaybe(testClassUuid), TestUtils.standardStartStepRequest());
		sr = launch.getStepReporter();
	}

	@AfterEach
	public void cleanup() throws InterruptedException {
		executor.shutdown();
		if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
			executor.shutdownNow();
		}
	}

	@Test
	public void test_sent_step_creates_nested_step() {
		String stepName = UUID.randomUUID().toString();
		sr.sendStep(stepName);

		ArgumentCaptor<StartTestItemRQ> stepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, timeout(1000).times(1)).startTestItem(eq(testMethodUuid), stepCaptor.capture());
		verify(client, after(1000).times(0)).finishTestItem(anyString(), any());

		StartTestItemRQ nestedStep = stepCaptor.getValue();
		assertThat(nestedStep.getName(), equalTo(stepName));
		sr.finishPreviousStep();
	}

	@Test
	public void verify_two_nested_steps_report_on_the_same_level() {
		String stepName = UUID.randomUUID().toString();
		sr.sendStep(stepName);

		String stepName2 = UUID.randomUUID().toString();
		sr.sendStep(stepName2);

		ArgumentCaptor<String> stepParentUuidCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<StartTestItemRQ> stepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, after(1000).times(3)).startTestItem(stepParentUuidCaptor.capture(), stepCaptor.capture());

		ArgumentCaptor<String> finishStepUuidCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<FinishTestItemRQ> finishStepCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, after(1000).times(1)).finishTestItem(finishStepUuidCaptor.capture(), finishStepCaptor.capture());

		List<String> parentUuids = stepParentUuidCaptor.getAllValues()
				.subList(0, 3); // I believe due to mockito bug there are over 9000 items in this list
		assertThat(parentUuids, contains(equalTo(testClassUuid), equalTo(testMethodUuid), equalTo(testMethodUuid)));

		List<StartTestItemRQ> nestedSteps = stepCaptor.getAllValues()
				.subList(0, 3); // I believe due to mockito bug there are over 9000 items in this list
		assertThat(nestedSteps.get(1).getName(), equalTo(stepName));
		assertThat(nestedSteps.get(2).getName(), equalTo(stepName2));

		String nestedStepFinishedUuid = finishStepUuidCaptor.getValue();
		assertThat(nestedStepFinishedUuid, equalTo(createdStepsList.get(0).blockingGet().getUniqueId()));

		FinishTestItemRQ finishRq = finishStepCaptor.getValue();
		assertThat(finishRq.getStatus(), equalTo(ItemStatus.PASSED.name()));
	}

	@Test
	public void verify_failed_nested_step_marks_parent_test_as_failed_parent_finish() {
		String stepName = UUID.randomUUID().toString();
		sr.sendStep(ItemStatus.FAILED, stepName);
		sr.finishPreviousStep();

		assertThat("StepReporter should save parent failures", sr.isFailed(testMethodUuidMaybe), equalTo(Boolean.TRUE));

		launch.finishTestItem(testMethodUuidMaybe, TestUtils.positiveFinishRequest());
		ArgumentCaptor<FinishTestItemRQ> finishStepCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, timeout(1000).times(1)).finishTestItem(eq(testMethodUuid), finishStepCaptor.capture());

		assertThat(
				"Parent test should fail if a nested step failed",
				finishStepCaptor.getValue().getStatus(),
				equalTo(ItemStatus.FAILED.name())
		);
	}

	@Test
	public void verify_failed_nested_step_marks_parent_test_as_failed_nested_finish() {
		String stepName = UUID.randomUUID().toString();
		sr.sendStep(ItemStatus.FAILED, stepName);
		sr.finishPreviousStep();

		assertThat("StepReporter should save parent failures", sr.isFailed(testMethodUuidMaybe), equalTo(Boolean.TRUE));
	}
}
