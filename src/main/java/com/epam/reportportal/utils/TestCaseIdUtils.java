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
import com.epam.reportportal.annotations.TestCaseIdKey;
import com.epam.reportportal.service.item.TestCaseIdEntry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 *
 */
public class TestCaseIdUtils {

	private TestCaseIdUtils() {
		//static only
	}

	private static final Function<List<Object>, String> TRANSFORM_PARAMETERS = it -> it.stream()
			.map(String::valueOf)
			.collect(Collectors.joining(",", "[", "]"));

	public static String getCodeRef(@Nonnull final Method method) {
		return method.getDeclaringClass().getCanonicalName() + "." + method.getName();
	}

	@Nullable
	public static String getParametersForTestCaseId(Method method, List<Object> parameters) {
		if (method == null || parameters == null || parameters.isEmpty()) {
			return null;
		}
		Annotation[][] parameterAnnotations = method.getParameterAnnotations();
		List<Integer> keys = new ArrayList<>();
		for (int paramIndex = 0; paramIndex < parameterAnnotations.length; paramIndex++) {
			for (int annotationIndex = 0; annotationIndex < parameterAnnotations[paramIndex].length; annotationIndex++) {
				Annotation testCaseIdAnnotation = parameterAnnotations[paramIndex][annotationIndex];
				if (testCaseIdAnnotation.annotationType() == TestCaseIdKey.class) {
					keys.add(paramIndex);
				}
			}
		}
		if (keys.isEmpty()) {
			return TRANSFORM_PARAMETERS.apply(parameters);
		}
		if (keys.size() <= 1) {
			return String.valueOf(parameters.get(keys.get(0)));
		}
		return TRANSFORM_PARAMETERS.apply(keys.stream().map(parameters::get).collect(Collectors.toList()));
	}

	public static TestCaseIdEntry getTestCaseId(@Nullable TestCaseId annotation, @Nullable Method method,
			@Nullable List<Object> parameters) {
		if (annotation != null) {
			if (annotation.value().isEmpty()) {
				if (annotation.parametrized()) {
					return ofNullable(getParametersForTestCaseId(method, parameters)).map(TestCaseIdEntry::new)
							.orElse(getTestCaseId(method, parameters));
				} else {
					return getTestCaseId(method, parameters);
				}
			} else {
				if (annotation.parametrized()) {
					return ofNullable(getParametersForTestCaseId(method, parameters)).map(p -> new TestCaseIdEntry(
							annotation.value() + (p.startsWith("[") ? p : "[" + p + "]"))).orElse(getTestCaseId(method, parameters));
				} else {
					return new TestCaseIdEntry(annotation.value());
				}
			}
		}
		return getTestCaseId(method, parameters);
	}

	public static TestCaseIdEntry getTestCaseId(@Nullable Method method, @Nullable List<Object> parameters) {
		return ofNullable(method).map(m -> getTestCaseId(getCodeRef(m), parameters)).orElse(getTestCaseId(parameters));
	}

	public static TestCaseIdEntry getTestCaseId(@Nullable String codeRef, @Nullable List<Object> parameters) {
		return ofNullable(codeRef).map(r -> new TestCaseIdEntry(codeRef + ofNullable(parameters).map(TRANSFORM_PARAMETERS).orElse("")))
				.orElse(getTestCaseId(parameters));
	}

	public static TestCaseIdEntry getTestCaseId(@Nullable List<Object> parameters) {
		if (parameters == null || parameters.isEmpty()) {
			return null;
		}
		return new TestCaseIdEntry(TRANSFORM_PARAMETERS.apply(parameters));
	}
}
