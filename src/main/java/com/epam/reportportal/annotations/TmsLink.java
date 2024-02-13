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
 * Add a link to a Test Case located in external Test Management System.
 * This annotation appends a hypertext link to  a Test Case into the Test Item description.
 */
public @interface TmsLink {

	/**
	 * TMS ticket ID.
	 *
	 * @return ID as string
	 */
	String value();

	/**
	 * Link text pattern, accepts 'tms_id' parameter.
	 *
	 * @return formatted link text
	 */
	String linkTextPattern() default "TMS #{tms_id}";

	/**
	 * Link URL pattern, accepts 'tms_id' parameter.
	 *
	 * @return link to a TMS ticket
	 */
	String urlPattern() default "";
}
