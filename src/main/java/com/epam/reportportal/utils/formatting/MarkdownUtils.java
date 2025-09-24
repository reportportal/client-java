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
package com.epam.reportportal.utils.formatting;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Optional.ofNullable;

/**
 * Set helpful of utility methods for reporting to ReportPortal
 */
public class MarkdownUtils {

	public static final String MARKDOWN_MODE = "!!!MARKDOWN_MODE!!!";
	private static final String NEW_LINE = "\n";
	public static final String ONE_SPACE = "\u00A0";
	public static final String TABLE_INDENT = "\u00A0\u00A0\u00A0\u00A0";
	public static final String TABLE_COLUMN_SEPARATOR = "|";
	public static final String TABLE_ROW_SEPARATOR = "-";
	public static final String TRUNCATION_REPLACEMENT = "...";
	public static final int PADDING_SPACES_NUM = 2;
	public static final int MAX_TABLE_SIZE = 83;
	public static final int MIN_COL_SIZE = 3;
	public static final String LOGICAL_SEPARATOR = NEW_LINE + NEW_LINE + "---" + NEW_LINE + NEW_LINE;

	private MarkdownUtils() {
		throw new IllegalStateException("Static only class");
	}

	/**
	 * Adds special prefix to make log message being processed as markdown
	 *
	 * @param message Message
	 * @return Message with markdown marker
	 */
	@Nonnull
	public static String asMarkdown(@Nonnull String message) {
		return MARKDOWN_MODE.concat(message);
	}

	/**
	 * Builds markdown representation of some script to be logged to ReportPortal
	 *
	 * @param language Script language
	 * @param script   Script
	 * @return Message to be sent to ReportPortal
	 */
	@Nonnull
	public static String asCode(@Nullable String language, @Nullable String script) {
		return asMarkdown("```" + ofNullable(language).orElse("") + NEW_LINE + script + NEW_LINE + "```");
	}

	private static List<Integer> calculateColSizes(@Nonnull List<List<String>> table) {
		int tableColNum = table.stream().mapToInt(List::size).max().orElse(-1);
		List<Iterator<String>> iterList = table.stream().map(List::iterator).collect(Collectors.toList());
		return IntStream.range(0, tableColNum)
				.mapToObj(n -> iterList.stream().filter(Iterator::hasNext).map(Iterator::next).collect(Collectors.toList()))
				.map(col -> col.stream().mapToInt(String::length).max().orElse(0))
				.collect(Collectors.toList());
	}

	private static int calculateTableSize(@Nonnull List<Integer> colSizes) {
		int colTableSize = colSizes.stream().reduce(Integer::sum).orElse(-1);
		colTableSize += (PADDING_SPACES_NUM + TABLE_COLUMN_SEPARATOR.length()) * colSizes.size() - 1; // Inner columns grid
		colTableSize += 2; // Outer table grid
		return colTableSize;
	}

	private static <T> List<List<T>> transposeTable(@Nonnull List<List<T>> table) {
		int tableColNum = table.stream().mapToInt(List::size).max().orElse(-1);
		List<Iterator<T>> iterList = table.stream().map(List::iterator).collect(Collectors.toList());
		return IntStream.range(0, tableColNum)
				.mapToObj(n -> iterList.stream().filter(Iterator::hasNext).map(Iterator::next).collect(Collectors.toList()))
				.collect(Collectors.toList());
	}

	@Nonnull
	private static List<Integer> adjustColSizes(@Nonnull List<Integer> colSizes, int maxTableSize) {
		int colTableSize = calculateTableSize(colSizes);
		if (maxTableSize >= colTableSize) {
			return colSizes;
		}
		List<Pair<Integer, Integer>> colsBySize = IntStream.range(0, colSizes.size())
				.mapToObj(i -> Pair.of(colSizes.get(i), i))
				.sorted()
				.collect(Collectors.toList());
		Collections.reverse(colsBySize);
		int sizeToShrink = colTableSize - maxTableSize;
		for (int i = 0; i < sizeToShrink; i++) {
			for (int j = 0; j < colsBySize.size(); j++) {
				Pair<Integer, Integer> currentCol = colsBySize.get(j);
				if (currentCol.getKey() <= MIN_COL_SIZE) {
					continue;
				}
				Pair<Integer, Integer> nextCol = colsBySize.size() > j + 1 ? colsBySize.get(j + 1) : Pair.of(0, 0);
				if (currentCol.getKey() >= nextCol.getKey()) {
					colsBySize.set(j, Pair.of(currentCol.getKey() - 1, currentCol.getValue()));
					break;
				}
			}
		}
		return colsBySize.stream().sorted(Map.Entry.comparingByValue()).map(Pair::getKey).collect(Collectors.toList());
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
		List<Integer> colSizes = calculateColSizes(table);
		boolean transpose = colSizes.size() > table.size() && calculateTableSize(colSizes) > maxTableSize;
		List<List<String>> printTable = transpose ? transposeTable(table) : table;
		if (transpose) {
			colSizes = calculateColSizes(printTable);
		}
		colSizes = adjustColSizes(colSizes, maxTableSize);
		int tableSize = calculateTableSize(colSizes);
		boolean addPadding = tableSize <= maxTableSize;
		boolean header = !transpose;
		StringBuilder result = new StringBuilder();
		for (List<String> row : printTable) {
			result.append(TABLE_INDENT).append(TABLE_COLUMN_SEPARATOR);
			for (int i = 0; i < row.size(); i++) {
				String cell = row.get(i);
				int colSize = colSizes.get(i);
				if (colSize < cell.length()) {
					if (TRUNCATION_REPLACEMENT.length() < colSize) {
						cell = cell.substring(0, colSize - TRUNCATION_REPLACEMENT.length()) + TRUNCATION_REPLACEMENT;
					} else {
						cell = cell.substring(0, colSize);
					}
				}
				int padSize = colSize - cell.length() + (addPadding ? PADDING_SPACES_NUM : 0);
				int lSpace = padSize / 2;
				int rSpace = padSize - lSpace;
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
					int maxSize = colSizes.get(i) + (addPadding ? PADDING_SPACES_NUM : 0);
					IntStream.range(0, maxSize).forEach(j -> result.append(TABLE_ROW_SEPARATOR));
					result.append(TABLE_COLUMN_SEPARATOR);
				}
			}
			result.append(NEW_LINE);
		}
		return result.toString().trim();
	}

	/**
	 * Converts a table represented as List of Lists to a formatted table string.
	 *
	 * @param table a table object
	 * @return string representation of the table
	 */
	@Nonnull
	public static String formatDataTable(@Nonnull final List<List<String>> table) {
		return formatDataTable(table, MAX_TABLE_SIZE);
	}

	/**
	 * Converts a table represented as Map to a formatted table string.
	 *
	 * @param table a table object
	 * @return string representation of the table
	 */
	@Nonnull
	public static String formatDataTable(@Nonnull final Map<String, String> table) {
		List<List<String>> toFormat = new ArrayList<>();
		List<String> keys = new ArrayList<>(table.keySet());
		toFormat.add(keys);
		toFormat.add(keys.stream().map(table::get).collect(Collectors.toList()));
		return formatDataTable(toFormat);
	}

	@Nonnull
	public static String asTwoParts(@Nonnull String firstPart, @Nonnull String secondPart) {
		return firstPart + LOGICAL_SEPARATOR + secondPart;
	}
}
