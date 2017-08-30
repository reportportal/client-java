/*
 * Copyright 2016 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/client-core
 *
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.epam.reportportal.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;

/**
 * This class contains functionality for parsing tags from string.
 * 
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
