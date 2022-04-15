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
import com.epam.reportportal.annotations.TemplateConfig;
import com.epam.reportportal.utils.templating.TemplateConfiguration;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class StepNameUtilsTest {

	private static final String STEP_NAME_PATTERN = "A test step value {0}";

	@Mock
	private MethodSignature methodSignature;

	@Mock(lenient = true)
	private JoinPoint joinPoint;

	@Step(templateConfig = @StepTemplateConfig)
	private void templateConfigMethod() {

	}

	/**
	 * @see <a href="https://github.com/reportportal/client-java/issues/73">Covers NPE issue fix</a>
	 */
	@Test
	public void createParamsMapping() throws NoSuchMethodException {

		String[] namesArray = { "firstName", "secondName", "thirdName" };
		when(methodSignature.getMethod()).thenReturn(this.getClass().getDeclaredMethod("templateConfigMethod"));
		when(methodSignature.getParameterNames()).thenReturn(namesArray);
		when(joinPoint.getArgs()).thenReturn(new String[] { "first", "second", null });

		StepTemplateConfig templateConfig = this.getClass()
				.getDeclaredMethod("templateConfigMethod")
				.getDeclaredAnnotation(Step.class)
				.templateConfig();

		Map<String, Object> paramsMapping = StepNameUtils.createParamsMapping(new TemplateConfiguration(templateConfig),
				methodSignature,
				joinPoint
		);

		//3 for name key + 3 for index key + method name key
		assertThat(paramsMapping.size(), equalTo(namesArray.length * 2 + 1));
		Arrays.stream(namesArray).forEach(name -> assertThat(paramsMapping, hasKey(name)));

		assertThat(paramsMapping, hasEntry("firstName", "first"));
		assertThat(paramsMapping, hasEntry("secondName", "second"));
		assertThat(paramsMapping, hasEntry("thirdName", null));
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

	private static Stream<String[]> formatData() {
		return Stream.of(new String[] { "Method name: {method}", "Method name: stepMethod" },
				new String[] { "Inner value: {this.value}", "Inner value: stepValue" },
				new String[] { "Inner value: {this.object.value}", "Inner value: pojoValue" }
		);
	}

	private static class PojoObject {
		@SuppressWarnings("unused")
		private final String value = "pojoValue";
	}

	@Step("Step name")
	public void stepMethod() {

	}

	@SuppressWarnings("unused")
	private final String value = "stepValue";
	@SuppressWarnings("unused")
	private final PojoObject object = new PojoObject();

	@ParameterizedTest
	@MethodSource("formatData")
	public void test_step_format_defaults(String stepName, String expectedResult) throws NoSuchMethodException {
		Method method = this.getClass().getDeclaredMethod("stepMethod");
		Step realStep = method.getAnnotation(Step.class);
		Step step = mock(Step.class, withSettings().defaultAnswer(invocation -> {
			Method invocationMethod = invocation.getMethod();
			if ("value".equals(invocationMethod.getName())) {
				return stepName;
			}
			return invocationMethod.invoke(realStep, invocation.getArguments());
		}));

		when(methodSignature.getMethod()).thenReturn(method);
		when(methodSignature.getParameterNames()).thenReturn(new String[0]);
		when(joinPoint.getThis()).thenReturn(this);
		when(joinPoint.getArgs()).thenReturn(new String[0]);

		String result = StepNameUtils.getStepName(step, methodSignature, joinPoint);

		assertThat(result, equalTo(expectedResult));
	}

	@Step("{method} {this} {0}")
	public void verifyNulls() {

	}

	@Test
	public void test_step_format_null_method() throws NoSuchMethodException {
		when(methodSignature.getMethod()).thenReturn(null);
		when(methodSignature.getParameterNames()).thenReturn(null);
		when(joinPoint.getThis()).thenReturn(null);
		when(joinPoint.getArgs()).thenReturn(null);

		String result = StepNameUtils.getStepName(this.getClass().getDeclaredMethod("verifyNulls").getAnnotation(Step.class),
				methodSignature,
				joinPoint
		);

		assertThat(result, equalTo("{method} {this} {0}"));
	}

	@Step("{this.test}")
	public void verifyNullReference() {

	}

	@Test
	public void test_null_reference() throws NoSuchMethodException {
		when(joinPoint.getThis()).thenReturn(null);

		String result = StepNameUtils.getStepName(this.getClass().getDeclaredMethod("verifyNullReference").getAnnotation(Step.class),
				methodSignature,
				joinPoint
		);

		assertThat(result, equalTo("{this.test}"));
	}

	@Step(value = "A {mmmethod}", templateConfig = @StepTemplateConfig(methodNameTemplate = "mmmethod"))
	public void verifyStepConfigurationUsage() {}

	@Test
	public void test_step_template_config() throws NoSuchMethodException {
		when(methodSignature.getMethod()).thenReturn(this.getClass().getDeclaredMethod("verifyStepConfigurationUsage"));
		when(methodSignature.getParameterNames()).thenReturn(new String[0]);
		when(joinPoint.getArgs()).thenReturn(new String[0]);

		String result = StepNameUtils.getStepName(methodSignature.getMethod().getAnnotation(Step.class), methodSignature, joinPoint);
		assertThat(result, equalTo("A verifyStepConfigurationUsage"));
	}

	@Step(value = "A {mmmethod}", config = @TemplateConfig(methodNameTemplate = "mmmethod"))
	public void verifyConfigurationUsage() {}

	@Test
	public void test_template_config() throws NoSuchMethodException {
		when(methodSignature.getMethod()).thenReturn(this.getClass().getDeclaredMethod("verifyConfigurationUsage"));
		when(methodSignature.getParameterNames()).thenReturn(new String[0]);
		when(joinPoint.getArgs()).thenReturn(new String[0]);

		String result = StepNameUtils.getStepName(methodSignature.getMethod().getAnnotation(Step.class), methodSignature, joinPoint);
		assertThat(result, equalTo("A verifyConfigurationUsage"));
	}

	@Step(value = "A {mmmethod}", templateConfig = @StepTemplateConfig(methodNameTemplate = "mmethod"),
			config = @TemplateConfig(methodNameTemplate = "mmmethod"))
	public void verifyConfigurationOverride() {}

	@Test
	public void test_template_config_override() throws NoSuchMethodException {
		when(methodSignature.getMethod()).thenReturn(this.getClass().getDeclaredMethod("verifyConfigurationOverride"));
		when(methodSignature.getParameterNames()).thenReturn(new String[0]);
		when(joinPoint.getArgs()).thenReturn(new String[0]);

		String result = StepNameUtils.getStepName(methodSignature.getMethod().getAnnotation(Step.class), methodSignature, joinPoint);
		assertThat(result, equalTo("A verifyConfigurationOverride"));
	}
}
