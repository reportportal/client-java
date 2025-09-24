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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
@SuppressWarnings("unused")
public class TestCaseIdUtilsTest {

	private final String testField = "test_field";

	@TestCaseId(parametrized = true)
	public void testCaseAnnotationTest(String firstParam, @TestCaseIdKey String id) {

	}

	@Test
	public void test_code_reference_generation_from_a_method() throws NoSuchMethodException {
		Method method = TestCaseIdUtilsTest.class.getDeclaredMethod("testCaseAnnotationTest", String.class, String.class);
		String codeRef = TestCaseIdUtils.getCodeRef(method);

		assertThat(codeRef, equalTo(getClass().getCanonicalName() + ".testCaseAnnotationTest"));
	}

	@Test
	public void test_case_id_should_use_one_parameter_if_one_key_specified() throws NoSuchMethodException {
		Method method = TestCaseIdUtilsTest.class.getDeclaredMethod("testCaseAnnotationTest", String.class, String.class);
		TestCaseId annotation = method.getAnnotation(TestCaseId.class);
		String expectedTestCaseId = "5";
		TestCaseIdEntry testCaseIdEntry = TestCaseIdUtils.getTestCaseId(
				annotation,
				method,
				Arrays.asList("firstParam", expectedTestCaseId)
		);

		assertThat(testCaseIdEntry, notNullValue());
		assertThat(testCaseIdEntry.getId(), equalTo(expectedTestCaseId));
	}

	@TestCaseId(parametrized = true)
	public void testCaseIdAnnotationParametersNoKeyTest(String firstParam, int id) {

	}

	@Test
	public void test_case_id_should_use_all_parameters_if_no_key_specified() throws NoSuchMethodException {
		Method method = TestCaseIdUtilsTest.class.getDeclaredMethod("testCaseIdAnnotationParametersNoKeyTest", String.class, Integer.TYPE);
		TestCaseId annotation = method.getAnnotation(TestCaseId.class);
		String expectedTestCaseId = "[firstParam,5]";
		TestCaseIdEntry testCaseIdEntry = TestCaseIdUtils.getTestCaseId(annotation, method, Arrays.asList("firstParam", 5));

		assertThat(testCaseIdEntry, notNullValue());
		assertThat(testCaseIdEntry.getId(), equalTo(expectedTestCaseId));
	}

	@Test
	public void test_case_id_should_not_fail_if_parameters_are_null() throws NoSuchMethodException {
		Method method = TestCaseIdUtilsTest.class.getDeclaredMethod("testCaseIdAnnotationParametersNoKeyTest", String.class, Integer.TYPE);
		TestCaseId annotation = method.getAnnotation(TestCaseId.class);
		String expectedTestCaseId = "[null,5]";
		TestCaseIdEntry testCaseIdEntry = TestCaseIdUtils.getTestCaseId(annotation, method, Arrays.asList(null, 5));

		assertThat(testCaseIdEntry, notNullValue());
		assertThat(testCaseIdEntry.getId(), equalTo(expectedTestCaseId));
	}

	@TestCaseId
	public void testCaseIdEmptyAnnotationTest(int id, String firstParam) {

	}

	@Test
	public void test_case_id_should_use_codref_and_parameters_if_nothing_is_specified_in_annotation() throws NoSuchMethodException {
		Method method = TestCaseIdUtilsTest.class.getDeclaredMethod("testCaseIdEmptyAnnotationTest", Integer.TYPE, String.class);
		TestCaseId annotation = method.getAnnotation(TestCaseId.class);
		String expectedTestCaseId = "com.epam.reportportal.utils.TestCaseIdUtilsTest.testCaseIdEmptyAnnotationTest[5,firstParam]";
		TestCaseIdEntry testCaseIdEntry = TestCaseIdUtils.getTestCaseId(annotation, method, Arrays.asList(5, "firstParam"));

		assertThat(testCaseIdEntry, notNullValue());
		assertThat(testCaseIdEntry.getId(), equalTo(expectedTestCaseId));
	}

	@TestCaseId("my test case id")
	public void testCaseIdAnnotationTest(@TestCaseIdKey int id, String firstParam) {

	}

