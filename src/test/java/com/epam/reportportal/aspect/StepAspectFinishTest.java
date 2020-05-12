/*
 *  Copyright 2019 EPAM Systems
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

package com.epam.reportportal.aspect;

import com.epam.reportportal.annotations.Step;
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.service.Launch;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import io.reactivex.Maybe;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="mailto:vadzim_hushchanskou@epam.com">Vadzim Hushchanskou</a>
 */
public class StepAspectFinishTest {
	private final String parentId = UUID.randomUUID().toString();
	private final String itemUuid = UUID.randomUUID().toString();
	private final Maybe<String> parentIdMaybe = StepAspectCommon.getMaybe(parentId);
	private final StepAspect aspect = new StepAspect();

	@Mock
	private Launch launch;
	@Mock
	public MethodSignature methodSignature;

	private Maybe<String> startStep;

	@BeforeEach
	public void setup() {
		StepAspect.setParentId(parentIdMaybe);
		StepAspect.addLaunch(UUID.randomUUID().toString(), launch);
		startStep = StepAspectCommon.simulateStartItemResponse(launch, parentIdMaybe, itemUuid);
		StepAspectCommon.simulateFinishItemResponse(launch, startStep);
	}

	/*
	 * Do not finish parent step inside nested step, leads to issue: https://github.com/reportportal/agent-java-testNG/issues/97
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void verify_only_nested_step_finished_and_no_parent_steps() throws NoSuchMethodException {
		Method method = StepAspectCommon.getMethod("testNestedStepSimple");
		aspect.startNestedStep(StepAspectCommon.getJoinPoint(methodSignature, method), method.getAnnotation(Step.class));
		aspect.finishNestedStep(method.getAnnotation(Step.class));

		ArgumentCaptor<Maybe<String>> finishUuids = ArgumentCaptor.forClass(Maybe.class);
		ArgumentCaptor<FinishTestItemRQ> finishRQs = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(launch, times(1)).finishTestItem(finishUuids.capture(), finishRQs.capture());

		Maybe<String> finishUuid = finishUuids.getValue();
		assertThat(finishUuid, sameInstance(startStep));

		FinishTestItemRQ resultRq = finishRQs.getValue();
		assertThat(resultRq.getStatus(), equalTo(ItemStatus.PASSED.name()));
		assertThat(resultRq.getIssue(), nullValue());
	}

	/*
	 * Do not finish parent step inside nested step, leads to issue: https://github.com/reportportal/agent-java-testNG/issues/97
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void verify_only_nested_step_finished_and_no_parent_steps_on_step_failure() throws NoSuchMethodException {
		Method method = StepAspectCommon.getMethod("testNestedStepSimple");
		aspect.startNestedStep(StepAspectCommon.getJoinPoint(methodSignature, method), method.getAnnotation(Step.class));
		aspect.failedNestedStep(method.getAnnotation(Step.class), new IllegalArgumentException());

		ArgumentCaptor<Maybe<String>> finishUuids = ArgumentCaptor.forClass(Maybe.class);
		ArgumentCaptor<FinishTestItemRQ> finishRQs = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(launch, times(1)).finishTestItem(finishUuids.capture(), finishRQs.capture());

		Maybe<String> finishUuid = finishUuids.getValue();
		assertThat(finishUuid, sameInstance(startStep));

		FinishTestItemRQ resultRq = finishRQs.getValue();
		assertThat(resultRq.getStatus(), equalTo(ItemStatus.FAILED.name()));
		assertThat(resultRq.getIssue(), nullValue());
	}
}
