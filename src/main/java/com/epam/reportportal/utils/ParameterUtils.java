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
import com.epam.ta.reportportal.ws.model.ParameterResource;

import javax.annotation.Nonnull;
import java.lang.annotation.Annotation;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Optional.ofNullable;

/**
 * An utility class for parameters read and processing.
 */
public class ParameterUtils {

	private ParameterUtils() {
	}

	/**
	 * Read all parameters from a method or a constructor and converts in into a list of {@link ParameterResource}.
	 * Respects {@link ParameterKey} annotation.
	 *
	 * @param method          a method to read parameters
	 * @param parameterValues a source of parameter values
	 * @return a list of parameter POJOs with fulfilled name and value.
	 */
	public static @Nonnull
	List<ParameterResource> getParameters(@Nonnull final Executable method, final List<Object> parameterValues) {
		List<Object> values = ofNullable(parameterValues).orElse(Collections.emptyList());
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
			res.setValue(ofNullable(i < values.size() ? values.get(i) : null).orElse("NULL").toString());
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
}
