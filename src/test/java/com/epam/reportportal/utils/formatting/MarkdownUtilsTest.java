/*
 * Copyright 2024 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.reportportal.utils.formatting;

import com.epam.reportportal.utils.formatting.MarkdownUtils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.epam.reportportal.utils.formatting.MarkdownUtils.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Andrei Varabyeu
 */
public class MarkdownUtilsTest {
	//@formatter:off
	public static final String ONE_ROW_EXPECTED_TABLE =
			TABLE_INDENT + "|\u00A0var_a\u00A0|\u00A0var_b\u00A0|\u00A0result\u00A0|\n"
					+ TABLE_INDENT + "|-------|-------|--------|\n"
					+ TABLE_INDENT + "|\u00A0\u00A0\u00A02\u00A0\u00A0\u00A0|\u00A0\u00A0\u00A02\u00A0\u00A0\u00A0|\u00A0\u00A0\u00A04\u00A0\u00A0\u00A0\u00A0|";

	public static final String TWO_ROWS_EXPECTED_TABLE =
			ONE_ROW_EXPECTED_TABLE
					+ "\n" + TABLE_INDENT + "|\u00A0\u00A0\u00A01\u00A0\u00A0\u00A0|\u00A0\u00A0\u00A02\u00A0\u00A0\u00A0|\u00A0\u00A0\u00A03\u00A0\u00A0\u00A0\u00A0|";
	//@formatter:on

	@Test
	public void asMarkdown() {
		assertThat("Incorrect markdown prefix", MarkdownUtils.asMarkdown("hello"), equalTo("!!!MARKDOWN_MODE!!!hello"));
	}

	@Test
	public void toMarkdownScript() {
		assertThat("Incorrect markdown prefix", asCode("groovy", "hello"), equalTo("!!!MARKDOWN_MODE!!!```groovy\nhello\n```"));
	}

	@Test
	public void test_format_data_table() {
		List<List<String>> table = Arrays.asList(Arrays.asList("var_a", "var_b", "result"),
				Arrays.asList("2", "2", "4"),
				Arrays.asList("1", "2", "3")
		);

		assertThat(formatDataTable(table), equalTo(TWO_ROWS_EXPECTED_TABLE));
	}

	public static final String BIG_COLUMN_VALUE = "4CZtyV3qsjVX08vBKu5YpvY2ckoFxLUombHEj1yf4uaBmrSGTzcXvlfba52HtUGLm56a8Vx4fBa0onEjlXY";
	public static final String TRUNCATED_COLUMN_VALUE = "4CZtyV3qsjVX08vBKu5YpvY2ckoFxLUombHEj1yf4uaBmrSGTzcXvlfba52...";

	//@formatter:off
	public static final String ONE_ROW_LONG_EXPECTED_TABLE =
			TABLE_INDENT + "|\u00A0var_a\u00A0|\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0var_b\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0|\u00A0result\u00A0|\n"
					+ TABLE_INDENT + "|-------|----------------------------------------------------------------|--------|\n"
					+ TABLE_INDENT + "|\u00A0\u00A0\u00A02\u00A0\u00A0\u00A0|\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A02\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0|\u00A0\u00A0\u00A04\u00A0\u00A0\u00A0\u00A0|";

	public static final String TWO_ROWS_LONG_EXPECTED_TABLE =
			ONE_ROW_LONG_EXPECTED_TABLE + "\n"
					+ TABLE_INDENT + "|\u00A0\u00A0\u00A01\u00A0\u00A0\u00A0|\u00A0" + TRUNCATED_COLUMN_VALUE + "\u00A0|\u00A0\u00A0\u00A03\u00A0\u00A0\u00A0\u00A0|";
	//@formatter:on

	@Test
	public void test_format_data_table_one_big_col() {
		List<List<String>> table = Arrays.asList(Arrays.asList("var_a", "var_b", "result"),
				Arrays.asList("2", "2", "4"),
				Arrays.asList("1", BIG_COLUMN_VALUE, "3")
		);
		assertThat(formatDataTable(table), equalTo(TWO_ROWS_LONG_EXPECTED_TABLE));
	}

	public static final String SECOND_TRUNCATED_COLUMN_VALUE = "4CZtyV3qsjVX08vBKu5YpvY2ckoFxLU...";

