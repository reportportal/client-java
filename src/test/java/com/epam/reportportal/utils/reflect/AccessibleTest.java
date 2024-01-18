/*
 * Copyright 2024 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.reportportal.utils.reflect;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("unused")
public class AccessibleTest extends BaseReflectTest {

	public static final String PUBLIC_FIELD_VALUE = "public_field";
	public static final String PRIVATE_FIELD_VALUE = "private_field";
	public static final String PUBLIC_METHOD_NO_PARAMS_VALUE = "public_method_no_params";
	public static final String PRIVATE_METHOD_NO_PARAMS_VALUE = "private_method_no_params";
	public static final String PUBLIC_METHOD_PARAMS_VALUE = "public_method_params";
	public static final String PRIVATE_METHOD_PARAMS_VALUE = "private_method_params";

	public final String publicField = PUBLIC_FIELD_VALUE;
	private final String privateField = PRIVATE_FIELD_VALUE;

	public String publicMethodNoParams() {
		return PUBLIC_METHOD_NO_PARAMS_VALUE;
	}

	private String privateMethodNoParams() {
		return PRIVATE_METHOD_NO_PARAMS_VALUE;
	}

	public String publicMethodParams(String param1, String param2) {
		return PUBLIC_METHOD_PARAMS_VALUE;
	}

	private String privateMethodParams(String param1, String param2) {
		return PRIVATE_METHOD_PARAMS_VALUE;
	}

	public static Stream<Arguments> fieldData() throws NoSuchFieldException {
		return Stream.of(Arguments.of("publicField", AccessibleTest.class.getField("publicField"), PUBLIC_FIELD_VALUE),
				Arguments.of("privateField", AccessibleTest.class.getDeclaredField("privateField"), PRIVATE_FIELD_VALUE),
				Arguments.of("publicBaseField", BaseReflectTest.class.getField("publicBaseField"), PUBLIC_BASE_FIELD_VALUE),
				Arguments.of("privateBaseField", BaseReflectTest.class.getDeclaredField("privateBaseField"), PRIVATE_BASE_FIELD_VALUE)
		);
	}

	@ParameterizedTest
	@MethodSource("fieldData")
	public void test_field_access_positive(String fieldName, Field field, String expectedValue) throws NoSuchFieldException {
		Accessible accessible = Accessible.on(this);
		AccessibleField accessibleField = accessible.field(fieldName);
		assertThat(accessibleField.getValue(), equalTo(expectedValue));
		assertThat(accessibleField.getType(), equalTo(String.class));
		accessibleField = accessible.field(field);
		assertThat(accessibleField.getValue(), equalTo(expectedValue));
		assertThat(accessibleField.getType(), equalTo(String.class));
	}

	@Test
	public void test_field_access_negative() {
		Accessible accessible = Accessible.on(this);
		NoSuchFieldException throwable = assertThrows(NoSuchFieldException.class, () -> accessible.field("noSuchField"));
		assertThat(throwable.getMessage(), containsString("noSuchField"));
	}

	public static Stream<Arguments> methodData() throws NoSuchMethodException {
		return Stream.of(Arguments.of("publicMethodNoParams",
						AccessibleTest.class.getMethod("publicMethodNoParams"),
						null,
						PUBLIC_METHOD_NO_PARAMS_VALUE
				), Arguments.of("privateMethodNoParams",
						AccessibleTest.class.getDeclaredMethod("privateMethodNoParams"),
						null,
						PRIVATE_METHOD_NO_PARAMS_VALUE
				), Arguments.of("publicMethodParams",
						AccessibleTest.class.getMethod("publicMethodParams", String.class, String.class),
						new String[] { "a", "b" },
						PUBLIC_METHOD_PARAMS_VALUE
				), Arguments.of("privateMethodParams",
						AccessibleTest.class.getDeclaredMethod("privateMethodParams", String.class, String.class),
						new String[] { "a", "b" },
						PRIVATE_METHOD_PARAMS_VALUE
				),

				Arguments.of("publicBaseMethodNoParams",
						BaseReflectTest.class.getMethod("publicBaseMethodNoParams"),
						null,
						PUBLIC_BASE_METHOD_NO_PARAMS_VALUE
				), Arguments.of("privateBaseMethodNoParams",
						BaseReflectTest.class.getDeclaredMethod("privateBaseMethodNoParams"),
						null,
						PRIVATE_BASE_METHOD_NO_PARAMS_VALUE
				), Arguments.of("publicBaseMethodParams",
						BaseReflectTest.class.getMethod("publicBaseMethodParams", String.class, String.class),
						new String[] { "a", "b" },
						PUBLIC_BASE_METHOD_PARAMS_VALUE
				), Arguments.of("privateBaseMethodParams",
						BaseReflectTest.class.getDeclaredMethod("privateBaseMethodParams", String.class, String.class),
						new String[] { "a", "b" },
						PRIVATE_BASE_METHOD_PARAMS_VALUE
				)
		);
	}

	@ParameterizedTest
	@MethodSource("methodData")
	public void test_method_access_positive(String methodName, Method method, Object[] params, String expectedValue) throws Throwable {
		Accessible accessible = Accessible.on(this);
		Class<?>[] paramsClasses = params == null ? new Class<?>[0] : Arrays.stream(params).map(Object::getClass).toArray(Class<?>[]::new);
		AccessibleMethod accessibleMethod = accessible.method(methodName, paramsClasses);
		assertThat(accessibleMethod.invoke(params), equalTo(expectedValue));
		accessibleMethod = accessible.method(method);
		assertThat(accessibleMethod.invoke(params), equalTo(expectedValue));
	}

	@Test
	public void test_method_access_negative() {
		Accessible accessible = Accessible.on(this);
		NoSuchMethodException throwable = assertThrows(NoSuchMethodException.class, () -> accessible.method("noSuchMethod"));
		assertThat(throwable.getMessage(), containsString("noSuchMethod"));
	}
}
