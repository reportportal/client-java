/*
 * Copyright 2025 EPAM Systems
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

package com.epam.reportportal.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;

public class BasicUtilsTest {
	public static Object[][] testData() {
		//@formatter:off
        return new Object[][]{
                // 1. String is longer than limit, truncateReplacement is less than limit
                {"HelloWorld", 5, "..", "Hel.."},
                // 2. String is less than limit
                {"Hello", 10, "...", "Hello"},
                // 3. String is longer than limit, truncateReplacement is longer than limit
                {"HelloWorld", 3, "foobar", "Hel"},
                // 4. Limit is 0
                {"Hello", 0, "...", ""},
                // 5. Limit is less than 0
                {"Hello", -3, "...", ""},
				{"Hello", -3, "...", ""},
				// 6. String length equals limit
				{"Hello", 5, "...", "Hello"},
				// 7. truncateReplacement length equals limit
				{"HelloWorld", 3, "xyz", "Hel"},
				// 8. Empty input
				{"", 3, "...", ""}
        };
        //@formatter:on
	}

	@ParameterizedTest
	@MethodSource("testData")
	public void test_truncate_string_scenarios(String input, int limit, String replacement, String expected) {
		assertThat(BasicUtils.truncateString(input, limit, replacement), equalTo(expected));
	}

	@Test
	public void test_truncate_string_null_replacement_uses_default_and_respects_limit() {
		String input = "HelloWorld";
		int limit = 5;
		String result = BasicUtils.truncateString(input, limit, null);
		assertThat(result.length(), equalTo(limit));
		assertThat(result, endsWith(CommonConstants.DEFAULT_TRUNCATE_REPLACEMENT));
	}
}
