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

import com.epam.reportportal.utils.reflect.Accessible;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for processing simple string templates.
 */
public class TemplateProcessing {
	private static final Logger LOGGER = LoggerFactory.getLogger(TemplateProcessing.class);

	private static final String NULL_VALUE = "NULL";

	private static final Pattern TEMPLATE_GROUP = Pattern.compile("\\{([\\w$]+(\\.[\\w$]+)*)}");

	private TemplateProcessing() {
		throw new IllegalStateException("Static only class");
	}

	/**
	 * Format given pattern with given parameters and configuration.
	 *
	 * @param pattern text patter to format
	 * @param parameters a map which will be used to locate reference replacements in pattern
	 * @param config templating mechanism configuration
	 * @return formatted string
	 */
	public static String processTemplate(String pattern, Map<String, Object> parameters, TemplateConfiguration config) {
		Matcher matcher = TEMPLATE_GROUP.matcher(pattern);
		StringBuffer stringBuffer = new StringBuffer();
		while (matcher.find()) {
			String templatePart = matcher.group(1);
			String replacement = getReplacement(templatePart, parameters, config);
			matcher.appendReplacement(stringBuffer, Matcher.quoteReplacement(replacement != null ? replacement : matcher.group(0)));
		}
		matcher.appendTail(stringBuffer);
		return stringBuffer.toString();
	}

	@Nullable
	private static String getReplacement(@Nonnull String templatePart, @Nonnull Map<String, Object> parametersMap,
			@Nonnull TemplateConfiguration templateConfig) {
		String[] fields = templatePart.split(Pattern.quote(templateConfig.getFieldDelimiter()));
		String variableName = fields[0];
		if (!parametersMap.containsKey(variableName)) {
			LOGGER.error("Param - " + variableName + " was not found");
			return null;
		}
		Object param = parametersMap.get(variableName);
		try {
			return retrieveValue(templateConfig, 1, fields, param);
		} catch (NoSuchFieldException e) {
			LOGGER.error("Unable to parse: " + templatePart);
			return null;
		}
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

		if (object == null) {
			return NULL_VALUE;
		}

		for (int i = index; i < fields.length; i++) {
			if (object.getClass().isArray()) {
				return parseArray(templateConfig, (Object[]) object, i, fields);
			}

			if (object instanceof Iterable) {
				return parseCollection(templateConfig, (Iterable<?>) object, i, fields);
			}

			object = Accessible.on(object).field(fields[i]).getValue();

		}

		return parseDescendant(templateConfig, object);
	}

	/**
	 * @param templateConfig {@link TemplateConfiguration}
	 * @param array          Array which elements should be parsed
	 * @param index          Index of the current field, from the template part
	 * @param fields         Fields of the template part
	 * @return {@link String} representation of the parsed Array
	 */
	private static String parseArray(TemplateConfiguration templateConfig, Object[] array, int index, String[] fields)
			throws NoSuchFieldException {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(templateConfig.getArrayStart());

		for (int i = 0; i < array.length; i++) {
			stringBuilder.append(retrieveValue(templateConfig, index, fields, array[i]));
			if (i < array.length - 1) {
				stringBuilder.append(templateConfig.getArrayDelimiter());
			}
		}

		stringBuilder.append(templateConfig.getArrayEnd());
		return stringBuilder.toString();
	}

	/**
	 * @param templateConfig {@link TemplateConfiguration}
	 * @param iterable       Collection which elements should be parsed
	 * @param index          Index of the current field, from the template part
	 * @param fields         Fields of the template part
	 * @return {@link String} representation of the parsed Collection
	 */
	private static String parseCollection(TemplateConfiguration templateConfig, Iterable<?> iterable, int index, String[] fields)
			throws NoSuchFieldException {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(templateConfig.getIterableStart());

		Iterator<?> iterator = iterable.iterator();
		while (iterator.hasNext()) {
			stringBuilder.append(retrieveValue(templateConfig, index, fields, iterator.next()));
			if (iterator.hasNext()) {
				stringBuilder.append(templateConfig.getIterableDelimiter());
			}
		}

		stringBuilder.append(templateConfig.getIterableEnd());
		return stringBuilder.toString();
	}

	/**
	 * @param templateConfig {@link TemplateConfiguration}
	 * @param descendant     The last element of the parsing chain
	 * @return {@link String} representation of the descendant
	 */
	private static String parseDescendant(TemplateConfiguration templateConfig, Object descendant) {
		if (descendant == null) {
			return NULL_VALUE;
		}
		if (descendant.getClass().isArray()) {
			return parseDescendantArray(templateConfig, descendant);
		}

		if (descendant instanceof Iterable) {
			return parseDescendantCollection(templateConfig, (Iterable<?>) descendant);
		}

		return String.valueOf(descendant);
	}

	/**
	 * @param templateConfig {@link TemplateConfiguration}
	 * @param array          Array of the descendant element which elements should be parsed
	 * @return {@link String} representation of the parsed Array
	 */
	private static String parseDescendantArray(TemplateConfiguration templateConfig, Object array) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(templateConfig.getArrayStart());

		int length = Array.getLength(array);
		for (int i = 0; i < length; i++) {
			stringBuilder.append(parseDescendant(templateConfig, Array.get(array, i)));
			if (i < length - 1) {
				stringBuilder.append(templateConfig.getArrayDelimiter());
			}
		}

		stringBuilder.append(templateConfig.getArrayEnd());
		return stringBuilder.toString();
	}

	/**
	 * @param templateConfig {@link TemplateConfiguration}
	 * @param iterable       Collection of the descendant element which elements should be parsed
	 * @return {@link String} representation of the parsed Collection
	 */
	private static String parseDescendantCollection(TemplateConfiguration templateConfig, Iterable<?> iterable) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(templateConfig.getIterableStart());

		Iterator<?> iterator = iterable.iterator();
		while (iterator.hasNext()) {
			stringBuilder.append(parseDescendant(templateConfig, iterator.next()));
			if (iterator.hasNext()) {
				stringBuilder.append(templateConfig.getIterableDelimiter());
			}
		}

		stringBuilder.append(templateConfig.getIterableEnd());
		return stringBuilder.toString();
	}
}
