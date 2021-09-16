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
import com.epam.reportportal.annotations.StepTemplateConfig;
import com.epam.reportportal.utils.StepTemplateUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Optional.ofNullable;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
class StepNameUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(StepNameUtils.class);

	private static final String STEP_GROUP = "\\{([\\w$]+(\\.[\\w$]+)*)}";

	private StepNameUtils() {
		//static only
	}

	@Nonnull
	static String getStepName(@Nonnull Step step, @Nonnull MethodSignature signature, @Nonnull JoinPoint joinPoint) {
		String nameTemplate = step.value();
		if (nameTemplate.trim().isEmpty()) {
			return signature.getMethod().getName();
		}
		Matcher matcher = Pattern.compile(STEP_GROUP).matcher(nameTemplate);
		Map<String, Object> parametersMap = createParamsMapping(step.templateConfig(), signature, joinPoint.getArgs());

		StringBuffer stringBuffer = new StringBuffer();
		while (matcher.find()) {
			String templatePart = matcher.group(1);
			String replacement = getReplacement(templatePart, parametersMap, step.templateConfig());
			matcher.appendReplacement(stringBuffer, Matcher.quoteReplacement(replacement != null ? replacement : matcher.group(0)));
		}
		matcher.appendTail(stringBuffer);
		return stringBuffer.toString();
	}

	@Nonnull
	static Map<String, Object> createParamsMapping(@Nonnull StepTemplateConfig templateConfig, @Nonnull MethodSignature signature,
			@Nullable final Object... args) {
		int paramsCount = Math.min(signature.getParameterNames().length, ofNullable(args).map(a -> a.length).orElse(0));
		Map<String, Object> paramsMapping = new HashMap<>();
		paramsMapping.put(templateConfig.methodNameTemplate(), signature.getMethod().getName());
		for (int i = 0; i < paramsCount; i++) {
			paramsMapping.put(signature.getParameterNames()[i], args[i]);
			paramsMapping.put(Integer.toString(i), args[i]);
		}
		return paramsMapping;
	}

	@Nullable
	private static String getReplacement(@Nonnull String templatePart, @Nonnull Map<String, Object> parametersMap,
			@Nonnull StepTemplateConfig templateConfig) {
		String[] fields = templatePart.split("\\.");
		String variableName = fields[0];
		if (!parametersMap.containsKey(variableName)) {
			LOGGER.error("Param - " + variableName + " was not found");
			return null;
		}
		Object param = parametersMap.get(variableName);
		try {
			return StepTemplateUtils.retrieveValue(templateConfig, 1, fields, param);
		} catch (NoSuchFieldException e) {
			LOGGER.error("Unable to parse: " + templatePart);
			return null;
		}
	}
}
