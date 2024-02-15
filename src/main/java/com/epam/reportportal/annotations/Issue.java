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

import java.lang.annotation.*;

/**
 * This annotation supposed to automatically link failed test items a specific Issue on ReportPortal.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface Issue {
	/**
	 * Type (Locator), Short Name (Abbreviation) or Long Name (Defect name) (specified by priority) of an Issue on ReportPortal for
	 * the current project. If there is no such issue found in Project Setting the value will be used "as is" and sent as
	 * Issue Type (Locator).
	 *
	 * @return Type (Locator), Short Name (Abbreviation) or Long Name (Defect name) of an Issue
	 */
	String value();

	/**
	 * Arbitrary text describing the issue.
	 *
	 * @return issue description
	 */
	String comment() default "";

	/**
	 * Links to External System where this issue is located.
	 *
	 * @return External Issue describing object
	 */
	ExternalIssue[] external() default {};
}
