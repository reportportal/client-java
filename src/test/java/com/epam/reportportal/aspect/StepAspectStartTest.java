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
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.test.TestUtils;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="mailto:vadzim_hushchanskou@epam.com">Vadzim Hushchanskou</a>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class StepAspectStartTest {
	private static final ReportPortalClient CLIENT = mock(ReportPortalClient.class);

	static {
		StepAspectCommon.simulateStartLaunch(CLIENT, "launch2");
	}

	private final StepAspect aspect = new StepAspect();
	private final ListenerParameters params = TestUtils.standardParameters();

	@Mock
	public MethodSignature methodSignature;

	@Test
	public void test_simple_nested_step_item_rq() throws NoSuchMethodException {
		// Avoid thread-local collision
		String parentId = UUID.randomUUID().toString();
		String itemUuid = UUID.randomUUID().toString();
		StepAspectCommon.simulateStartItemResponse(CLIENT, parentId, itemUuid);
		StepAspect.setParentId(CommonUtils.createMaybe(parentId));
		ReportPortal.create(CLIENT, params).newLaunch(TestUtils.standardLaunchRequest(params)).start();

		Method method = StepAspectCommon.getMethod("testNestedStepSimple");
		aspect.startNestedStep(StepAspectCommon.getJoinPoint(methodSignature, method), method.getAnnotation(Step.class));

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(CLIENT).startTestItem(same(parentId), captor.capture());
		StartTestItemRQ result = captor.getValue();

		assertThat(result.getName(), equalTo(StepAspectCommon.TEST_STEP_NAME));
		assertThat(result.getDescription(), equalTo(StepAspectCommon.TEST_STEP_DESCRIPTION));
		assertThat(result.getAttributes(), nullValue());
		StepAspectCommon.simulateFinishItemResponse(CLIENT, itemUuid);
		aspect.finishNestedStep(method.getAnnotation(Step.class));

		StepAspectCommon.simulateFinishItemResponse(CLIENT, itemUuid);
		aspect.finishNestedStep(method.getAnnotation(Step.class));
	}

	@Test
	public void test_nested_step_attribute_processing() throws NoSuchMethodException {
		// Avoid thread-local collision
		String parentId = UUID.randomUUID().toString();
		String itemUuid = UUID.randomUUID().toString();
		StepAspectCommon.simulateStartItemResponse(CLIENT, parentId, itemUuid);
		StepAspect.setParentId(CommonUtils.createMaybe(parentId));
		ReportPortal.create(CLIENT, params).newLaunch(TestUtils.standardLaunchRequest(params)).start();

		Method method = StepAspectCommon.getMethod("testNestedStepAttributeAnnotation");
		aspect.startNestedStep(StepAspectCommon.getJoinPoint(methodSignature, method), method.getAnnotation(Step.class));

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(CLIENT).startTestItem(same(parentId), captor.capture());
		StartTestItemRQ result = captor.getValue();

		assertThat(result.getAttributes(), hasSize(1));
		assertThat(result.getAttributes(), contains(notNullValue()));

		StepAspectCommon.simulateFinishItemResponse(CLIENT, itemUuid);
		aspect.finishNestedStep(method.getAnnotation(Step.class));
	}
}
