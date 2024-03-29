/*
 * Copyright 2024 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.reportportal.annotations;

/**
 * Filter Test Set by Parameter.
 */
public @interface TestParamFilter {
	/**
	 * Parameter index to which this filter should be applied. If it's not specified then the filter try to match every parameter.
	 *
	 * @return parameter index
	 */
	int paramIndex() default -1;

	/**
	 * Select a test parameter which name starts with specified String. Designed to use with {@link ParameterKey} annotation, since there is
	 * no parameter names in Java runtime.
	 *
	 * @return required prefix
	 */
	String nameStartsWith() default "";

	/**
	 * Select a test parameter which name ends with specified String. Designed to use with {@link ParameterKey} annotation, since there is
	 * no parameter names in Java runtime.
	 *
	 * @return required prefix
	 */
	String nameEndsWith() default "";

	/**
	 * Select a test parameter which name should contain specified String. Designed to use with {@link ParameterKey} annotation, since there
	 * is no parameter names in Java runtime.
	 *
	 * @return required prefix
	 */
	String nameContains() default "";

	/**
	 * Select a test parameter which value starts with specified String. Non-string parameter values convert with {@link Object#toString()},
	 * method.
	 *
	 * @return required prefix
	 */
	String valueStartsWith() default "";

	/**
	 * Select a test parameter which value ends with specified String. Non-string parameter values convert with {@link Object#toString()},
	 * method.
	 *
	 * @return required prefix
	 */
	String valueEndsWith() default "";

	/**
	 * Select a test parameter which value should contain specified String. Non-string parameter values convert with
	 * {@link Object#toString()}, method.
	 *
	 * @return required prefix
	 */
	String valueContains() default "";
}
