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
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class StepNameUtilsTest {

	private final MethodSignature methodSignature = mock(MethodSignature.class);

	@BeforeEach
	public void init() throws NoSuchMethodException {
		when(methodSignature.getMethod()).thenReturn(this.getClass().getDeclaredMethod("templateConfigMethod"));
	}

	/**
	 * @see <a href="https://github.com/reportportal/client-java/issues/73">Covers NPE issue fix</a>
	 */
	@Test
	public void createParamsMapping() throws NoSuchMethodException {

		String[] namesArray = { "firstName", "secondName", "thirdName" };
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
}