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

package com.epam.reportportal.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to declare the uniqueness of the test method/class.
 * If value is not provided it will be generated on the Report Portal
 * back-end side as hash of the generated id.
 *
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface TestCaseId {

	/**
	 * @return provided value or empty {@link String} by default.
	 * Default value is provided to avoid unnecessary value setting in parameterized tests
	 */
	String value() default "";

	/**
	 * @return flag to define whether test is parameterized or not
	 */
	boolean parametrized() default false;

	String[] selectedParameters() default {};

	/**
	 * TestCaseId template configuration to customize keywords and special symbols.
	 *
	 * @return template configuration
	 */
	TemplateConfig config() default @TemplateConfig;
}
