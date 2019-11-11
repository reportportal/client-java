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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link Step#value()} template configuration. Required for customizing representation of the parsed collections and arrays.
 * {@link StepTemplateConfig#methodNameTemplate()} required to set the invoked method name template to be included in the result value to
 * prevent situations when the method argument has the same name as a default {@link StepTemplateConfig#METHOD_NAME_TEMPLATE}
 *
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface StepTemplateConfig {

	String METHOD_NAME_TEMPLATE = "method";
	String ITERABLE_START_PATTERN = "[";
	String ITERABLE_END_PATTERN = "]";
	String ITERABLE_ELEMENT_DELIMITER = ", ";
	String ARRAY_START_PATTERN = "{";
	String ARRAY_END_PATTERN = "}";
	String ARRAY_ELEMENT_DELIMITER = ", ";

	String methodNameTemplate() default METHOD_NAME_TEMPLATE;

	String iterableStartSymbol() default ITERABLE_START_PATTERN;

	String iterableEndSymbol() default ITERABLE_END_PATTERN;

	String iterableElementDelimiter() default ITERABLE_ELEMENT_DELIMITER;

	String arrayStartSymbol() default ARRAY_START_PATTERN;

	String arrayEndSymbol() default ARRAY_END_PATTERN;

	String arrayElementDelimiter() default ARRAY_ELEMENT_DELIMITER;
}
