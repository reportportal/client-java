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

import com.epam.reportportal.annotations.attribute.Attribute;
import com.epam.reportportal.annotations.attribute.Attributes;
import com.epam.reportportal.annotations.attribute.MultiKeyAttribute;
import com.epam.reportportal.annotations.attribute.MultiValueAttribute;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.epam.reportportal.annotations.attribute.AttributeConstants.*;

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

	public static Set<ItemAttributesRQ> retrieveAttributes(Attributes attributesAnnotation) {
		Set<ItemAttributesRQ> itemAttributes = Sets.newLinkedHashSet();
		itemAttributes.addAll(createItemAttributes(COMPONENT_KEY, attributesAnnotation.component(), false, false));
		itemAttributes.addAll(createItemAttributes(E2E_KEY, attributesAnnotation.e2e(), false, false));
		itemAttributes.addAll(createItemAttributes(PERSONA_KEY, attributesAnnotation.persona(), false, false));
		itemAttributes.addAll(createItemAttributes(PRODUCT_KEY, attributesAnnotation.product(), false, false));
		itemAttributes.addAll(createItemAttributes(VERTICAL_KEY, attributesAnnotation.vertical(), false, false));
		for (Attribute attribute : attributesAnnotation.attributes()) {
			if (!attribute.value().trim().isEmpty()) {
				itemAttributes.add(createItemAttribute(attribute.key(), attribute.value(), attribute.isSystem(), attribute.isNullKey()));
			}
		}
		for (MultiKeyAttribute attribute : attributesAnnotation.multiKeyAttributes()) {
			itemAttributes.addAll(createItemAttributes(attribute.keys(), attribute.value(), attribute.isSystem(), attribute.isNullKey()));
		}
		for (MultiValueAttribute attribute : attributesAnnotation.multiValueAttributes()) {
			itemAttributes.addAll(createItemAttributes(attribute.key(), attribute.values(), attribute.isSystem(), attribute.isNullKey()));
		}

		return itemAttributes;
	}

	public static List<ItemAttributesRQ> createItemAttributes(String[] keys, String value, boolean isSystem, boolean isNullKey) {
		if (value == null || value.trim().isEmpty()) {
			return Collections.emptyList();
		}
		if (keys == null || keys.length < 1) {
			return Collections.singletonList(createItemAttribute(null, value, isSystem, isNullKey));
		}

		List<ItemAttributesRQ> itemAttributes = Lists.newArrayListWithExpectedSize(keys.length);
		for (String key : keys) {
			itemAttributes.add(createItemAttribute(key, value, isSystem, isNullKey));
		}
		return itemAttributes;
	}

	public static List<ItemAttributesRQ> createItemAttributes(String key, String[] values, boolean isSystem, boolean isNullKey) {
		if (values != null && values.length > 0) {
			List<ItemAttributesRQ> attributes = Lists.newArrayListWithExpectedSize(values.length);
			for (String value : values) {
				if (value != null && !value.trim().isEmpty()) {
					attributes.add(createItemAttribute(key, value, isSystem, isNullKey));
				}
			}

			return attributes;
		}

		return Collections.emptyList();
	}

	public static ItemAttributesRQ createItemAttribute(String key, String value, boolean isSystem, boolean isNullKey) {
		return new ItemAttributesRQ(isNullKey ? null : key, value, isSystem);
	}
}
