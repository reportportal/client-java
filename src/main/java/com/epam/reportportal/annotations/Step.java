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

import com.epam.ta.reportportal.ws.model.StartTestItemRQ;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation-marker for methods that are invoked during the test execution. Methods that are marked by this annotation
 * are represented in Report Portal as 'Nested Steps' with {@link StartTestItemRQ#isHasStats()} equal to 'false'.
 * Methods marked with this annotation can be nested in other methods and will be attached (reported as a child)
 * to the 'closest' wrapper (either test method or another method marked with this annotation)
 *
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Step {

	String value() default "";

	String description() default "";

	boolean isIgnored() default false;

	StepTemplateConfig templateConfig() default @StepTemplateConfig;
}
