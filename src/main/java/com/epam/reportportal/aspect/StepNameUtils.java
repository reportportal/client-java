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
import com.epam.reportportal.utils.templating.TemplateConfiguration;
import com.epam.reportportal.utils.templating.TemplateProcessing;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static java.util.Optional.ofNullable;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
class StepNameUtils {

	private StepNameUtils() {
		throw new IllegalStateException("Static only class");
	}

	@Nonnull
	static String getStepName(@Nonnull Step step, @Nonnull MethodSignature signature, @Nonnull JoinPoint joinPoint) {
		String nameTemplate = step.value();
		if (nameTemplate.trim().isEmpty()) {
			return signature.getMethod().getName();
		}

		TemplateConfiguration defaultConfig = new TemplateConfiguration();
		TemplateConfiguration deprecatedConfig = new TemplateConfiguration(step.templateConfig());
		TemplateConfiguration config = new TemplateConfiguration(step.config());
		if (!deprecatedConfig.equals(defaultConfig)) {
			if (config.equals(defaultConfig)) {
				config = deprecatedConfig;
			}
		}

		Map<String, Object> parametersMap = createParamsMapping(config, signature, joinPoint);
		return TemplateProcessing.processTemplate(nameTemplate, parametersMap, config);
	}

	@Nonnull
	static Map<String, Object> createParamsMapping(@Nonnull TemplateConfiguration templateConfig, @Nonnull MethodSignature signature,
			@Nonnull JoinPoint joinPoint) {
		Object[] args = joinPoint.getArgs();
		String[] parameterNames = signature.getParameterNames();
		int paramsCount = Math.min(ofNullable(parameterNames).map(p -> p.length).orElse(0), ofNullable(args).map(a -> a.length).orElse(0));
		Map<String, Object> paramsMapping = new HashMap<>();
		ofNullable(signature.getMethod()).map(Method::getName).ifPresent(name -> paramsMapping.put(templateConfig.getMethodName(), name));
		ofNullable(joinPoint.getThis()).ifPresent(current -> paramsMapping.put(templateConfig.getSelfName(), current));
		for (int i = 0; i < paramsCount; i++) {
			paramsMapping.put(parameterNames[i], args[i]);
			paramsMapping.put(Integer.toString(i), args[i]);
		}
		return paramsMapping;
	}
}
