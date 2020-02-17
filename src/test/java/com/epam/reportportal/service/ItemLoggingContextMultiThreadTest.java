/*
 *  Copyright 2020 EPAM Systems
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.epam.reportportal.service;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ItemLoggingContextMultiThreadTest {
	@Before
	public void prepare() {

	}

	@After
	public void cleanUp() {

	}

	/**
	 * TestNG and other frameworks executes the very first startTestItem call from main thread (start root suite).
	 * Since all other threads are children of the main thread it leads to a situation when all threads share one LoggingContext.
	 * This test is here to ensure that will never happen again: https://github.com/reportportal/agent-java-testNG/issues/76
	 */
	@Test
	public void test_main_and_other_threads_have_different_logging_contexts() {

	}
}
