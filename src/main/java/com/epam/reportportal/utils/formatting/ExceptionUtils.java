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

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/***
 * Utility class for exception formatting.
 */
public class ExceptionUtils {
	public static final String SKIP_TRACE_MARKER = "...";
	public static final String LINE_DELIMITER = "\n";

	/***
	 * Get stack trace of the throwable excluding the stack trace of the base throwable.
	 *
	 * @param throwable Throwable to get stack trace from
	 * @param baseThrowable Throwable to exclude stack trace from
	 * @param preserveCause Preserve cause in the stack trace
	 * @return Formatted stack trace
	 */
	public static String getStackTrace(Throwable throwable, Throwable baseThrowable, boolean preserveCause) {
		String[] mainFrames = org.apache.commons.lang3.exception.ExceptionUtils.getStackFrames(throwable);
		Set<String> baseFrames = Arrays.stream(org.apache.commons.lang3.exception.ExceptionUtils.getStackFrames(baseThrowable)).collect(
				Collectors.toSet());
		StringBuilder sb = new StringBuilder();
		if (mainFrames.length > 0) {
			sb.append(mainFrames[0]).append(LINE_DELIMITER);
			boolean skipping = false;
			for (int i = 1; i < mainFrames.length; i++) {
				String frame = mainFrames[i];
				if(baseFrames.contains(frame) && (!frame.startsWith("Caused by:") || !preserveCause)) {
					if (!skipping) {
						sb.append(SKIP_TRACE_MARKER);
						skipping = true;
					}
				} else {
					skipping = false;
					sb.append(frame).append(LINE_DELIMITER);
				}
			}
		}
		return sb.toString();
	}

	/**
	 * Get stack trace of the throwable excluding the stack trace of the base throwable.
	 *
	 * @param throwable Throwable to get stack trace from
	 * @param baseThrowable Throwable to exclude stack trace from
	 * @return Formatted stack trace
	 */
	public static String getStackTrace(Throwable throwable, Throwable baseThrowable) {
		return getStackTrace(throwable, baseThrowable, false);
	}
}
