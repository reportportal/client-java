/*
 * Copyright 2020 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.reportportal.utils;

import com.epam.reportportal.listeners.ItemStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class StatusEvaluationTest {
	public static Object[][] testData() {
		//@formatter:off
		return new Object[][]{
				{ItemStatus.FAILED, ItemStatus.SKIPPED, ItemStatus.FAILED},
				{ItemStatus.PASSED, ItemStatus.SKIPPED, ItemStatus.PASSED},
				{ItemStatus.PASSED, ItemStatus.FAILED, ItemStatus.FAILED},
				{ItemStatus.SKIPPED, ItemStatus.FAILED, ItemStatus.FAILED},
				{ItemStatus.PASSED, ItemStatus.INTERRUPTED, ItemStatus.INTERRUPTED},
				{ItemStatus.PASSED, ItemStatus.CANCELLED, ItemStatus.CANCELLED},
				{ItemStatus.FAILED, ItemStatus.INTERRUPTED, ItemStatus.FAILED},
				{ItemStatus.FAILED, ItemStatus.CANCELLED, ItemStatus.FAILED},
				{null, ItemStatus.SKIPPED, ItemStatus.SKIPPED},
				{ItemStatus.PASSED, null, ItemStatus.PASSED},
				{ItemStatus.SKIPPED, ItemStatus.PASSED, ItemStatus.PASSED},
				{ItemStatus.STOPPED, ItemStatus.PASSED, ItemStatus.PASSED},
				{ItemStatus.INFO, ItemStatus.PASSED, ItemStatus.PASSED},
				{ItemStatus.WARN, ItemStatus.PASSED, ItemStatus.PASSED},
				{ItemStatus.SKIPPED, ItemStatus.SKIPPED, ItemStatus.SKIPPED},
		};
		//@formatter:on
	}

	@ParameterizedTest
	@MethodSource("testData")
	public void test_status_calculation_for_some_cases(ItemStatus currentStatus, ItemStatus childStatus, ItemStatus expectedStatus) {
		assertThat(StatusEvaluation.evaluateStatus(currentStatus, childStatus), equalTo(expectedStatus));
	}
}
