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

import com.epam.ta.reportportal.ws.model.FinishExecutionRQ;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasLength;

class ObjectUtilsTest {

	@Test
	void test_clonePojo_keeps_short_description_untouched() {
		FinishExecutionRQ original = new FinishExecutionRQ();
		String description = "short description";
		original.setDescription(description);

		FinishExecutionRQ cloned = ObjectUtils.clonePojo(original, FinishExecutionRQ.class);

		assertThat(cloned.getDescription(), equalTo(description));
	}

	@Test
	void test_clonePojo_truncates_long_description_to_ten_megabytes_with_ellipsis() {
		FinishExecutionRQ original = new FinishExecutionRQ();

		int expectedLength = CommonConstants.TEN_MEGABYTES;
		// Create a string larger than limit, e.g. limit + 10 characters
		int extra = 10;
		StringBuilder sb = new StringBuilder(expectedLength + extra);
		for (int i = 0; i < expectedLength + extra; i++) {
			sb.append('a');
		}
		String longDescription = sb.toString();
		original.setDescription(longDescription);

		FinishExecutionRQ cloned = ObjectUtils.clonePojo(original, FinishExecutionRQ.class);

		String clonedDescription = cloned.getDescription();
		String replacement = CommonConstants.DEFAULT_TRUNCATE_REPLACEMENT;
		// ObjectUtils configures truncation to TEN_MEGABYTES including replacement
		assertThat(clonedDescription, hasLength(expectedLength));
		assertThat(clonedDescription.substring(expectedLength - replacement.length()), equalTo(replacement));
	}
}
