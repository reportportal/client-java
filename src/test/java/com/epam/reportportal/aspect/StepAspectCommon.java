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
import com.epam.reportportal.annotations.attribute.Attribute;
import com.epam.reportportal.annotations.attribute.Attributes;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.test.TestUtils;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.*;
import com.epam.ta.reportportal.ws.model.item.ItemCreatedRS;
import okhttp3.MultipartBody;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.aspectj.lang.reflect.SourceLocation;

import java.lang.reflect.Method;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * @author <a href="mailto:vadzim_hushchanskou@epam.com">Vadzim Hushchanskou</a>
 */
public class StepAspectCommon {

	static void simulateLaunch(ReportPortalClient client, String launchId) {
		when(client.startLaunch(any())).thenReturn(TestUtils.startLaunchResponse(launchId));
		lenient().when(client.log(any(MultipartBody.class))).thenReturn(CommonUtils.createMaybe(new BatchSaveOperatingRS()));
		when(client.finishLaunch(anyString(),
				any(FinishExecutionRQ.class)
		)).thenReturn(CommonUtils.createMaybe(new OperationCompletionRS()));
	}

	static void simulateStartItemResponse(ReportPortalClient client, String parentId, final String itemUuid) {
		when(client.startTestItem(same(parentId),
				any(StartTestItemRQ.class)
		)).thenReturn(CommonUtils.createMaybe(new ItemCreatedRS(itemUuid, itemUuid)));
	}

	static void simulateFinishItemResponse(ReportPortalClient client, String id) {
		when(client.finishTestItem(same(id), any(FinishTestItemRQ.class))).thenReturn(CommonUtils.createMaybe(new OperationCompletionRS()));
	}

	static final String TEST_STEP_NAME = "Test step name";
	static final String TEST_STEP_DESCRIPTION = "Test step name";

	@Step(value = TEST_STEP_NAME, description = TEST_STEP_DESCRIPTION)
	@SuppressWarnings("unused")
	public void testNestedStepSimple() {
	}

	static Method getMethod(String name) throws NoSuchMethodException {
		return StepAspectCommon.class.getMethod(name);
	}

	public static JoinPoint getJoinPointNoParams(final MethodSignature mock, Method method) {
		when(mock.getMethod()).thenReturn(method);
		return new JoinPoint() {
			@Override
			public String toShortString() {
				return null;
			}

			@Override
			public String toLongString() {
				return null;
			}

			@Override
			public Object getThis() {
				return null;
			}

			@Override
			public Object getTarget() {
				return null;
			}

			@Override
			public Object[] getArgs() {
				return new Object[0];
			}

			@Override
			public Signature getSignature() {
				return mock;
			}

			@Override
			public SourceLocation getSourceLocation() {
				return null;
			}

			@Override
			public String getKind() {
				return null;
			}

			@Override
			public StaticPart getStaticPart() {
				return null;
			}
		};
	}

	public static JoinPoint getJoinPoint(final MethodSignature mock, Method method) {
		JoinPoint joinPoint = getJoinPointNoParams(mock, method);
		when(mock.getParameterNames()).thenReturn(new String[0]);
		return joinPoint;
	}

	@Step(value = TEST_STEP_NAME, description = TEST_STEP_DESCRIPTION)
	@Attributes(attributes = @Attribute(key = "test", value = "value"))
	@SuppressWarnings("unused")
	public void testNestedStepAttributeAnnotation() {
	}
}
