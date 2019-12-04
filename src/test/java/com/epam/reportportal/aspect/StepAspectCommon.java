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
import com.epam.reportportal.service.Launch;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import io.reactivex.MaybeEmitter;
import io.reactivex.MaybeOnSubscribe;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.aspectj.lang.reflect.SourceLocation;

import java.lang.reflect.Method;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author <a href="mailto:vadzim_hushchanskou@epam.com">Vadzim Hushchanskou</a>
 */
public class StepAspectCommon {
	static void simulateStartItemResponse(Launch launch, Maybe<String> parentIdMaybe, final String itemUuid) {
		when(launch.startTestItem(eq(parentIdMaybe), any(StartTestItemRQ.class))).thenReturn(Maybe.create(emitter -> {
			emitter.onSuccess(itemUuid);
			emitter.onComplete();
		}));
	}

	static <T> Maybe<T> getMaybe(final T response) {
		return Maybe.create(new MaybeOnSubscribe<T>() {
			@Override
			public void subscribe(MaybeEmitter<T> emitter) {
				emitter.onSuccess(response);
				emitter.onComplete();
			}

			@Override
			public String toString() {
				return response.toString();
			}
		});
	}

	static final String TEST_STEP_NAME = "Test step name";
	static final String TEST_STEP_DESCRIPTION = "Test step name";

	@Step(value = TEST_STEP_NAME, description = TEST_STEP_DESCRIPTION)
	public void testNestedStepSimple() {
	}

	static Method getMethod(String name) throws NoSuchMethodException {
		return StepAspectCommon.class.getMethod(name);
	}

	static JoinPoint getJoinPoint(final MethodSignature mock, Method method) {
		when(mock.getMethod()).thenReturn(method);
		when(mock.getParameterNames()).thenReturn(new String[0]);
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

	@Step(value = TEST_STEP_NAME, description = TEST_STEP_DESCRIPTION)
	@Attributes(attributes = @Attribute(key = "test", value = "value"))
	public void testNestedStepAttributeAnnotation() {
	}
}
