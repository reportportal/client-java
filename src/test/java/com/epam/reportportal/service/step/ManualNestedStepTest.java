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
import com.epam.ta.reportportal.ws.model.BatchSaveOperatingRS;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.item.ItemCreatedRS;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Maybe;
import org.apache.commons.lang3.tuple.Pair;
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
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.epam.reportportal.test.TestUtils.shutdownExecutorService;
import static com.epam.reportportal.utils.SubscriptionUtils.createConstantMaybe;
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
	private final Maybe<String> launchUuid = createConstantMaybe(testLaunchUuid);
	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	@Mock
	private ReportPortalClient client;

	private Launch launch;
	private Maybe<String> testClassUuidMaybe;
	private Maybe<String> testMethodUuidMaybe;
	private StepReporter sr;

	private final List<Maybe<ItemCreatedRS>> createdStepsList = new ArrayList<>();
	private final Supplier<Maybe<ItemCreatedRS>> maybeSupplier = () -> {
		String uuid = UUID.randomUUID().toString();
		Maybe<ItemCreatedRS> maybe = createConstantMaybe(new ItemCreatedRS(uuid, uuid));
		createdStepsList.add(maybe);
		return maybe;
	};

	@BeforeEach
	public void initMocks() {
		Maybe<ItemCreatedRS> classCreatedMaybe = createConstantMaybe(new ItemCreatedRS(testClassUuid, testClassUuid));
		when(client.startTestItem(same(testLaunchUuid), any())).thenReturn(classCreatedMaybe);
		Maybe<ItemCreatedRS> testMethodCreatedMaybe = createConstantMaybe(new ItemCreatedRS(testMethodUuid, testMethodUuid));
		when(client.startTestItem(same(testClassUuid), any())).thenReturn(testMethodCreatedMaybe);

		// mock start nested steps
		when(client.startTestItem(same(testMethodUuid),
				any()
		)).thenAnswer((Answer<Maybe<ItemCreatedRS>>) invocation -> maybeSupplier.get());

		ReportPortal rp = ReportPortal.create(client, TestUtils.STANDARD_PARAMETERS, executor);
		launch = rp.withLaunch(launchUuid);
		testClassUuidMaybe = launch.startTestItem(createConstantMaybe(testLaunchUuid), TestUtils.standardStartTestRequest());
		testMethodUuidMaybe = launch.startTestItem(testClassUuidMaybe, TestUtils.standardStartStepRequest());
		testMethodUuidMaybe.blockingGet();
		sr = launch.getStepReporter();
	}

	public void mockFinishNestedStep() {
		when(client.finishTestItem(any(String.class),
				any(FinishTestItemRQ.class)
		)).thenAnswer((Answer<Maybe<OperationCompletionRS>>) invocation -> createConstantMaybe(new OperationCompletionRS()));
	}

	@AfterEach
	public void cleanup() {
		shutdownExecutorService(executor);
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
	}

	@Test
	public void verify_two_nested_steps_report_on_the_same_level() {
		mockFinishNestedStep();

		String stepName = UUID.randomUUID().toString();
		sr.sendStep(stepName);

		String stepName2 = UUID.randomUUID().toString();
		sr.sendStep(stepName2);

		ArgumentCaptor<String> stepParentUuidCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<StartTestItemRQ> stepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, after(1000).times(4)).startTestItem(stepParentUuidCaptor.capture(), stepCaptor.capture());

		ArgumentCaptor<String> finishStepUuidCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<FinishTestItemRQ> finishStepCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, after(1000).times(1)).finishTestItem(finishStepUuidCaptor.capture(), finishStepCaptor.capture());

		List<String> parentUuids = stepParentUuidCaptor.getAllValues()
				.subList(0, 4); // I believe due to mockito bug there are over 9000 items in this list
		assertThat(
				parentUuids,
				contains(equalTo(testLaunchUuid), equalTo(testClassUuid), equalTo(testMethodUuid), equalTo(testMethodUuid))
		);

		List<StartTestItemRQ> nestedSteps = stepCaptor.getAllValues()
				.subList(0, 4); // I believe due to mockito bug there are over 9000 items in this list
		assertThat(nestedSteps.get(2).getName(), equalTo(stepName));
		assertThat(nestedSteps.get(3).getName(), equalTo(stepName2));

		String nestedStepFinishedUuid = finishStepUuidCaptor.getValue();
		assertThat(nestedStepFinishedUuid, equalTo(createdStepsList.get(0).blockingGet().getUniqueId()));

		FinishTestItemRQ finishRq = finishStepCaptor.getValue();
		assertThat(finishRq.getStatus(), equalTo(ItemStatus.PASSED.name()));
	}

	@Test
	public void verify_failed_nested_step_marks_parent_test_as_failed_parent_finish() {
		mockFinishNestedStep();

		String stepName = UUID.randomUUID().toString();
		sr.sendStep(ItemStatus.FAILED, stepName);
		sr.finishPreviousStep();

		assertThat("StepReporter should save parent failures", sr.isFailed(testClassUuidMaybe), equalTo(Boolean.TRUE));
		assertThat("StepReporter should save parent failures", sr.isFailed(testMethodUuidMaybe), equalTo(Boolean.TRUE));

		launch.finishTestItem(testMethodUuidMaybe, TestUtils.positiveFinishRequest());
		ArgumentCaptor<FinishTestItemRQ> finishStepCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, timeout(1000).times(1)).finishTestItem(eq(testMethodUuid), finishStepCaptor.capture());

		assertThat("Parent test should fail if a nested step failed",
				finishStepCaptor.getValue().getStatus(),
				equalTo(ItemStatus.FAILED.name())
		);
	}

	@Test
	public void verify_failed_nested_step_marks_parent_test_as_failed_nested_finish() {
		mockFinishNestedStep();

		String stepName = UUID.randomUUID().toString();
		sr.sendStep(ItemStatus.FAILED, stepName);

		verify(client, timeout(1000).times(1)).startTestItem(eq(testMethodUuid), any());
		sr.finishPreviousStep();

		ArgumentCaptor<FinishTestItemRQ> finishStepCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, timeout(1000).times(1)).finishTestItem(eq(createdStepsList.get(0).blockingGet().getUniqueId()),
				finishStepCaptor.capture()
		);

		assertThat(finishStepCaptor.getValue().getStatus(), equalTo(ItemStatus.FAILED.name()));
		assertThat("StepReporter should save parent failures", sr.isFailed(testClassUuidMaybe), equalTo(Boolean.TRUE));
		assertThat("StepReporter should save parent failures", sr.isFailed(testMethodUuidMaybe), equalTo(Boolean.TRUE));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void verify_nested_step_with_a_batch_of_logs() {
		// TODO: fix
//		when(client.log(any(MultiPartRequest.class))).thenReturn(createConstantMaybe(new BatchSaveOperatingRS()));

		int logNumber = 3;

		String stepName = UUID.randomUUID().toString();
		String[] logs = IntStream.range(0, logNumber).mapToObj(i -> UUID.randomUUID().toString()).toArray(String[]::new);
		sr.sendStep(stepName, logs);

		ArgumentCaptor<StartTestItemRQ> stepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, timeout(1000).times(1)).startTestItem(eq(testMethodUuid), stepCaptor.capture());
		// TODO: fix
//		ArgumentCaptor<MultiPartRequest> logCaptor = ArgumentCaptor.forClass(MultiPartRequest.class);
//		verify(client, timeout(1000).times(logNumber)).log(logCaptor.capture());

		StartTestItemRQ nestedStep = stepCaptor.getValue();
		assertThat(nestedStep.getName(), equalTo(stepName));

//		List<Pair<String, String>> logRequests = logCaptor.getAllValues()
//				.stream()
//				.flatMap(rq -> rq.getSerializedRQs().stream())
//				.flatMap(e -> ((List<SaveLogRQ>) e.getRequest()).stream())
//				.map(e -> Pair.of(e.getLevel(), e.getMessage()))
//				.collect(Collectors.toList());

//		IntStream.range(0, logNumber).forEach(i -> {
//			assertThat(logRequests.get(i).getKey(), equalTo("INFO"));
//			assertThat(logRequests.get(i).getValue(), equalTo(logs[i]));
//		});
	}
}
