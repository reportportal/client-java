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
import com.epam.reportportal.annotations.TestCaseIdTemplate;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class TestCaseIdUtilsTest {

	@TestCaseId(pattern = "id")
	public void testCaseAnnotationTest(String firstParam, @TestCaseIdTemplate(value = "id", isInteger = true) String id) {

	}

	@Test
	public void shouldEvaluateHashCode() throws NoSuchMethodException {
		Method method = TestCaseIdUtilsTest.class.getDeclaredMethod("testCaseAnnotationTest", String.class, String.class);
		String expectedTestCaseId = "5";
		Integer testCaseId = TestCaseIdUtils.getTestCaseId(method.getAnnotation(TestCaseId.class),
				method,
				"firstParam",
				expectedTestCaseId
		);

		Assert.assertNotNull(testCaseId);
		Assert.assertEquals(Integer.parseInt(expectedTestCaseId), (int) testCaseId);
	}

}