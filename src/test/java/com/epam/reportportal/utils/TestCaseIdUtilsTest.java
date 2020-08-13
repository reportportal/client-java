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

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
@SuppressWarnings("unused")
public class TestCaseIdUtilsTest {

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
		TestCaseIdEntry testCaseIdEntry = TestCaseIdUtils.getTestCaseId(annotation, method, Arrays.asList("firstParam", expectedTestCaseId));

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
		Method method = TestCaseIdUtilsTest.class.getDeclaredMethod("testCaseIdAnnotationParameterizedNoKeyParam", Integer.TYPE, String.class);
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
		Method method = TestCaseIdUtilsTest.class.getDeclaredMethod("testCaseIdAnnotationTwoParamTest", Integer.TYPE, String.class, Boolean.class);
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
}
