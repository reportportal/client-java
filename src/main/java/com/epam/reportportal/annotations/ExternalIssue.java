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
 * Link current Issue with an Issue posted in External Bug Tracking System. This annotation designed to use within {@link Issue} annotation
 * only and does not allow to add it to any other target.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface ExternalIssue {
	/**
	 * External System Issue ID
	 *
	 * @return Issue ID
	 */
	String value();

	/**
	 * Optional, use custom Bug Tracking System URL instead of one which is set in `reportportal.properties` file.
	 *
	 * @return Bug Tracking System URL
	 */
	String btsUrl() default "";

	/**
	 * Optional, use custom Bug Tracking System Project name instead of one which is set in `reportportal.properties` file.
	 *
	 * @return Bug Tracking System Project name
	 */
	String btsProject() default "";

	/**
	 * Optional, use custom Bug Tracking System Issue URL pattern instead of one which is set in `reportportal.properties` file. Use
	 * <code>{issue_id}</code> mark to put issue ID into the result URL. Use <code>{bts_project}</code> mark to put project name into the
	 * result.
	 *
	 * @return Bug Tracking System Issue URL pattern
	 */
	String urlPattern() default "";
}
