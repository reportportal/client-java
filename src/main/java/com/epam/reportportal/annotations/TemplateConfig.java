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

import com.epam.reportportal.utils.formatting.templating.TemplateConfiguration;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Template configuration. Required for customizing representation of the parsed collections and arrays.
 * {@link TemplateConfig#methodNameTemplate()} required to set the current method name template to be included in the result value to
 * prevent situations when the method argument has the same name as a default {@link TemplateConfiguration#METHOD_NAME_TEMPLATE}
 * <p>
 * {@link TemplateConfig#selfNameTemplate()} required to set the current object name template to be included in the result value to
 * prevent situations when the method argument has the same name as a default {@link TemplateConfiguration#SELF_NAME_TEMPLATE}
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface TemplateConfig {

	String classNameTemplate() default TemplateConfiguration.CLASS_SIMPLE_NAME_TEMPLATE;

	String classRefTemplate() default TemplateConfiguration.CLASS_FULL_NAME_TEMPLATE;

	String methodNameTemplate() default TemplateConfiguration.METHOD_NAME_TEMPLATE;

	String selfNameTemplate() default TemplateConfiguration.SELF_NAME_TEMPLATE;

	String fieldDelimiter() default TemplateConfiguration.FIELD_REFERENCE_DELIMITER;

	String iterableStartSymbol() default TemplateConfiguration.ITERABLE_START_PATTERN;

	String iterableEndSymbol() default TemplateConfiguration.ITERABLE_END_PATTERN;

	String iterableElementDelimiter() default TemplateConfiguration.ITERABLE_ELEMENT_DELIMITER;

	String arrayStartSymbol() default TemplateConfiguration.ARRAY_START_PATTERN;

	String arrayEndSymbol() default TemplateConfiguration.ARRAY_END_PATTERN;

	String arrayElementDelimiter() default TemplateConfiguration.ARRAY_ELEMENT_DELIMITER;
}
