/*
 * Copyright 2020 EPAM Systems
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

package com.epam.reportportal.aspect;

import com.epam.reportportal.annotations.Step;
import com.epam.reportportal.annotations.StepTemplateConfig;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class StepNameUtilsTest {

	private static final String STEP_NAME_PATTERN = "A test step value {0}";

	@Mock
	private MethodSignature methodSignature;

	@Mock
	private JoinPoint joinPoint;

	/**
	 * @see <a href="https://github.com/reportportal/client-java/issues/73">Covers NPE issue fix</a>
	 */
	@Test
	public void createParamsMapping() throws NoSuchMethodException {

		String[] namesArray = { "firstName", "secondName", "thirdName" };
		when(methodSignature.getMethod()).thenReturn(this.getClass().getDeclaredMethod("templateConfigMethod"));
		when(methodSignature.getParameterNames()).thenReturn(namesArray);

		StepTemplateConfig templateConfig = this.getClass()
				.getDeclaredMethod("templateConfigMethod")
				.getDeclaredAnnotation(Step.class)
				.templateConfig();

		Map<String, Object> paramsMapping = StepNameUtils.createParamsMapping(templateConfig, methodSignature, "first", "second", null);

		//3 for name key + 3 for index key + method name key
		assertThat(paramsMapping.size(), equalTo(namesArray.length * 2 + 1));
		Arrays.stream(namesArray).forEach(name -> assertThat(paramsMapping, hasKey(name)));

		assertThat(paramsMapping, hasEntry("firstName", "first"));
		assertThat(paramsMapping, hasEntry("secondName", "second"));
		assertThat(paramsMapping, hasEntry("thirdName", null));
	}

	@Step(templateConfig = @StepTemplateConfig)
	private void templateConfigMethod() {

	}

	@Step(STEP_NAME_PATTERN)
	@SuppressWarnings("unused")
	private void stepWithAValueInName(String value) {

	}

	private static Stream<String> stepNameValues() {
		return Stream.of("aaaa", "/$^&^@#", null, "");
	}

	@ParameterizedTest
	@MethodSource("stepNameValues")
	public void test_special_characters_in_step_name(String name) throws NoSuchMethodException {
		when(methodSignature.getMethod()).thenReturn(this.getClass().getDeclaredMethod("stepWithAValueInName", String.class));
		when(methodSignature.getParameterNames()).thenReturn(new String[] { "value" });
		when(joinPoint.getArgs()).thenReturn(new String[] { name });

		String result = StepNameUtils.getStepName(methodSignature.getMethod().getAnnotation(Step.class), methodSignature, joinPoint);
		String expected = "A test step value " + (name == null ? "NULL" : name);
		assertThat(result, equalTo(expected));
	}

	@Step(STEP_NAME_PATTERN)
	private void stepWithAValueInNameNoParams() {

	}

	@Test
	public void test_no_format_in_case_a_step_with_no_params() throws NoSuchMethodException {
		when(methodSignature.getMethod()).thenReturn(this.getClass().getDeclaredMethod("stepWithAValueInNameNoParams"));
		when(methodSignature.getParameterNames()).thenReturn(new String[0]);
		when(joinPoint.getArgs()).thenReturn(new String[0]);

		String result = StepNameUtils.getStepName(methodSignature.getMethod().getAnnotation(Step.class), methodSignature, joinPoint);
		assertThat(result, equalTo(STEP_NAME_PATTERN));
	}
}
