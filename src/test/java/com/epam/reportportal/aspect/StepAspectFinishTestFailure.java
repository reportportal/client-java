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
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.test.TestUtils;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.*;

/**
 * @author <a href="mailto:vadzim_hushchanskou@epam.com">Vadzim Hushchanskou</a>
 */
public class StepAspectFinishTestFailure {
	private final StepAspect aspect = new StepAspect();
	private final ListenerParameters params = TestUtils.standardParameters();

	private final String parentId = UUID.randomUUID().toString();
	private final String itemUuid = UUID.randomUUID().toString();

	@Mock(name = "StepAspectFinishTestFailure.class")
	public ReportPortalClient client;
	@Mock
	public MethodSignature methodSignature;
	private Launch myLaunch;

	@BeforeEach
	public void launchSetup() {
		StepAspectCommon.simulateLaunch(client, "launch2");
		StepAspectCommon.simulateStartItemResponse(client, parentId, itemUuid);
		StepAspectCommon.simulateFinishItemResponse(client, itemUuid);
		ReportPortal.create(client, params).newLaunch(TestUtils.standardLaunchRequest(params)).start();
		myLaunch = ReportPortal.create(client, params).newLaunch(TestUtils.standardLaunchRequest(params));
		myLaunch.start();
		StepAspect.setParentId(myLaunch, CommonUtils.createMaybe(parentId));
	}

	/*
	 * Do not finish parent step inside nested step, leads to issue: https://github.com/reportportal/client-java/issues/97
	 */
	@Test
	public void verify_only_nested_step_finished_and_no_parent_steps_on_step_failure() throws NoSuchMethodException {
		Method method = StepAspectCommon.getMethod("testNestedStepSimple");
		aspect.startNestedStep(StepAspectCommon.getJoinPoint(methodSignature, method), method.getAnnotation(Step.class));
		aspect.failedNestedStep(method.getAnnotation(Step.class), new IllegalArgumentException());
		myLaunch.finish(TestUtils.standardLaunchFinishRequest());

		ArgumentCaptor<FinishTestItemRQ> finishRQs = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, timeout(1000).times(1)).finishTestItem(same(itemUuid), finishRQs.capture());

		FinishTestItemRQ resultRq = finishRQs.getValue();
		assertThat(resultRq.getStatus(), equalTo(ItemStatus.FAILED.name()));
		assertThat(resultRq.getIssue(), nullValue());
	}
}
