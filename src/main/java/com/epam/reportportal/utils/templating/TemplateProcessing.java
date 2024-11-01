/*
 *  Copyright 2022 EPAM Systems
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.epam.reportportal.utils.templating;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Executable;
import java.util.Map;

/**
 * Class for processing simple string templates.
 *
 * @deprecated use {@link com.epam.reportportal.utils.formatting.templating.TemplateProcessing} instead
 */
@Deprecated
public class TemplateProcessing {
	public static final String NULL_VALUE = "NULL";

	private TemplateProcessing() {
		throw new IllegalStateException("Static only class");
	}

	/**
	 * Format given pattern with given parameters and configuration.
	 *
	 * @param pattern    text patter to format
	 * @param object     current object context
	 * @param executable current execution context
	 * @param parameters a map which will be used to locate reference replacements in pattern
	 * @param config     templating mechanism configuration
	 * @return formatted string
	 */
	public static String processTemplate(@Nonnull String pattern, @Nullable Object object, @Nullable Executable executable,
			@Nullable Map<String, Object> parameters, @Nonnull TemplateConfiguration config) {
		return com.epam.reportportal.utils.formatting.templating.TemplateProcessing.processTemplate(pattern,
				object,
				executable,
				parameters,
				config.getDelegate()
		);
	}

	/**
	 * Format given pattern with given parameters and configuration.
	 *
	 * @param pattern    text patter to format
	 * @param parameters a map which will be used to locate reference replacements in pattern
	 * @param config     templating mechanism configuration
	 * @return formatted string
	 */
	public static String processTemplate(@Nonnull String pattern, @Nullable Map<String, Object> parameters,
			@Nonnull TemplateConfiguration config) {
		return processTemplate(pattern, null, null, parameters, config);
	}

	/**
	 * Example:
	 * fields = {"object", "names", "hash"} from 'template part' = {object.names.hash}
	 * templateConfig - default
	 * <p>
	 * Given:
	 * object - some object
	 * names - the {@link java.util.List} containing 3 {@link String} objects with hashes: {25,32,57}
	 * hash - field, which value should be retrieved
	 * <p>
	 * Result:
	 * "[25,32,57]"
	 *
	 * @param templateConfig {@link TemplateConfiguration} for result formatting
	 * @param index          Index of the current field, from the template part
	 * @param fields         Fields of the template part
	 * @param object         Value of the current field
	 * @return {@link String} representation of object field(s) value(s).
	 * @throws NoSuchFieldException if field not found
	 */
	public static String retrieveValue(TemplateConfiguration templateConfig, int index, String[] fields, Object object)
			throws NoSuchFieldException {
		return com.epam.reportportal.utils.formatting.templating.TemplateProcessing.retrieveValue(templateConfig.getDelegate(),
				index,
				fields,
				object
		);
	}
}
