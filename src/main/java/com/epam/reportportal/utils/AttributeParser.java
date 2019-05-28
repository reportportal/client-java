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

import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import com.google.common.collect.Sets;

import java.util.Set;

/**
 * This class contains functionality for parsing tags from string.
 */
public class AttributeParser {

	public static final String ATTRIBUTES_SPLITTER = ";";
	public static final String KEY_VALUE_SPLITTER = ":";

	/**
	 * Parse attribute string.<br>
	 * Input attribute string should have format: build:4r3wf234;attributeKey:attributeValue;attributeValue2;attributeValue3.<br>
	 * Output map should have format:<br>
	 * build:4r3wf234<br>
	 * attributeKey:attributeValue<br>
	 * null:attributeValue2<br>
	 * null:attributeValue3<br>
	 *
	 * @param rawAttributes Attributes string
	 */
	public static Set<ItemAttributesRQ> parseAsSet(String rawAttributes) {
		if (null == rawAttributes) {
			return Sets.newHashSet();
		}
		Set<ItemAttributesRQ> attributes = Sets.newHashSet();

		String[] attributesSplitted = rawAttributes.trim().split(ATTRIBUTES_SPLITTER);
		for (int i = 0; i < attributesSplitted.length; i++) {
			ItemAttributesRQ itemAttributeResource = splitKeyValue(attributesSplitted[i]);
			if (itemAttributeResource != null) {
				attributes.add(itemAttributeResource);
			}
		}
		return attributes;
	}

	public static ItemAttributesRQ splitKeyValue(String attribute) {
		if (null == attribute || attribute.trim().isEmpty()) {
			return null;
		}
		String[] keyValue = attribute.split(KEY_VALUE_SPLITTER);
		if (keyValue.length == 1) {
			return new ItemAttributesRQ(null, keyValue[0].trim());
		} else if (keyValue.length == 2) {
			String key = keyValue[0].trim();
			if (key.isEmpty()) {
				key = null;
			}
			return new ItemAttributesRQ(key, keyValue[1].trim());
		}
		return null;
	}
}
