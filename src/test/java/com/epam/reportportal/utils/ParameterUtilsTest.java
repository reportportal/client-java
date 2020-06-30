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
import com.epam.reportportal.service.item.TestCaseIdEntry;
import com.epam.ta.reportportal.ws.model.ParameterResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ParameterUtilsTest {

	public static class ParameterUtilsTestObject {
		public ParameterUtilsTestObject(String string, TestCaseIdEntry id, int number) {
		}
	}

	public static class ParameterUtilsTestObjectKey {
		public ParameterUtilsTestObjectKey(String string, TestCaseIdEntry id, @ParameterKey("index") int number) {
		}
	}

	private static final int FINAL = 16;

	private static final Method method = Arrays.stream(ParameterUtilsTest.class.getDeclaredMethods())
			.filter(m -> "test".equals(m.getName()))
			.findAny()
			.orElse(null);

	private static final Method methodWithKey = Arrays.stream(ParameterUtilsTest.class.getDeclaredMethods())
			.filter(m -> "testKey".equals(m.getName()))
			.findAny()
			.orElse(null);

	private static final Constructor constructor = Arrays.stream(ParameterUtilsTestObject.class.getDeclaredConstructors())
			.findAny()
			.orElse(null);

	private static final Constructor constructorWithKey = Arrays.stream(ParameterUtilsTestObjectKey.class.getDeclaredConstructors())
			.findAny()
			.orElse(null);

	public void test(String string, TestCaseIdEntry id, int number) {
	}

	public void testKey(String string, @ParameterKey("id") TestCaseIdEntry id, int number) {
	}

	@Test
	public void test_method_parameter_reading() {
		Parameter[] parameterOrder = method.getParameters();

		List<Object> parameterValues = Arrays.asList("test", null, 10);
		List<ParameterResource> parameters = ParameterUtils.getParameters(method, parameterValues);

		assertThat(parameters, hasSize(3));
		IntStream.range(0, parameters.size()).forEach(i -> {
			ParameterResource p = parameters.get(i);
			Object v = parameterValues.get(i);
			assertThat(p.getKey(), equalTo(parameterOrder[i].getType().getName()));
			assertThat(p.getValue(), equalTo(v == null ? "NULL" : v.toString()));
		});
	}

	@Test
	public void test_method_constructor_reading() {
		Parameter[] parameterOrder = constructor.getParameters();

		List<Object> parameterValues = Arrays.asList("test", null, 10);
		List<ParameterResource> parameters = ParameterUtils.getParameters(constructor, parameterValues);

		assertThat(parameters, hasSize(3));
		IntStream.range(0, parameters.size()).forEach(i -> {
			ParameterResource p = parameters.get(i);
			Object v = parameterValues.get(i);
			assertThat(p.getKey(), equalTo(parameterOrder[i].getType().getName()));
			assertThat(p.getValue(), equalTo(v == null ? "NULL" : v.toString()));
		});
	}

	@Test
	public void test_method_parameter_key_annotation_reading() {
		Parameter[] parameterOrder = methodWithKey.getParameters();

		List<Object> parameterValues = Arrays.asList("test", null, 10);
		List<ParameterResource> parameters = ParameterUtils.getParameters(methodWithKey, parameterValues);

		assertThat(parameters, hasSize(3));
		IntStream.range(0, parameters.size()).forEach(i -> {
			ParameterResource p = parameters.get(i);
			Object v = parameterValues.get(i);
			if (i != 1) {
				assertThat(p.getKey(), equalTo(parameterOrder[i].getType().getName()));
			} else {
				assertThat(p.getKey(), equalTo("id"));
			}
			assertThat(p.getValue(), equalTo(v == null ? "NULL" : v.toString()));
		});
	}

	@Test
	public void test_constructor_parameter_key_annotation_reading() {
		Parameter[] parameterOrder = constructorWithKey.getParameters();

		List<Object> parameterValues = Arrays.asList("test", null, 10);
		List<ParameterResource> parameters = ParameterUtils.getParameters(constructorWithKey, parameterValues);

		assertThat(parameters, hasSize(3));
		IntStream.range(0, parameters.size()).forEach(i -> {
			ParameterResource p = parameters.get(i);
			Object v = parameterValues.get(i);
			if (i != 2) {
				assertThat(p.getKey(), equalTo(parameterOrder[i].getType().getName()));
			} else {
				assertThat(p.getKey(), equalTo("index"));
			}
			assertThat(p.getValue(), equalTo(v == null ? "NULL" : v.toString()));
		});
	}

	public static Iterable<Object[]> typeConversion() {
		List<Object[]> result = new ArrayList<>();
		result.add(new Object[] { boolean.class, Boolean.class });
		result.add(new Object[] { byte.class, Byte.class });
		result.add(new Object[] { char.class, Character.class });
		result.add(new Object[] { float.class, Float.class });
		result.add(new Object[] { int.class, Integer.class });
		result.add(new Object[] { long.class, Long.class });
		result.add(new Object[] { short.class, Short.class });
		result.add(new Object[] { double.class, Double.class });
		return result;
	}

	@ParameterizedTest
	@MethodSource("typeConversion")
	public void test_type_conversion(Class<?> from, Class<?> to) {
		assertThat(ParameterUtils.toBoxedType(from), sameInstance(to));
	}
}
