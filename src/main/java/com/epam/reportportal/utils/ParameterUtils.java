/*
 *  Copyright 2020 EPAM Systems
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

package com.epam.reportportal.utils;

import com.epam.reportportal.annotations.ParameterKey;
import com.epam.reportportal.utils.formatting.MarkdownUtils;
import com.epam.ta.reportportal.ws.model.ParameterResource;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

/**
 * A utility class for parameters read and processing.
 */
public class ParameterUtils {

	public static final String NULL_VALUE = "NULL";

	private ParameterUtils() {
		throw new IllegalStateException("Static only class");
	}

	/**
	 * Read all parameters from a method or a constructor and converts in into a list of {@link ParameterResource}.
	 * Respects {@link ParameterKey} annotation.
	 *
	 * @param <T>             parameter values type
	 * @param method          a method to read parameters
	 * @param parameterValues a source of parameter values
	 * @return a list of parameter POJOs with fulfilled name and value.
	 */
	@Nonnull
	public static <T> List<ParameterResource> getParameters(@Nonnull final Executable method, @Nullable final List<T> parameterValues) {
		List<?> values = ofNullable(parameterValues).orElse(Collections.emptyList());
		Parameter[] params = method.getParameters();
		Annotation[][] parameterAnnotations = method.getParameterAnnotations();
		return IntStream.range(0, params.length).boxed().map(i -> {
			ParameterResource res = new ParameterResource();
			String parameterName = Arrays.stream(parameterAnnotations[i])
					.filter(a -> ParameterKey.class.equals(a.annotationType()))
					.map(a -> ((ParameterKey) a).value())
					.findFirst()
					.orElseGet(() -> params[i].getType().getName());
			res.setKey(parameterName);
			res.setValue(ofNullable(i < values.size() ? values.get(i) : null).map(String::valueOf).orElse(NULL_VALUE));
			return res;
		}).collect(Collectors.toList());
	}

	/**
	 * Converts primitive type to a corresponding boxed class, or returns the same instance if the input is not primitive.
	 *
	 * @param primitiveType a class which needs to be boxed
	 * @return boxed class type
	 */
	public static Class<?> toBoxedType(@Nonnull Class<?> primitiveType) {
		if (primitiveType.isPrimitive()) {
			if (primitiveType == Boolean.TYPE) {
				return Boolean.class;
			} else if (primitiveType == Byte.TYPE) {
				return Byte.class;
			} else if (primitiveType == Character.TYPE) {
				return Character.class;
			} else if (primitiveType == Short.TYPE) {
				return Short.class;
			} else if (primitiveType == Integer.TYPE) {
				return Integer.class;
			} else if (primitiveType == Long.TYPE) {
				return Long.class;
			} else if (primitiveType == Float.TYPE) {
				return Float.class;
			} else if (primitiveType == Double.TYPE) {
				return Double.class;
			} else if (primitiveType == Void.TYPE) {
				return Void.class;
			} else {
				return null;
			}
		} else {
			return primitiveType;
		}
	}

	@Nonnull
	private static <T> List<ParameterResource> getParameters(@Nullable final List<Pair<String, T>> arguments) {
		return ofNullable(arguments).map(args -> args.stream().map(a -> {
			ParameterResource p = new ParameterResource();
			p.setKey(a.getKey());
			p.setValue(ofNullable(a.getValue()).map(String::valueOf).orElse(NULL_VALUE));
			return p;
		}).collect(Collectors.toList())).orElse(Collections.emptyList());
	}

	/**
	 * Read all parameters from a method or a constructor and converts in into a list of {@link ParameterResource}.
	 * Respects {@link ParameterKey} annotation.
	 *
	 * @param <T>        parameter values type
	 * @param codeRef    a method reference to read parameters
	 * @param parameters a source of parameter values and parameter names if not set by {@link ParameterKey} annotation
	 * @return a list of parameter POJOs with fulfilled name and value.
	 */
	@Nonnull
	public static <T> List<ParameterResource> getParameters(@Nullable final String codeRef,
			@Nullable final List<Pair<String, T>> parameters) {
		Optional<List<Object>> paramValues = ofNullable(parameters).map(args -> args.stream()
				.map(a -> (Object) a.getValue())
				.collect(Collectors.toList()));

		return ofNullable(codeRef).flatMap(cr -> {
			int lastDelimiterIndex = cr.lastIndexOf('.');
			String className = cr.substring(0, lastDelimiterIndex);
			String methodName = cr.substring(lastDelimiterIndex + 1);

			Optional<Class<?>> testStepClass;
			try {
				testStepClass = Optional.of(Class.forName(className));
			} catch (ClassNotFoundException e1) {
				try {
					testStepClass = Optional.of(Class.forName(cr));
				} catch (ClassNotFoundException e2) {
					testStepClass = Optional.empty();
				}
			}
			return testStepClass.flatMap(cl -> Stream.concat(
							Arrays.stream(cl.getDeclaredMethods()),
							Arrays.stream(cl.getDeclaredConstructors())
					)
					.filter(m -> methodName.equals(m.getName()) || cr.equals(m.getName()))
					.filter(m -> m.getParameterCount() == paramValues.map(List::size).orElse(0))
					.findAny());
		}).map(m -> ParameterUtils.getParameters(m, paramValues.orElse(null))).orElse(getParameters(parameters));
	}

	/**
	 * Format parameters as Markdown table.
	 *
	 * @param parameters list of parameters
	 * @return text representation of parameter table
	 */
	@Nonnull
	public static String formatParametersAsTable(@Nonnull List<ParameterResource> parameters) {
		List<List<String>> tableList = new ArrayList<>();
		tableList.add(parameters.stream().map(ParameterResource::getKey).collect(Collectors.toList()));
		tableList.add(parameters.stream().map(ParameterResource::getValue).collect(Collectors.toList()));
		return MarkdownUtils.formatDataTable(tableList);
	}
}
