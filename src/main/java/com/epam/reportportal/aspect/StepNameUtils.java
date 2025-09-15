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
import com.epam.reportportal.utils.formatting.templating.TemplateConfiguration;
import com.epam.reportportal.utils.formatting.templating.TemplateProcessing;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

import static java.util.Optional.ofNullable;

/**
 * Helper methods to generate step names.
 */
public class StepNameUtils {

	private StepNameUtils() {
		throw new IllegalStateException("Static only class");
	}

	/**
	 * Generate step name based on a template bypassed in {@link Step} annotation.
	 *
	 * @param step      annotation
	 * @param signature signature of the method annotated with {@link Step}
	 * @param joinPoint intercepted step join point
	 * @return step name
	 */
	@Nonnull
	public static String getStepName(@Nonnull Step step, @Nonnull MethodSignature signature, @Nonnull JoinPoint joinPoint) {
		String nameTemplate = step.value();
		if (nameTemplate.trim().isEmpty()) {
			return signature.getMethod().getName();
		}

		TemplateConfiguration config = new TemplateConfiguration(step.config());
		return getStepName(nameTemplate, config, signature, joinPoint);
	}

	/**
	 * Generate step name based on a template and configuration bypassed.
	 *
	 * @param nameTemplate template string
	 * @param config       template configuration to use
	 * @param signature    signature of the method related to the step
	 * @param joinPoint    intercepted step join point
	 * @return step name
	 */
	@Nonnull
	public static String getStepName(@Nonnull String nameTemplate, @Nonnull TemplateConfiguration config,
			@Nonnull MethodSignature signature, @Nonnull JoinPoint joinPoint) {
		Map<String, Object> parametersMap = createParamsMapping(signature, joinPoint);
		return TemplateProcessing.processTemplate(nameTemplate, joinPoint.getThis(), signature.getMethod(), parametersMap, config);
	}

	@Nonnull
	static Map<String, Object> createParamsMapping(@Nonnull MethodSignature signature, @Nonnull JoinPoint joinPoint) {
		Object[] args = joinPoint.getArgs();
		String[] parameterNames = signature.getParameterNames();
		int paramsCount = Math.min(ofNullable(parameterNames).map(p -> p.length).orElse(0), ofNullable(args).map(a -> a.length).orElse(0));

		Map<String, Object> paramsMapping = new HashMap<>();
		for (int i = 0; i < paramsCount; i++) {
			paramsMapping.put(parameterNames[i], args[i]);
			paramsMapping.put(Integer.toString(i), args[i]);
		}
		return paramsMapping;
	}
}
