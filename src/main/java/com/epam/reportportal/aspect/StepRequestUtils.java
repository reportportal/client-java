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

package com.epam.reportportal.aspect;

import com.epam.reportportal.annotations.Step;
import com.epam.reportportal.annotations.UniqueID;
import com.epam.reportportal.annotations.attribute.Attribute;
import com.epam.reportportal.annotations.attribute.Attributes;
import com.epam.reportportal.annotations.attribute.MultiKeyAttribute;
import com.epam.reportportal.annotations.attribute.MultiValueAttribute;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.*;

import static com.epam.reportportal.annotations.attribute.AttributeConstants.*;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
class StepRequestUtils {

	private StepRequestUtils() {
		//static only
	}

	static StartTestItemRQ buildStartStepRequest(MethodSignature signature, Step step, JoinPoint joinPoint) {
		UniqueID uniqueIdAnnotation = signature.getMethod().getAnnotation(UniqueID.class);
		String uniqueId = uniqueIdAnnotation != null ? uniqueIdAnnotation.value() : null;
		String name = StepNameUtils.getStepName(step, signature, joinPoint);

		StartTestItemRQ request = new StartTestItemRQ();
		if (uniqueId != null && !uniqueId.trim().isEmpty()) {
			request.setUniqueId(uniqueId);
		}
		request.setAttributes(createStepAttributes(signature));
		if (!step.description().isEmpty()) {
			request.setDescription(step.description());
		}
		request.setName(name);
		request.setStartTime(Calendar.getInstance().getTime());
		request.setType("STEP");
		request.setHasStats(false);

		return request;
	}

	static FinishTestItemRQ buildFinishStepRequest(String status, Date endTime) {
		FinishTestItemRQ rq = new FinishTestItemRQ();
		rq.setEndTime(endTime);
		rq.setStatus(status);
		return rq;
	}

	private static Set<ItemAttributesRQ> createStepAttributes(MethodSignature methodSignature) {
		Attributes attributesAnnotation = methodSignature.getMethod().getAnnotation(Attributes.class);
		if (attributesAnnotation != null) {
			return retrieveAttributes(attributesAnnotation);
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
