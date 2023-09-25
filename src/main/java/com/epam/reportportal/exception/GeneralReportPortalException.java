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
package com.epam.reportportal.exception;

/**
 * Base ReportPortal. Used for unclassified errors
 *
 * @author Andrei Varabyeu
 */
public class GeneralReportPortalException extends RuntimeException {
	private static final long serialVersionUID = -3747137063782963453L;

	/**
	 * HTTP Status Code
	 */
	protected final int statusCode;
	/**
	 * HTTP Status Message
	 */
	protected final String statusMessage;

	public GeneralReportPortalException(int statusCode, String statusMessage, String errorContent) {
		super(errorContent);
		this.statusCode = statusCode;
		this.statusMessage = statusMessage;

	}

	public int getStatusCode() {
		return statusCode;
	}

	public String getStatusMessage() {
		return statusMessage;
	}

	@Override
	public String getMessage() {
		final StringBuilder builder = new StringBuilder();
		builder.append("ReportPortal returned error\n")
				.append("Status code: ")
				.append(statusCode)
				.append("\n")
				.append("Status message: ")
				.append(statusMessage)
				.append("\n");
		if (null != super.getMessage()) {
			builder.append("Error Message: ").append(super.getMessage()).append("\n");
		}
		return builder.toString();
	}
}
