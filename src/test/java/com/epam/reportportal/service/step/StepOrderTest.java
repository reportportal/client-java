/*
 * Copyright 2020 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.reportportal.service.step;

import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.test.TestUtils;
import com.epam.ta.reportportal.ws.reporting.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.reporting.OperationCompletionRS;
import com.epam.ta.reportportal.ws.reporting.StartTestItemRQ;
import com.epam.ta.reportportal.ws.reporting.ItemCreatedRS;
import io.reactivex.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

public class StepOrderTest {

	private final String testLaunchUuid = "launch" + UUID.randomUUID().toString().substring(6);
	private final String testClassUuid = "class" + UUID.randomUUID().toString().substring(5);
	private final String testMethodUuid = "test" + UUID.randomUUID().toString().substring(4);
	private final Maybe<String> launchUuid = Maybe.just(testLaunchUuid);
	private final AtomicInteger counter = new AtomicInteger();

	@Mock
	private ReportPortalClient client;

	private StepReporter sr;

	private final List<Maybe<ItemCreatedRS>> createdStepsList = new ArrayList<>();
	private final Supplier<Maybe<ItemCreatedRS>> maybeSupplier = () -> {
		String uuid = UUID.randomUUID().toString();
		Maybe<ItemCreatedRS> maybe = Maybe.just(new ItemCreatedRS(uuid, uuid));
		createdStepsList.add(maybe);
		return maybe;
	};

	@BeforeEach
	public void initMocks() {
		Maybe<ItemCreatedRS> testMethodCreatedMaybe = Maybe.just(new ItemCreatedRS(testMethodUuid, testMethodUuid));
		when(client.startTestItem(eq(testClassUuid), any())).thenReturn(testMethodCreatedMaybe);

		// mock start nested steps
		when(client.startTestItem(
				eq(testMethodUuid),
				any()
		)).thenAnswer((Answer<Maybe<ItemCreatedRS>>) invocation -> createdStepsList.get(counter.getAndIncrement()));
		// mock finish nested steps
		when(client.finishTestItem(
				any(String.class),
				any(FinishTestItemRQ.class)
		)).thenAnswer((Answer<Maybe<OperationCompletionRS>>) invocation -> Maybe.just(new OperationCompletionRS()));

		ReportPortal rp = ReportPortal.create(client, TestUtils.STANDARD_PARAMETERS);
		Launch launch = rp.withLaunch(launchUuid);
		launch.startTestItem(Maybe.just(testClassUuid), TestUtils.standardStartStepRequest());
		sr = launch.getStepReporter();
	}

	@Test
	public void test_steps_have_different_start_time() {
		int stepNum = 30;

		// create nested steps
		for (int i = 0; i < stepNum; i++) {
			maybeSupplier.get();
		}

		for (int i = 0; i < stepNum; i++) {
			sr.sendStep(i + " step");
		}

		ArgumentCaptor<StartTestItemRQ> stepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, timeout(1000).times(stepNum)).startTestItem(eq(testMethodUuid), stepCaptor.capture());

		List<StartTestItemRQ> rqs = stepCaptor.getAllValues();
		assertThat(rqs, hasSize(stepNum));
		for (int i = 1; i < stepNum; i++) {
			assertThat(
					"Each nested step should not complete in the same millisecond, iteration: " + i,
					rqs.get(i - 1).getStartTime(),
					not(equalTo(rqs.get(i).getStartTime()))
			);
		}
	}
}
