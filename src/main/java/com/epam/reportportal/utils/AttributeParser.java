/*
 * Copyright 2019 EPAM Systems
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

import com.epam.reportportal.annotations.attribute.*;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

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
	 * @return {@link Set} of {@link ItemAttributesRQ}
	 */
	public static Set<ItemAttributesRQ> parseAsSet(String rawAttributes) {
		if (null == rawAttributes) {
			return Collections.emptySet();
		}
		Set<ItemAttributesRQ> attributes = new HashSet<>();

		String[] attributesSplit = rawAttributes.trim().split(ATTRIBUTES_SPLITTER);
		for (String s : attributesSplit) {
			ItemAttributesRQ itemAttributeResource = splitKeyValue(s);
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

	public static Set<ItemAttributesRQ> retrieveAttributes(Attributes attributesAnnotation) {
		Set<ItemAttributesRQ> itemAttributes = new LinkedHashSet<>();
		for (Attribute attribute : attributesAnnotation.attributes()) {
			if (!attribute.value().trim().isEmpty()) {
				itemAttributes.add(createItemAttribute(attribute.key(), attribute.value()));
			}
		}
		for (AttributeValue attributeValue : attributesAnnotation.attributeValues()) {
			if (!attributeValue.value().trim().isEmpty()) {
				itemAttributes.add(createItemAttribute(null, attributeValue.value()));
			}
		}
		for (MultiKeyAttribute attribute : attributesAnnotation.multiKeyAttributes()) {
			itemAttributes.addAll(createItemAttributes(attribute.keys(), attribute.value()));
		}
		for (MultiValueAttribute attribute : attributesAnnotation.multiValueAttributes()) {
			itemAttributes.addAll(createItemAttributes(attribute.isNullKey() ? null : attribute.key(), attribute.values()));
		}

		return itemAttributes;
	}

	private static List<ItemAttributesRQ> createItemAttributes(String[] keys, String value) {
		if (value == null || value.trim().isEmpty()) {
			return Collections.emptyList();
		}
		if (keys == null || keys.length < 1) {
			return Collections.singletonList(createItemAttribute(null, value));
		}

		return Arrays.stream(keys).map(k -> createItemAttribute(k, value)).collect(Collectors.toList());
	}

	private static List<ItemAttributesRQ> createItemAttributes(String key, String[] values) {
		if (values != null && values.length > 0) {
			return Arrays.stream(values)
					.filter(StringUtils::isNotBlank)
					.map(v -> createItemAttribute(key, v))
					.collect(Collectors.toList());
		}

		return Collections.emptyList();
	}

	private static ItemAttributesRQ createItemAttribute(String key, String value) {
		return new ItemAttributesRQ(key, value);
	}
}
