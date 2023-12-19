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

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.epam.reportportal.utils.markdown.MarkdownUtils.asCode;
import static com.epam.reportportal.utils.markdown.MarkdownUtils.formatDataTable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Andrei Varabyeu
 */
public class MarkdownUtilsTest {

	public static final String ONE_ROW_EXPECTED_TABLE = "\u00A0\u00A0\u00A0\u00A0|\u00A0var_a\u00A0|\u00A0var_b\u00A0|\u00A0result\u00A0|\n"
			+ "\u00A0\u00A0\u00A0\u00A0|-------|-------|--------|\n"
			+ "\u00A0\u00A0\u00A0\u00A0|\u00A0\u00A0\u00A02\u00A0\u00A0\u00A0|\u00A0\u00A0\u00A02\u00A0\u00A0\u00A0|\u00A0\u00A0\u00A04\u00A0\u00A0\u00A0\u00A0|";

	public static final String TWO_ROWS_EXPECTED_TABLE = ONE_ROW_EXPECTED_TABLE + "\n"
		+ "\u00A0\u00A0\u00A0\u00A0|\u00A0\u00A0\u00A01\u00A0\u00A0\u00A0|\u00A0\u00A0\u00A02\u00A0\u00A0\u00A0|\u00A0\u00A0\u00A03\u00A0\u00A0\u00A0\u00A0|";

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
		List<List<String>> table =
				Arrays.asList(
						Arrays.asList("var_a", "var_b", "result"),
						Arrays.asList("2", "2", "4"),
						Arrays.asList("1", "2", "3")
				);

		assertThat(formatDataTable(table), equalTo(TWO_ROWS_EXPECTED_TABLE));
	}
}
