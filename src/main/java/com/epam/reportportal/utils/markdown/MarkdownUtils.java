/*
 * Copyright (C) 2018 EPAM Systems
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

import static com.google.common.base.Strings.nullToEmpty;

/**
 * Set helpful of utility methods for reporting to ReportPortal
 *
 * @author Andrei Varabyeu
 */
public class MarkdownUtils {

	public static final String MARKDOWN_MODE = "!!!MARKDOWN_MODE!!!";
	private static final char NEW_LINE = '\n';

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
		//@formatter:off
		return new StringBuilder()
				.append(MARKDOWN_MODE)
				.append("```").append(nullToEmpty(language)).append(NEW_LINE)
				.append(script).append(NEW_LINE)
				.append("```")
				.toString();
		//@formatter:on
	}
}