	@Test
	public void test_case_id_should_use_annotation_and_not_fail_if_parameters_are_null() throws NoSuchMethodException {
		Method method = TestCaseIdUtilsTest.class.getDeclaredMethod("testCaseIdAnnotationTest", Integer.TYPE, String.class);
		TestCaseId annotation = method.getAnnotation(TestCaseId.class);
		String expectedTestCaseId = "my test case id";
		TestCaseIdEntry testCaseIdEntry = TestCaseIdUtils.getTestCaseId(annotation, method, Arrays.asList(5, null));

		assertThat(testCaseIdEntry, notNullValue());
		assertThat(testCaseIdEntry.getId(), equalTo(expectedTestCaseId));
	}

	@Test
	public void test_case_id_should_use_annotation_value_if_it_is_only_field_specified() throws NoSuchMethodException {
		Method method = TestCaseIdUtilsTest.class.getDeclaredMethod("testCaseIdAnnotationTest", Integer.TYPE, String.class);
		TestCaseId annotation = method.getAnnotation(TestCaseId.class);
		String expectedTestCaseId = "my test case id";
		TestCaseIdEntry testCaseIdEntry = TestCaseIdUtils.getTestCaseId(annotation, method, Arrays.asList(5, "firstParam"));

		assertThat(testCaseIdEntry, notNullValue());
		assertThat(testCaseIdEntry.getId(), equalTo(expectedTestCaseId));
	}

	@TestCaseId(value = "my test case id", parametrized = true)
	public void testCaseIdAnnotationOneParamTest(@TestCaseIdKey int id, String firstParam) {

	}

	@Test
	public void test_case_id_should_use_annotation_value_and_one_marked_parameter() throws NoSuchMethodException {
		Method method = TestCaseIdUtilsTest.class.getDeclaredMethod("testCaseIdAnnotationOneParamTest", Integer.TYPE, String.class);
		TestCaseId annotation = method.getAnnotation(TestCaseId.class);
		String expectedTestCaseId = "my test case id[5]";
		TestCaseIdEntry testCaseIdEntry = TestCaseIdUtils.getTestCaseId(annotation, method, Arrays.asList(5, "firstParam"));

		assertThat(testCaseIdEntry, notNullValue());
		assertThat(testCaseIdEntry.getId(), equalTo(expectedTestCaseId));
	}

	@TestCaseId(value = "my test case id", parametrized = true)
	public void testCaseIdAnnotationParameterizedNoKeyParam(int id, String firstParam) {

	}

	@Test
	public void test_case_id_should_use_annotation_value_and_parameters() throws NoSuchMethodException {
		Method method = TestCaseIdUtilsTest.class.getDeclaredMethod(
				"testCaseIdAnnotationParameterizedNoKeyParam",
				Integer.TYPE,
				String.class
		);
		TestCaseId annotation = method.getAnnotation(TestCaseId.class);
		String expectedTestCaseId = "my test case id[5,firstParam]";
		TestCaseIdEntry testCaseIdEntry = TestCaseIdUtils.getTestCaseId(annotation, method, Arrays.asList(5, "firstParam"));

		assertThat(testCaseIdEntry, notNullValue());
		assertThat(testCaseIdEntry.getId(), equalTo(expectedTestCaseId));
	}

	@TestCaseId(value = "my test case id", parametrized = true)
	public void testCaseIdAnnotationTwoParamTest(@TestCaseIdKey int id, String stringParam, @TestCaseIdKey Boolean bool) {

	}

	@Test
	public void test_case_id_should_use_annotation_value_and_two_marked_parameter() throws NoSuchMethodException {
		Method method = TestCaseIdUtilsTest.class.getDeclaredMethod(
				"testCaseIdAnnotationTwoParamTest",
				Integer.TYPE,
				String.class,
				Boolean.class
		);
		TestCaseId annotation = method.getAnnotation(TestCaseId.class);
		String expectedTestCaseId = "my test case id[5,true]";
		TestCaseIdEntry testCaseIdEntry = TestCaseIdUtils.getTestCaseId(annotation, method, Arrays.asList(5, "stringParam", Boolean.TRUE));

		assertThat(testCaseIdEntry, notNullValue());
		assertThat(testCaseIdEntry.getId(), equalTo(expectedTestCaseId));
	}

	public void noTestCaseIdAnnotationParameterized(int id, String firstParam) {

	}

