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
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * A static class which contains methods to generate Test Case IDs and code references.
 */
public class TestCaseIdUtils {

	private TestCaseIdUtils() {
		throw new IllegalStateException("Static only class");
	}

	private static final Function<List<?>, String> TRANSFORM_PARAMETERS = it -> it.stream()
			.map(String::valueOf)
			.collect(Collectors.joining(",", "[", "]"));

	/**
	 * Generates a text code reference by consuming a {@link Executable}
	 *
	 * @param executable a executable or constructor, the value should not be null
	 * @return a text code reference
	 */
	@Nonnull
	public static String getCodeRef(@Nonnull final Executable executable) {
		if (executable instanceof Constructor) {
			return executable.getName();
		}
		return executable.getDeclaringClass().getCanonicalName() + "." + executable.getName();
	}

	/**
	 * Returns a string of parameters values, separated by comma and embraced by '[]'.
	 *
	 * @param executable a constructor or method
	 * @param parameters a list of parameter values
	 * @param <T>        a type of parameters
	 * @return a string representation of parameters
	 */
	@Nullable
	public static <T> String getParametersForTestCaseId(Executable executable, List<T> parameters) {
		if (executable == null || parameters == null || parameters.isEmpty()) {
			return null;
		}
		Annotation[][] parameterAnnotations = executable.getParameterAnnotations();
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

	/**
	 * Generates a {@link TestCaseIdEntry}
	 *
	 * @param annotation a {@link TestCaseId} annotation instance
	 * @param executable a constructor or method for {@link TestCaseIdKey} scan
	 * @param parameters a list of parameter values
	 * @param <T>        a type of parameters
	 * @return Test Case ID or null if not possible to generate (all parameters are nulls, empty, etc.)
	 */
	@Nullable
	public static <T> TestCaseIdEntry getTestCaseId(@Nullable TestCaseId annotation, @Nullable Executable executable,
			@Nullable List<T> parameters) {
		return getTestCaseId(annotation, executable, null, parameters);
	}

	/**
	 * Generates a {@link TestCaseIdEntry}
	 *
	 * @param annotation a {@link TestCaseId} annotation instance
	 * @param executable a constructor or method for {@link TestCaseIdKey} scan
	 * @param codRef     a code reference to use to generate the ID
	 * @param parameters a list of parameter values
	 * @param <T>        a type of parameters
	 * @return Test Case ID or null if not possible to generate (all parameters are nulls, empty, etc.)
	 */
	@Nullable
	public static <T> TestCaseIdEntry getTestCaseId(@Nullable TestCaseId annotation, @Nullable Executable executable,
			@Nullable String codRef, @Nullable List<T> parameters) {
		if (annotation != null) {
			if (annotation.value().isEmpty()) {
				if (annotation.parametrized()) {
					return ofNullable(getParametersForTestCaseId(executable, parameters)).map(TestCaseIdEntry::new)
							.orElse(ofNullable(codRef).map(c -> getTestCaseId(c, parameters))
									.orElse(getTestCaseId(executable, parameters)));
				} else {
					return ofNullable(codRef).map(c -> getTestCaseId(c, parameters)).orElse(getTestCaseId(executable, parameters));
				}
			} else {
				if (annotation.parametrized()) {
					return ofNullable(getParametersForTestCaseId(executable, parameters)).map(p -> new TestCaseIdEntry(
							annotation.value() + (p.startsWith("[") ? p : "[" + p + "]")))
							.orElse(ofNullable(codRef).map(c -> getTestCaseId(c, parameters))
									.orElse(getTestCaseId(executable, parameters)));
				} else {
					return new TestCaseIdEntry(annotation.value());
				}
			}
		}
		return ofNullable(codRef).map(c -> getTestCaseId(c, parameters)).orElse(getTestCaseId(executable, parameters));
	}

	/**
	 * Generates Test Case ID based on a executable reference and a list of parameters
	 *
	 * @param <T>        parameters type
	 * @param executable a {@link Executable} object
	 * @param parameters a list of parameters
	 * @return a Test Case ID or null
	 */
	@Nullable
	public static <T> TestCaseIdEntry getTestCaseId(@Nullable Executable executable, @Nullable List<T> parameters) {
		return ofNullable(executable).map(m -> getTestCaseId(getCodeRef(m), parameters)).orElse(getTestCaseId(parameters));
	}

	/**
	 * Generates Test Case ID based on a code reference and a list of parameters
	 *
	 * @param <T>        parameters type
	 * @param codeRef    a code reference
	 * @param parameters a list of parameters
	 * @return a Test Case ID or null
	 */
	@Nullable
	public static <T> TestCaseIdEntry getTestCaseId(@Nullable String codeRef, @Nullable List<T> parameters) {
		return ofNullable(codeRef).map(r -> new TestCaseIdEntry(codeRef + ofNullable(parameters).map(TRANSFORM_PARAMETERS).orElse("")))
				.orElse(getTestCaseId(parameters));
	}

	/**
	 * Generates Test Case ID based on a list of parameters
	 *
	 * @param <T>        parameters type
	 * @param parameters a list of parameters
	 * @return a Test Case ID or null
	 */
	@Nullable
	public static <T> TestCaseIdEntry getTestCaseId(@Nullable List<T> parameters) {
		if (parameters == null || parameters.isEmpty()) {
			return null;
		}
		return new TestCaseIdEntry(TRANSFORM_PARAMETERS.apply(parameters));
	}
}
