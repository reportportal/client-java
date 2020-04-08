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
package com.epam.reportportal.exception;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Andrei Varabyeu
 */
public class ReportPortalExceptionTest {

	@Test
	public void trimMessage() {
		String errMessage = "Incorrect truncate";
		String toTrim = "hello world";

		assertThat(errMessage, ReportPortalException.trimMessage(toTrim, 10), equalTo("hello worl"));
		assertThat(errMessage, ReportPortalException.trimMessage(toTrim, 15), equalTo("hello world"));
		assertThat(errMessage, ReportPortalException.trimMessage(toTrim, 11), equalTo("hello world"));
		assertThat(errMessage, ReportPortalException.trimMessage(toTrim, 5), equalTo("hello"));
		assertThat(errMessage, ReportPortalException.trimMessage(null, 5), equalTo(""));
	}
}