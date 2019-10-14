/*
 * Copyright 2019 EPAM Systems
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

package com.epam.reportportal.utils;

import com.epam.reportportal.annotations.TestCaseId;
import com.epam.reportportal.annotations.TestCaseIdTemplate;
import io.reactivex.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class TestCaseIdUtils {

	private TestCaseIdUtils() {
		//static only
	}

	@Nullable
	public static Integer getTestCaseId(TestCaseId testCaseId, Method method, Object... parameters) {
		Annotation[][] parameterAnnotations = method.getParameterAnnotations();
		for (int paramIndex = 0; paramIndex < parameterAnnotations.length; paramIndex++) {
			for (int annotationIndex = 0; annotationIndex < parameterAnnotations[paramIndex].length; annotationIndex++) {
				Annotation testCaseIdAnnotation = parameterAnnotations[paramIndex][annotationIndex];
				if (testCaseIdAnnotation.annotationType() == TestCaseIdTemplate.class) {
					return getTestCaseId((TestCaseIdTemplate) testCaseIdAnnotation, testCaseId.pattern(), paramIndex, parameters);
				}
			}
		}
		return null;
	}

	@Nullable
	private static Integer getTestCaseId(TestCaseIdTemplate testCaseIdTemplate, String pattern, int paramIndex, Object... parameters) {
		if (testCaseIdTemplate.value().equalsIgnoreCase(pattern)) {
			Object testCaseIdParam = parameters[paramIndex];
			if (testCaseIdParam != null) {
				return testCaseIdParam.hashCode();
			}
		}
		return null;
	}
}
