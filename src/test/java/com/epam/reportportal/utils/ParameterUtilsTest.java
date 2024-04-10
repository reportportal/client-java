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
import com.epam.reportportal.utils.markdown.MarkdownUtilsTest;
import com.epam.ta.reportportal.ws.reporting.ParameterResource;
import org.apache.commons.lang3.tuple.Pair;
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
		@SuppressWarnings("unused")
		public ParameterUtilsTestObject(String string, TestCaseIdEntry id, int number) {
		}
	}

	public static class ParameterUtilsTestObjectKey {
		@SuppressWarnings("unused")
		public ParameterUtilsTestObjectKey(String string, TestCaseIdEntry id, @ParameterKey("index") int number) {
		}
	}

	private static final Method method = Arrays.stream(ParameterUtilsTest.class.getDeclaredMethods())
			.filter(m -> "test".equals(m.getName()))
			.findAny()
			.orElse(null);

	private static final Method methodWithKey = Arrays.stream(ParameterUtilsTest.class.getDeclaredMethods())
			.filter(m -> "testKey".equals(m.getName()))
			.findAny()
			.orElse(null);

	private static final Constructor<?> constructor = Arrays.stream(ParameterUtilsTestObject.class.getDeclaredConstructors())
			.findAny()
			.orElse(null);

	private static final Constructor<?> constructorWithKey = Arrays.stream(ParameterUtilsTestObjectKey.class.getDeclaredConstructors())
			.findAny()
			.orElse(null);

	@SuppressWarnings("unused")
	public void test(String string, TestCaseIdEntry id, int number) {
	}

	@SuppressWarnings("unused")
	public void testKey(String string, @ParameterKey("id") TestCaseIdEntry id, int number) {
	}

	private static final List<?> PARAM_VALUES = Arrays.asList("test", null, 10);

	@Test
	public void test_method_parameter_reading() {
		Parameter[] parameterOrder = method.getParameters();

		List<ParameterResource> parameters = ParameterUtils.getParameters(method, PARAM_VALUES);

		assertThat(parameters, hasSize(3));
		IntStream.range(0, parameters.size()).forEach(i -> {
			ParameterResource p = parameters.get(i);
			Object v = PARAM_VALUES.get(i);
			assertThat(p.getKey(), equalTo(parameterOrder[i].getType().getName()));
			assertThat(p.getValue(), equalTo(v == null ? "NULL" : v.toString()));
		});
	}

	@Test
	public void test_method_constructor_reading() {
		Parameter[] parameterOrder = constructor.getParameters();

		List<ParameterResource> parameters = ParameterUtils.getParameters(constructor, PARAM_VALUES);

		assertThat(parameters, hasSize(3));
		IntStream.range(0, parameters.size()).forEach(i -> {
			ParameterResource p = parameters.get(i);
			Object v = PARAM_VALUES.get(i);
			assertThat(p.getKey(), equalTo(parameterOrder[i].getType().getName()));
			assertThat(p.getValue(), equalTo(v == null ? "NULL" : v.toString()));
		});
	}

	@Test
	public void test_method_parameter_key_annotation_reading() {
		Parameter[] parameterOrder = methodWithKey.getParameters();

		List<ParameterResource> parameters = ParameterUtils.getParameters(methodWithKey, PARAM_VALUES);

		assertThat(parameters, hasSize(3));
		IntStream.range(0, parameters.size()).forEach(i -> {
			ParameterResource p = parameters.get(i);
			Object v = PARAM_VALUES.get(i);
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

		List<ParameterResource> parameters = ParameterUtils.getParameters(constructorWithKey, PARAM_VALUES);

		assertThat(parameters, hasSize(3));
		IntStream.range(0, parameters.size()).forEach(i -> {
			ParameterResource p = parameters.get(i);
			Object v = PARAM_VALUES.get(i);
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

	private static final List<Pair<String, Object>> PARAM_KEY_VALUES = Arrays.asList(Pair.of("str", "test"),
			Pair.of("my_id", null),
			Pair.of("number", 10)
	);

	@Test
	public void test_method_parameter_reading_codref() {
		String codeRef = TestCaseIdUtils.getCodeRef(method);
		Parameter[] parameterOrder = method.getParameters();

		List<ParameterResource> parameters = ParameterUtils.getParameters(codeRef, PARAM_KEY_VALUES);

		assertThat(parameters, hasSize(3));
		IntStream.range(0, parameters.size()).forEach(i -> {
			ParameterResource p = parameters.get(i);
			Object v = PARAM_KEY_VALUES.get(i).getValue();
			assertThat(p.getKey(), equalTo(parameterOrder[i].getType().getName()));
			assertThat(p.getValue(), equalTo(v == null ? "NULL" : v.toString()));
		});
	}

	@Test
	public void test_method_parameter_key_annotation_reading_codref() {
		String codeRef = TestCaseIdUtils.getCodeRef(methodWithKey);
		Parameter[] parameterOrder = methodWithKey.getParameters();

		List<ParameterResource> parameters = ParameterUtils.getParameters(codeRef, PARAM_KEY_VALUES);

		assertThat(parameters, hasSize(3));
		IntStream.range(0, parameters.size()).forEach(i -> {
			ParameterResource p = parameters.get(i);
			Object v = PARAM_KEY_VALUES.get(i).getValue();
			if (i != 1) {
				assertThat(p.getKey(), equalTo(parameterOrder[i].getType().getName()));
			} else {
				assertThat(p.getKey(), equalTo("id"));
			}
			assertThat(p.getValue(), equalTo(v == null ? "NULL" : v.toString()));
		});
	}

	@Test
	public void test_constructor_parameter_key_annotation_reading_codref() {
		String codeRef = TestCaseIdUtils.getCodeRef(constructorWithKey);
		Parameter[] parameterOrder = constructorWithKey.getParameters();

		List<ParameterResource> parameters = ParameterUtils.getParameters(codeRef, PARAM_KEY_VALUES);

		assertThat(parameters, hasSize(3));
		IntStream.range(0, parameters.size()).forEach(i -> {
			ParameterResource p = parameters.get(i);
			Object v = PARAM_KEY_VALUES.get(i).getValue();
			if (i != 2) {
				assertThat(p.getKey(), equalTo(parameterOrder[i].getType().getName()));
			} else {
				assertThat(p.getKey(), equalTo("index"));
			}
			assertThat(p.getValue(), equalTo(v == null ? "NULL" : v.toString()));
		});
	}

	@Test
	public void test_non_existent_codref() {
		String codeRef = "my.not.existent.code.ref";

		List<ParameterResource> parameters = ParameterUtils.getParameters(codeRef, PARAM_KEY_VALUES);

		assertThat(parameters, hasSize(3));
		IntStream.range(0, parameters.size()).forEach(i -> {
			ParameterResource p = parameters.get(i);
			Object v = PARAM_KEY_VALUES.get(i).getValue();
			assertThat(p.getKey(), equalTo(PARAM_KEY_VALUES.get(i).getKey()));
			assertThat(p.getValue(), equalTo(v == null ? "NULL" : v.toString()));
		});
	}

	@Test
	public void test_parameters_format() {
		ParameterResource varA = new ParameterResource();
		ParameterResource varB = new ParameterResource();
		ParameterResource result = new ParameterResource();

		varA.setKey("var_a");
		varB.setKey("var_b");
		result.setKey("result");
		varA.setValue("2");
		varB.setValue("2");
		result.setValue("4");

		assertThat(ParameterUtils.formatParametersAsTable(Arrays.asList(varA, varB, result)),
				equalTo(MarkdownUtilsTest.ONE_ROW_EXPECTED_TABLE));
	}
}
