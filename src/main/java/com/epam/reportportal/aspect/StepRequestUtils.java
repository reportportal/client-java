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
import com.epam.reportportal.annotations.attribute.Attributes;
import com.epam.reportportal.utils.AttributeParser;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Calendar;
import java.util.Set;

import static java.util.Optional.ofNullable;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
class StepRequestUtils {

	private StepRequestUtils() {
		//static only
	}

	public static StartTestItemRQ buildStartStepRequest(@Nonnull String name, @Nullable String description,
			@Nonnull MethodSignature signature) {
		StartTestItemRQ request = new StartTestItemRQ();
		request.setAttributes(createStepAttributes(signature));
		ofNullable(description).filter(d -> !d.isEmpty()).ifPresent(request::setDescription);
		request.setName(name);
		request.setStartTime(Calendar.getInstance().getTime());
		request.setType("STEP");
		request.setHasStats(false);

		return request;
	}

	public static StartTestItemRQ buildStartStepRequest(MethodSignature signature, Step step, JoinPoint joinPoint) {
		String name = StepNameUtils.getStepName(step, signature, joinPoint);
		return buildStartStepRequest(name, step.description(), signature);
	}

	private static Set<ItemAttributesRQ> createStepAttributes(MethodSignature methodSignature) {
		Attributes attributesAnnotation = methodSignature.getMethod().getAnnotation(Attributes.class);
		if (attributesAnnotation != null) {
			return AttributeParser.retrieveAttributes(attributesAnnotation);
		}
		return null;
	}
}