	@Test
	public void test_case_id_should_use_code_ref_and_parameters() throws NoSuchMethodException {
		Method method = TestCaseIdUtilsTest.class.getDeclaredMethod("noTestCaseIdAnnotationParameterized", Integer.TYPE, String.class);
		TestCaseId annotation = method.getAnnotation(TestCaseId.class);
		String expectedTestCaseId = getClass().getCanonicalName() + ".noTestCaseIdAnnotationParameterized[5,firstParam]";
		TestCaseIdEntry testCaseIdEntry = TestCaseIdUtils.getTestCaseId(annotation, method, Arrays.asList(5, "firstParam"));

		assertThat(testCaseIdEntry, notNullValue());
		assertThat(testCaseIdEntry.getId(), equalTo(expectedTestCaseId));
	}

	public void noTestCaseIdAnnotationNoParameters() {

	}

	@Test
	public void test_case_id_should_use_code_ref_if_no_parameters() throws NoSuchMethodException {
		Method method = TestCaseIdUtilsTest.class.getDeclaredMethod("noTestCaseIdAnnotationNoParameters");
		TestCaseId annotation = method.getAnnotation(TestCaseId.class);
		String expectedTestCaseId = getClass().getCanonicalName() + ".noTestCaseIdAnnotationNoParameters";
		TestCaseIdEntry testCaseIdEntry = TestCaseIdUtils.getTestCaseId(annotation, method, null);

		assertThat(testCaseIdEntry, notNullValue());
		assertThat(testCaseIdEntry.getId(), equalTo(expectedTestCaseId));
	}

	@Test
	public void test_case_id_null() {
		TestCaseIdEntry testCaseIdEntry = TestCaseIdUtils.getTestCaseId(null, null, null);

		assertThat(testCaseIdEntry, nullValue());
	}

	@Test
	public void test_case_id_should_use_bypassed_code_ref_instead_of_method() throws NoSuchMethodException {
		Method method = TestCaseIdUtilsTest.class.getDeclaredMethod("noTestCaseIdAnnotationParameterized", Integer.TYPE, String.class);
		TestCaseId annotation = method.getAnnotation(TestCaseId.class);
		String codeRef = "my.custom.code.ref";
		String expectedTestCaseId = "my.custom.code.ref[5,firstParam]";
		TestCaseIdEntry testCaseIdEntry = TestCaseIdUtils.getTestCaseId(annotation, method, codeRef, Arrays.asList(5, "firstParam"));

		assertThat(testCaseIdEntry, notNullValue());
		assertThat(testCaseIdEntry.getId(), equalTo(expectedTestCaseId));
	}

	private static Stream<Object[]> formatData() {
		return Stream.of(
				new Object[] { PARAMS, "Test Case ID {method}", "Test Case ID verifyTestCaseId" },
				new Object[] { null, "Test Case ID {this.value}", "Test Case ID stepValue" },
				new Object[] { PARAMS, "Test Case ID {this.object.value}", "Test Case ID pojoValue" },
				new Object[] { PARAMS, "Test Case ID {0}", "Test Case ID one" },
				new Object[] { PARAMS, "Test Case ID {1}", "Test Case ID two" },
				new Object[] { null, "Test Case ID {1}", "Test Case ID {1}" },
				new Object[] { null, "Test Case ID {class}", "Test Case ID TestCaseIdUtilsTest" },
				new Object[] { null, "Test Case ID {classRef}", "Test Case ID com.epam.reportportal.utils.TestCaseIdUtilsTest" }
		);
	}

	private static class PojoObject {
		@SuppressWarnings("unused")
		private final String value = "pojoValue";
	}

	@TestCaseId(value = "Test Case ID")
	public void verifyTestCaseId() {

	}

	private static final List<String> PARAMS = Arrays.asList("one", "two");
	@SuppressWarnings("unused")
	private final String value = "stepValue";
	@SuppressWarnings("unused")
	private final PojoObject object = new PojoObject();

	@ParameterizedTest
	@MethodSource("formatData")
	public void test_case_id_format_defaults(List<Object> params, String id, String expectedResult) throws NoSuchMethodException {
		Method method = this.getClass().getDeclaredMethod("verifyTestCaseId");
		TestCaseId realId = method.getAnnotation(TestCaseId.class);
		TestCaseId testCaseId = mock(
				TestCaseId.class, withSettings().defaultAnswer(invocation -> {
					Method invocationMethod = invocation.getMethod();
					if ("value".equals(invocationMethod.getName())) {
						return id;
					}
					return invocationMethod.invoke(realId, invocation.getArguments());
				})
		);

		TestCaseIdEntry result = TestCaseIdUtils.getTestCaseId(testCaseId, method, null, params, this);

		assertThat(result, notNullValue());
		assertThat(result.getId(), equalTo(expectedResult));
	}
}
