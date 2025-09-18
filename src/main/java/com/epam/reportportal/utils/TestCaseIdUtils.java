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
import com.epam.reportportal.utils.formatting.templating.TemplateConfiguration;
import com.epam.reportportal.utils.formatting.templating.TemplateProcessing;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
	 * @param executable an executable or constructor, the value should not be null
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
		if (keys.size() == 1) {
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
		return getTestCaseId(annotation, executable, codRef, parameters, null);
	}

	/**
	 * Generates a {@link TestCaseIdEntry}
	 *
	 * @param annotation   a {@link TestCaseId} annotation instance
	 * @param executable   a constructor or method for {@link TestCaseIdKey} scan
	 * @param codRef       a code reference to use to generate the ID
	 * @param parameters   a list of parameter values
	 * @param testInstance an instance of the current test, used in template processing
	 * @param <T>          a type of parameters
	 * @return Test Case ID or null if not possible to generate (all parameters are nulls, empty, etc.)
	 */
	@Nullable
	public static <T> TestCaseIdEntry getTestCaseId(@Nullable TestCaseId annotation, @Nullable Executable executable,
			@Nullable String codRef, @Nullable List<T> parameters, @Nullable Object testInstance) {
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
				String idTemplate = annotation.value();
				TemplateConfiguration templateConfig = new TemplateConfiguration(annotation.config());
				Map<String, Object> parametersMap = new HashMap<>();
				ofNullable(parameters).ifPresent(params -> IntStream.range(0, params.size())
						.boxed()
						.forEach(i -> parametersMap.put(i.toString(), params.get(i))));
				String id = TemplateProcessing.processTemplate(idTemplate, testInstance, executable, parametersMap, templateConfig);

				if (annotation.parametrized()) {
					String resultParameters = getParametersForTestCaseId(executable, parameters);
					return ofNullable(resultParameters).map(p -> new TestCaseIdEntry(id + (p.startsWith("[") ? p : "[" + p + "]")))
							.orElse(ofNullable(codRef).map(c -> getTestCaseId(c, parameters))
									.orElse(getTestCaseId(executable, parameters)));
				} else {
					return new TestCaseIdEntry(id);
				}
			}
		}
		return ofNullable(codRef).map(c -> getTestCaseId(c, parameters)).orElse(getTestCaseId(executable, parameters));
	}

	/**
	 * Generates Test Case ID based on an executable reference and a list of parameters
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
