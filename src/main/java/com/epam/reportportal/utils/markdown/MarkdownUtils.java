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
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Optional.ofNullable;

/**
 * Set helpful of utility methods for reporting to ReportPortal
 *
 * @author Andrei Varabyeu
 */
public class MarkdownUtils {

	public static final String MARKDOWN_MODE = "!!!MARKDOWN_MODE!!!";
	private static final char NEW_LINE = '\n';
	public static final String ONE_SPACE = "\u00A0";
	public static final String TABLE_INDENT = "\u00A0\u00A0\u00A0\u00A0";
	public static final String TABLE_COLUMN_SEPARATOR = "|";
	public static final String TABLE_ROW_SEPARATOR = "-";

	/**
	 * Adds special prefix to make log message being processed as markdown
	 *
	 * @param message Message
	 * @return Message with markdown marker
	 */
	public static String asMarkdown(String message) {
		return MARKDOWN_MODE.concat(message);
	}

	/**
	 * Builds markdown representation of some script to be logged to ReportPortal
	 *
	 * @param language Script language
	 * @param script   Script
	 * @return Message to be sent to ReportPortal
	 */
	public static String asCode(String language, String script) {
		return asMarkdown("```" + ofNullable(language).orElse("") + NEW_LINE +
				script + NEW_LINE +
				"```");
	}

	/**
	 * Converts a table represented as List of Lists to a formatted table string
	 *
	 * @param table a table object
	 * @return string representation of the table
	 */
	@Nonnull
	public static String formatDataTable(@Nonnull final List<List<String>> table) {
		StringBuilder result = new StringBuilder();
		int tableLength = table.stream().mapToInt(List::size).max().orElse(-1);
		List<Iterator<String>> iterList = table.stream().map(List::iterator).collect(Collectors.toList());
		List<Integer> colSizes = IntStream.range(0, tableLength)
				.mapToObj(n -> iterList.stream().filter(Iterator::hasNext).map(Iterator::next).collect(Collectors.toList()))
				.map(col -> col.stream().mapToInt(String::length).max().orElse(0))
				.collect(Collectors.toList());

		boolean header = true;
		for (List<String> row : table) {
			result.append(TABLE_INDENT).append(TABLE_COLUMN_SEPARATOR);
			for (int i = 0; i < row.size(); i++) {
				String cell = row.get(i);
				int maxSize = colSizes.get(i) - cell.length() + 2;
				int lSpace = maxSize / 2;
				int rSpace = maxSize - lSpace;
				IntStream.range(0, lSpace).forEach(j -> result.append(ONE_SPACE));
				result.append(cell);
				IntStream.range(0, rSpace).forEach(j -> result.append(ONE_SPACE));
				result.append(TABLE_COLUMN_SEPARATOR);
			}
			if (header) {
				header = false;
				result.append(NEW_LINE);
				result.append(TABLE_INDENT).append(TABLE_COLUMN_SEPARATOR);
				for (int i = 0; i < row.size(); i++) {
					int maxSize = colSizes.get(i) + 2;
					IntStream.range(0, maxSize).forEach(j -> result.append(TABLE_ROW_SEPARATOR));
					result.append(TABLE_COLUMN_SEPARATOR);
				}
			}
			result.append(NEW_LINE);
		}
		return result.toString().trim();
	}
}
