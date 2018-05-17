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
package com.epam.reportportal.exception;

import com.epam.ta.reportportal.ws.model.ErrorRS;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

/**
 * Base ReportPortal. Used for unclassified errors
 *
 * @author Andrei Varabyeu
 */
public class ReportPortalException extends GeneralReportPortalException {
	private static final int MAX_ERROR_MESSAGE_LENGTH = 100000;
	private static final long serialVersionUID = -3747137063782963453L;

	/**
	 * HTTP Error Response Body
	 */
	protected ErrorRS error;

	public ReportPortalException(int statusCode, String statusMessage, ErrorRS error) {
		super(statusCode, statusMessage, error.getMessage());
		this.error = error;
	}

	public ErrorRS getError() {
		return error;
	}

	@Override
	public String getMessage() {
		final StringBuilder builder = new StringBuilder();
		builder.append("Report Portal returned error\n")
				.append("Status code: ")
				.append(statusCode)
				.append("\n")
				.append("Status message: ")
				.append(statusMessage)
				.append("\n");
		if (null != error) {
			builder.append("Error Message: ")
					.append(trimMessage(error.getMessage(), MAX_ERROR_MESSAGE_LENGTH))
					.append("\n")
					.append("Error Type: ")
					.append(error.getErrorType())
					.append("\n");
		}
		return builder.toString();
	}

	@VisibleForTesting
	static String trimMessage(String message, int maxLength) {
		if (Strings.isNullOrEmpty(message)) {
			return "";
		}

		if (message.length() > maxLength) {
			return message.substring(0, maxLength);
		}
		return message;
	}
}
