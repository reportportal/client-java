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
package com.epam.reportportal.utils;

import com.google.common.collect.Sets;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class contains functionality for parsing tags from string.
 */
public class TagsParser {

	public static final String BUILD_TAG = "build";
	public static final String BUILD_MARKER = "build:";

	/**
	 * Parse tag string.<br>
	 * Input tag string should have format: build:4r3wf234;tag1;tag2;tag3.<br>
	 * Output map should have format:<br>
	 * build:4r3wf234<br>
	 * tag1:tag1<br>
	 * tag2:tag2<br>
	 * tag3:tag3<br>
	 *
	 * @param rawTags
	 */
	public static Map<String, String> findAllTags(String rawTags) {
		if (rawTags == null) {
			return null;
		} else {
			return parseTags(rawTags);
		}
	}

	public static Set<String> parseAsSet(String rawTags) {
		if (null == rawTags) {
			return null;
		}
		return Sets.newHashSet(rawTags.trim().split(";"));
	}

	/**
	 * Parse tag string.
	 *
	 * @param rawTags
	 */
	private static Map<String, String> parseTags(String rawTags) {
		Map<String, String> result = new HashMap<String, String>();
		String[] splitRawTags = rawTags.trim().split(";");
		for (String tag : splitRawTags) {
			if (isBuildTag(tag)) {
				result.put(BUILD_TAG, tag.substring(BUILD_MARKER.length()));
			} else {
				if (!tag.trim().isEmpty()) {
					result.put(tag, tag);
				}
			}
		}
		return result;

	}

	private static boolean isBuildTag(String tag) {
		// equivalent to start ignore case
		return (BUILD_MARKER.length() <= tag.length()) && (BUILD_MARKER.equalsIgnoreCase(tag.substring(0, BUILD_MARKER.length())));
	}
}
