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
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.test.TestUtils;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="mailto:vadzim_hushchanskou@epam.com">Vadzim Hushchanskou</a>
 */
public class StepAspectStartTestAttribute {
	private final StepAspect aspect = new StepAspect();
	private final ListenerParameters params = TestUtils.standardParameters();

	private final String parentId = UUID.randomUUID().toString();
	private final String itemUuid = UUID.randomUUID().toString();

	@Mock(name = "StepAspectStartTest.class")
	public ReportPortalClient client;
	@Mock
	public MethodSignature methodSignature;
	private Launch myLaunch;

	@BeforeEach
	public void launchSetup() {
		StepAspectCommon.simulateLaunch(client, "launch4");
		StepAspectCommon.simulateStartItemResponse(client, parentId, itemUuid);
		StepAspectCommon.simulateFinishItemResponse(client, itemUuid);
		StepAspect.setParentId(CommonUtils.createMaybe(parentId));
		myLaunch = ReportPortal.create(client, params).newLaunch(TestUtils.standardLaunchRequest(params));
		myLaunch.start();
	}

	@Test
	public void test_nested_step_attribute_processing() throws NoSuchMethodException {
		Method method = StepAspectCommon.getMethod("testNestedStepAttributeAnnotation");
		aspect.startNestedStep(StepAspectCommon.getJoinPoint(methodSignature, method), method.getAnnotation(Step.class));

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client).startTestItem(same(parentId), captor.capture());
		StartTestItemRQ result = captor.getValue();

		assertThat(result.getAttributes(), hasSize(1));
		assertThat(result.getAttributes(), contains(notNullValue()));

		aspect.finishNestedStep(method.getAnnotation(Step.class));
		myLaunch.finish(TestUtils.standardLaunchFinishRequest());
	}
}
