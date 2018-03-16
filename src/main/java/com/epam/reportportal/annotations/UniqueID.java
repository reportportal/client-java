/*
 * Copyright (C) 2018 EPAM Systems
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
 * Report Portal uses uniqueID for identifying test item's originality.
 * Report portal generates ID automatically by default.
 * Using the annotation you can provide custom ID for test item.
 * Note that custom ID can affect functionality based on
 * test item uniqueness.
 *
 * @author Pavel Bortnik
 * @since Report Portal Api v3.1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface UniqueID {

	/**
	 * Returns test item unique ID
	 *
	 * @return unique ID
	 */
	String value() default "";

}
