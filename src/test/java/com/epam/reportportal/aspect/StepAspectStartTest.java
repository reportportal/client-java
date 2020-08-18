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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="mailto:vadzim_hushchanskou@epam.com">Vadzim Hushchanskou</a>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class StepAspectStartTest {
	private final StepAspect aspect = new StepAspect();

	private ReportPortalClient client;
	@Mock
	public MethodSignature methodSignature;
	private Method method;

	private String parentId;
	private String itemUuid;

	@BeforeAll
	public void launchSetup() {
		client = mock(ReportPortalClient.class);
		StepAspectCommon.simulateStartLaunch(client, "launch2");
	}

	@BeforeEach
	public void stepSetup() {
		parentId = UUID.randomUUID().toString();
		itemUuid = UUID.randomUUID().toString();
		StepAspectCommon.simulateStartItemResponse(client, parentId, itemUuid);
		StepAspect.setParentId(CommonUtils.createMaybe(parentId));
		ListenerParameters params = TestUtils.standardParameters();
		ReportPortal.create(client, params).newLaunch(TestUtils.standardLaunchRequest(params)).start();
	}

	@AfterEach
	public void cleanup() {
		StepAspectCommon.simulateFinishItemResponse(client, itemUuid);
		aspect.finishNestedStep(method.getAnnotation(Step.class));
	}

	@Test
	public void test_simple_nested_step_item_rq() throws NoSuchMethodException {
		method = StepAspectCommon.getMethod("testNestedStepSimple");
		aspect.startNestedStep(StepAspectCommon.getJoinPoint(methodSignature, method), method.getAnnotation(Step.class));

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client).startTestItem(eq(parentId), captor.capture());
		StartTestItemRQ result = captor.getValue();

		assertThat(result.getName(), equalTo(StepAspectCommon.TEST_STEP_NAME));
		assertThat(result.getDescription(), equalTo(StepAspectCommon.TEST_STEP_DESCRIPTION));
		assertThat(result.getAttributes(), nullValue());
		StepAspectCommon.simulateFinishItemResponse(client, itemUuid);
		aspect.finishNestedStep(method.getAnnotation(Step.class));
	}

	@Test
	public void test_nested_step_attribute_processing() throws NoSuchMethodException {
		method = StepAspectCommon.getMethod("testNestedStepAttributeAnnotation");
		aspect.startNestedStep(StepAspectCommon.getJoinPoint(methodSignature, method), method.getAnnotation(Step.class));

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client).startTestItem(same(parentId), captor.capture());
		StartTestItemRQ result = captor.getValue();

		assertThat(result.getAttributes(), hasSize(1));
		assertThat(result.getAttributes(), contains(notNullValue()));
	}
}
