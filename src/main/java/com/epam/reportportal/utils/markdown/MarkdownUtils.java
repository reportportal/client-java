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
package com.epam.reportportal.utils.markdown;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

/**
 * Set helpful of utility methods for reporting to ReportPortal
 *
 * @author Andrei Varabyeu
 * @deprecated Use {@link com.epam.reportportal.utils.formatting.MarkdownUtils} instead
 */
@Deprecated
public class MarkdownUtils {
	/**
	 * Adds special prefix to make log message being processed as markdown
	 *
	 * @param message Message
	 * @return Message with markdown marker
	 */
	public static String asMarkdown(String message) {
		return com.epam.reportportal.utils.formatting.MarkdownUtils.asMarkdown(message);
	}

	/**
	 * Builds markdown representation of some script to be logged to ReportPortal
	 *
	 * @param language Script language
	 * @param script   Script
	 * @return Message to be sent to ReportPortal
	 */
	public static String asCode(String language, String script) {
		return com.epam.reportportal.utils.formatting.MarkdownUtils.asCode(language, script);
	}

	/**
	 * Converts a table represented as List of Lists to a formatted table string.
	 *
	 * @param table        a table object
	 * @param maxTableSize maximum size in characters of result table, cells will be truncated
	 * @return string representation of the table
	 */
	@Nonnull
	public static String formatDataTable(@Nonnull final List<List<String>> table, int maxTableSize) {
		return com.epam.reportportal.utils.formatting.MarkdownUtils.formatDataTable(table, maxTableSize);
	}

	/**
	 * Converts a table represented as List of Lists to a formatted table string.
	 *
	 * @param table a table object
	 * @return string representation of the table
	 */
	@Nonnull
	public static String formatDataTable(@Nonnull final List<List<String>> table) {
		return com.epam.reportportal.utils.formatting.MarkdownUtils.formatDataTable(table);
	}

	/**
	 * Converts a table represented as Map to a formatted table string.
	 *
	 * @param table a table object
	 * @return string representation of the table
	 */
	@Nonnull
	public static String formatDataTable(@Nonnull final Map<String, String> table) {
		return com.epam.reportportal.utils.formatting.MarkdownUtils.formatDataTable(table);
	}

	public static String asTwoParts(@Nonnull String firstPart, @Nonnull String secondPart) {
		return com.epam.reportportal.utils.formatting.MarkdownUtils.asTwoParts(firstPart, secondPart);
	}
}