	//@formatter:off
	public static final String ONE_ROW_LONG_EXPECTED_TABLE_TWO =
			TABLE_INDENT + "|\u00A0var_a\u00A0|\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0var_b\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0|\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0result\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0|\n"
					+ TABLE_INDENT + "|-------|------------------------------------|------------------------------------|\n"
					+ TABLE_INDENT + "|\u00A0\u00A0\u00A02\u00A0\u00A0\u00A0|\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A02\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0|\u00A0" + SECOND_TRUNCATED_COLUMN_VALUE + "\u00A0|";

	public static final String TWO_ROWS_LONG_EXPECTED_TABLE_TWO =
			ONE_ROW_LONG_EXPECTED_TABLE_TWO + "\n"
					+ TABLE_INDENT + "|\u00A0\u00A0\u00A01\u00A0\u00A0\u00A0|\u00A0" + SECOND_TRUNCATED_COLUMN_VALUE + "\u00A0|\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A03\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0|";
	//@formatter:on

	@Test
	public void test_format_data_table_two_big_col() {
		List<List<String>> table = Arrays.asList(Arrays.asList("var_a", "var_b", "result"),
				Arrays.asList("2", "2", BIG_COLUMN_VALUE),
				Arrays.asList("1", BIG_COLUMN_VALUE, "3")
		);
		assertThat(formatDataTable(table), equalTo(TWO_ROWS_LONG_EXPECTED_TABLE_TWO));
	}

	@Test
	public void test_format_data_table_map() {
		Map<String, String> table = new LinkedHashMap<String, String>() {{
			put("var_a", "2");
			put("var_b", "2");
			put("result", "4");
		}};
		assertThat(formatDataTable(table), equalTo(ONE_ROW_EXPECTED_TABLE));
	}

	//@formatter:off
	public static final String MIN_ROW_WIDTH_EXPECTED_TABLE_TRANSPOSE =
			TABLE_INDENT + "|var|2|\n"
					+ TABLE_INDENT + "|var|2|\n"
					+ TABLE_INDENT + "|res|4|";
	//@formatter:on

	@Test
	public void test_format_data_table_min_size_transpose() {
		List<List<String>> table = Arrays.asList(Arrays.asList("var_a", "var_b", "result"), Arrays.asList("2", "2", "4"));
		assertThat(formatDataTable(table, 0), equalTo(MIN_ROW_WIDTH_EXPECTED_TABLE_TRANSPOSE));
	}

	//@formatter:off
	public static final String MIN_ROW_WIDTH_EXPECTED_TABLE_NO_TRANSPOSE =
			TABLE_INDENT + "|var|res|\n"
					+ TABLE_INDENT + "|---|---|\n"
					+ TABLE_INDENT + "|\u00A02\u00A0|\u00A04\u00A0|";
	//@formatter:on

	@Test
	public void test_format_data_table_min_size_no_transpose() {
		List<List<String>> table = Arrays.asList(Arrays.asList("var_a", "result"), Arrays.asList("2", "4"));
		assertThat(formatDataTable(table, 0), equalTo(MIN_ROW_WIDTH_EXPECTED_TABLE_NO_TRANSPOSE));
	}

	//@formatter:off
	public static final String MIN_ROW_WIDTH_EXPECTED_TABLE_TRANSPOSE_PAD =
			TABLE_INDENT + "|\u00A0var_a\u00A0\u00A0|\u00A02\u00A0|\n"
					+ TABLE_INDENT + "|\u00A0var_b\u00A0\u00A0|\u00A02\u00A0|\n"
					+ TABLE_INDENT + "|\u00A0result\u00A0|\u00A04\u00A0|";
	//@formatter:on

	@Test
	public void test_format_data_table_min_size_transpose_pad() {
		List<List<String>> table = Arrays.asList(Arrays.asList("var_a", "var_b", "result"), Arrays.asList("2", "2", "4"));
		assertThat(formatDataTable(table, 14), equalTo(MIN_ROW_WIDTH_EXPECTED_TABLE_TRANSPOSE_PAD));
	}

	public static final String TEXT_PART_ONE = "This is a text";
	public static final String TEXT_PART_TWO = "This is another text";
	public static final String EXPECTED_TWO_PARTS = TEXT_PART_ONE + "\n---\n" + TEXT_PART_TWO;

	@Test
	public void test_format_two_parts() {
		assertThat(asTwoParts(TEXT_PART_ONE, TEXT_PART_TWO), equalTo(EXPECTED_TWO_PARTS));
	}
}
