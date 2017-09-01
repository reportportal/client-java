/*
 * Copyright 2016 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/client-core
 *
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.epam.reportportal.utils;

import static com.google.common.base.Strings.nullToEmpty;

/**
 * Set helpful of utility methods for reporting to ReportPortal
 *
 * @author Andrei Varabyeu
 */
public class ReportPortalUtils {

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
	public static String toMarkdownCode(String language, String script) {
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
