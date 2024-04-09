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

package com.epam.reportportal.utils.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ContentTypeTest {
	@Test
	public void test_known_types() {
		assertThat(ContentType.KNOWN_TYPES, allOf(hasItem("application/json"), hasItem("image/bmp"), hasItem("application/pdf")));
	}

	private static Iterable<Object[]> testIsKnownType() {
		return Arrays.asList(
				new Object[] { "application/json", true },
				new Object[] { "application/pdf", true },
				new Object[] { "image/bmp", true },
				new Object[] { "video/vnd.iptvforum.2dparityfec-2005", false },
				new Object[] { "", false },
				new Object[] { "    ", false },
				new Object[] { "; charset=utf-8", false },
				new Object[] { null, false }
		);
	}

	@ParameterizedTest
	@MethodSource("testIsKnownType")
	public void test_is_known_type(String mediaType, boolean expected) {
		assertThat(ContentType.isKnownType(mediaType), equalTo(expected));
	}

	private static Iterable<Object[]> testMediaTypeStripData() {
		return Arrays.asList(
				new Object[] { "application/json; charset=utf-8", "application/json" },
				new Object[] { "", null },
				new Object[] { "    ", null },
				new Object[] { "; charset=utf-8", null }
		);
	}

	@ParameterizedTest
	@MethodSource("testMediaTypeStripData")
	public void test_media_type_strip(String contentType, String expected) {
		assertThat(ContentType.stripMediaType(contentType), equalTo(expected));
	}

	private static Iterable<Object[]> testMediaTypeValid() {
		return Arrays.asList(
				new Object[] { "application/json", true },
				new Object[] { "video/vnd.iptvforum.2dparityfec-2005", true },
				new Object[] { "application/json; charset=utf-8", false },
				new Object[] { "application/json;", false },
				new Object[] { "pdf", false },
				new Object[] { "    ", false },
				new Object[] { "; charset=utf-8", false },
				new Object[] { "", false },
				new Object[] { null, false }
		);
	}

	@ParameterizedTest
	@MethodSource("testMediaTypeValid")
	public void test_media_type_valid(String mediaType, boolean expected) {
		assertThat(ContentType.isValidType(mediaType), equalTo(expected));
	}
}
