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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Objects;

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
		if (effectiveLimit <= replacement.length()) {
			return string.substring(0, effectiveLimit);
		}
		return string.substring(0, effectiveLimit - replacement.length()) + replacement;
	}

	/**
	 * Compares two semantic versions following SemVer precedence rules.
	 * Returns a negative integer, zero, or a positive integer if the first
	 * argument is less than, equal to, or greater than the second respectively.
	 */
	public static int compareSemanticVersions(@Nonnull String compared, @Nonnull String basic) {
		String comparedNorm = normalizeVersion(compared);
		String basicNorm = normalizeVersion(basic);

		String[] basePreCompared = splitBaseAndPreRelease(comparedNorm);
		String[] basePreBasic = splitBaseAndPreRelease(basicNorm);

		int coreCompare = compareCoreVersions(basePreCompared[0], basePreBasic[0]);
		if (coreCompare != 0) {
			return coreCompare;
		}

		String pre1 = basePreCompared[1];
		String pre2 = basePreBasic[1];
		if (Objects.equals(pre1, pre2)) {
			return 0;
		}
		if (pre1 == null) {
			return 1;
		}
		if (pre2 == null) {
			return -1;
		}
		return comparePreRelease(pre1, pre2);
	}

	@Nonnull
	private static String normalizeVersion(@Nonnull String version) {
		String v = version.trim();
		if (v.startsWith("v") || v.startsWith("V")) {
			v = v.substring(1);
		}
		int plusIdx = v.indexOf('+');
		if (plusIdx >= 0) {
			v = v.substring(0, plusIdx);
		}
		return v;
	}

	@Nonnull
	private static String[] splitBaseAndPreRelease(@Nonnull String version) {
		int dashIdx = version.indexOf('-');
		if (dashIdx < 0) {
			return new String[] { version, null };
		}
		return new String[] { version.substring(0, dashIdx), version.substring(dashIdx + 1) };
	}

	private static int compareCoreVersions(@Nonnull String core1, @Nonnull String core2) {
		String[] p1 = core1.split("\\.");
		String[] p2 = core2.split("\\.");
		int len = Math.max(p1.length, p2.length);
		for (int i = 0; i < len; i++) {
			long n1 = i < p1.length ? parseLongSafe(p1[i]) : 0L;
			long n2 = i < p2.length ? parseLongSafe(p2[i]) : 0L;
			if (n1 != n2) {
				return n1 < n2 ? -1 : 1;
			}
		}
		return 0;
	}

	private static int comparePreRelease(@Nonnull String pre1, @Nonnull String pre2) {
		String[] t1 = pre1.split("\\.");
		String[] t2 = pre2.split("\\.");
		int len = Math.max(t1.length, t2.length);
		for (int i = 0; i < len; i++) {
			String a = i < t1.length ? t1[i] : null;
			String b = i < t2.length ? t2[i] : null;
			if (Objects.equals(a, b)) {
				continue;
			}
			if (a == null) {
				return -1;
			}
			if (b == null) {
				return 1;
			}
			boolean aNum = isNumeric(a);
			boolean bNum = isNumeric(b);
			if (aNum && bNum) {
				long na = parseLongSafe(a);
				long nb = parseLongSafe(b);
				if (na != nb) {
					return na < nb ? -1 : 1;
				}
			} else if (aNum != bNum) {
				return aNum ? -1 : 1;
			} else {
				int cmp = a.compareTo(b);
				if (cmp != 0) {
					return cmp < 0 ? -1 : 1;
				}
			}
		}
		return 0;
	}

	private static long parseLongSafe(@Nonnull String s) {
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static boolean isNumeric(@Nonnull String s) {
		if (s.isEmpty()) {
			return false;
		}
		for (int i = 0; i < s.length(); i++) {
			if (!Character.isDigit(s.charAt(i))) {
				return false;
			}
		}
		return true;
	}
}
