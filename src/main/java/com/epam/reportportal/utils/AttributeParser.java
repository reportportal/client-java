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
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Executable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class contains functionality for parsing tags and attributes from string.
 */
public class AttributeParser {

	public static final String ATTRIBUTES_SPLITTER = ";";
	public static final String KEY_VALUE_SPLITTER = ":";

	private AttributeParser() {
		throw new IllegalStateException("Static only class");
	}

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
	@Nonnull
	public static Set<ItemAttributesRQ> parseAsSet(@Nullable String rawAttributes) {
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

	/**
	 * Parse a string representation of an attribute to ReportPortal attribute object instance.
	 * E.G.: 'key:value', '   :value', 'tag'
	 *
	 * @param attribute string representation of an attribute
	 * @return ReportPortal attribute object instance
	 */
	@Nullable
	public static ItemAttributesRQ splitKeyValue(@Nullable String attribute) {
		if (StringUtils.isBlank(attribute)) {
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

	/**
	 * Parse ReportPortal attributes from {@link Attributes} annotation instance.
	 *
	 * @param attributesAnnotation annotation instance
	 * @return a set of ReportPortal attributes
	 */
	@Nonnull
	public static Set<ItemAttributesRQ> retrieveAttributes(@Nullable Attributes attributesAnnotation) {
		Set<ItemAttributesRQ> itemAttributes = new LinkedHashSet<>();
		if (attributesAnnotation == null) {
			return itemAttributes;
		}
		itemAttributes.addAll(retrieveAttributes(attributesAnnotation.attributes()));
		itemAttributes.addAll(retrieveAttributes(attributesAnnotation.attributeValues()));
		itemAttributes.addAll(retrieveAttributes(attributesAnnotation.multiKeyAttributes()));
		itemAttributes.addAll(retrieveAttributes(attributesAnnotation.multiValueAttributes()));
		return itemAttributes;
	}

	/**
	 * Parse ReportPortal attributes from {@link Attribute} annotations.
	 *
	 * @param attributes annotation instances
	 * @return a set of ReportPortal attributes
	 */
	@Nonnull
	public static Set<ItemAttributesRQ> retrieveAttributes(@Nullable Attribute... attributes) {
		Set<ItemAttributesRQ> itemAttributes = new LinkedHashSet<>();
		if (attributes != null) {
			for (Attribute attribute : attributes) {
				if (StringUtils.isNotBlank(attribute.value())) {
					itemAttributes.add(createItemAttribute(attribute.key(), attribute.value()));
				}
			}
		}
		return itemAttributes;
	}

	/**
	 * Parse ReportPortal attributes from {@link AttributeValue} annotations.
	 *
	 * @param attributeValues annotation instances
	 * @return a set of ReportPortal attributes
	 */
	@Nonnull
	public static Set<ItemAttributesRQ> retrieveAttributes(@Nullable AttributeValue... attributeValues) {
		Set<ItemAttributesRQ> itemAttributes = new LinkedHashSet<>();
		if (attributeValues != null) {
			for (AttributeValue attributeValue : attributeValues) {
				if (StringUtils.isNotBlank(attributeValue.value())) {
					itemAttributes.add(createItemAttribute(null, attributeValue.value()));
				}
			}
		}
		return itemAttributes;
	}

	/**
	 * Parse ReportPortal attributes from {@link MultiKeyAttribute} annotations.
	 *
	 * @param multiKeyAttributes annotation instances
	 * @return a set of ReportPortal attributes
	 */
	@Nonnull
	public static Set<ItemAttributesRQ> retrieveAttributes(@Nullable MultiKeyAttribute... multiKeyAttributes) {
		Set<ItemAttributesRQ> itemAttributes = new LinkedHashSet<>();
		if (multiKeyAttributes != null) {
			for (MultiKeyAttribute attribute : multiKeyAttributes) {
				itemAttributes.addAll(createItemAttributes(attribute.keys(), attribute.value()));
			}
		}
		return itemAttributes;
	}

	/**
	 * Parse ReportPortal attributes from {@link MultiValueAttribute} annotations.
	 *
	 * @param multiValueAttributes annotation instances
	 * @return a set of ReportPortal attributes
	 */
	@Nonnull
	public static Set<ItemAttributesRQ> retrieveAttributes(@Nullable MultiValueAttribute... multiValueAttributes) {
		Set<ItemAttributesRQ> itemAttributes = new LinkedHashSet<>();
		if (multiValueAttributes != null) {
			for (MultiValueAttribute attribute : multiValueAttributes) {
				itemAttributes.addAll(createItemAttributes(attribute.isNullKey() ? null : attribute.key(), attribute.values()));
			}
		}
		return itemAttributes;
	}

	/**
	 * Create list of attributes from key array and a value.
	 *
	 * @param keys  attribute keys
	 * @param value attribute value
	 * @return list of ReportPortal attributes
	 */
	@Nonnull
	public static List<ItemAttributesRQ> createItemAttributes(@Nullable String[] keys, @Nullable String value) {
		if (StringUtils.isBlank(value)) {
			return Collections.emptyList();
		}
		if (keys == null || keys.length < 1) {
			return Collections.singletonList(createItemAttribute(null, value));
		}

		return Arrays.stream(keys).map(k -> createItemAttribute(k, value)).collect(Collectors.toList());
	}

	/**
	 * Create list of attributes from a key and value array.
	 *
	 * @param key    attribute key
	 * @param values attribute values
	 * @return list of ReportPortal attributes
	 */
	@Nonnull
	public static List<ItemAttributesRQ> createItemAttributes(@Nullable String key, @Nullable String[] values) {
		if (values != null && values.length > 0) {
			return Arrays.stream(values).filter(StringUtils::isNotBlank).map(v -> createItemAttribute(key, v)).collect(Collectors.toList());
		}
		return Collections.emptyList();
	}

	/**
	 * Create an ItemAttributesRQ instance with key and value.
	 *
	 * @param key   attribute key
	 * @param value attribute value
	 * @return ReportPortal attribute
	 */
	@Nonnull
	public static ItemAttributesRQ createItemAttribute(@Nullable String key, @Nonnull String value) {
		return new ItemAttributesRQ(key, value);
	}

	/**
	 * Scan for attributes annotations on the given executable and its declaration.
	 *
	 * @param executable the executable to scan
	 * @return a set of ReportPortal attributes or null if not found
	 */
	@Nonnull
	public static Set<ItemAttributesRQ> retrieveAttributes(@Nonnull Executable executable) {
		Set<ItemAttributesRQ> itemAttributes = new LinkedHashSet<>();
		itemAttributes.addAll(retrieveAttributes(executable.getAnnotation(Attributes.class)));
		itemAttributes.addAll(retrieveAttributes(executable.getAnnotationsByType(Attribute.class)));
		itemAttributes.addAll(retrieveAttributes(executable.getAnnotationsByType(AttributeValue.class)));
		itemAttributes.addAll(retrieveAttributes(executable.getAnnotationsByType(MultiKeyAttribute.class)));
		itemAttributes.addAll(retrieveAttributes(executable.getAnnotationsByType(MultiValueAttribute.class)));
		return itemAttributes;
	}
}
