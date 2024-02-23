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

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Link current Issue with a specific Parameterized or Dynamic test by applying filters.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface TestFilter {

	/**
	 * Specify Test Name filters to select certain test for {@link Issue} applying, suitable for dynamic tests.
	 *
	 * @return Test Name filters
	 */
	TestNameFilter[] name() default {};


	/**
	 * Specify Test Parameter filters to select certain test for {@link Issue} applying, suitable for parameterized tests.
	 *
	 * @return Test Name filters
	 */
	TestParamFilter[] param() default {};
}
