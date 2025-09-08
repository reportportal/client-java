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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BasicUtils {
	private BasicUtils() {
		throw new IllegalStateException("Static only class");
	}

	/**
	 * Truncates string to the specified limit. If string length exceeds the limit, it will be cut and the truncateReplacement will be
	 * appended.
	 *
	 * @param string              string to truncate
	 * @param limit               maximum allowed length
	 * @param truncateReplacement string to append if truncation happens, defaults to {@link CommonConstants#DEFAULT_TRUNCATE_REPLACEMENT}
	 *                            if null
	 * @return truncated string if original length exceeds the limit, original string otherwise
	 */
	@Nonnull
	public static String truncateString(@Nonnull String string, int limit, @Nullable String truncateReplacement) {
		int effectiveLimit = Math.max(0, limit);
		if (string.length() <= effectiveLimit) {
			return string;
		}
		if (effectiveLimit == 0) {
			return "";
		}
		String replacement = truncateReplacement == null ? CommonConstants.DEFAULT_TRUNCATE_REPLACEMENT : truncateReplacement;
		return string.length() > replacement.length() ?
				string.substring(0, effectiveLimit - replacement.length()) + replacement :
				string.substring(0, effectiveLimit);
	}
}
