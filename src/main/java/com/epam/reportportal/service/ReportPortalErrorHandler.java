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
package com.epam.reportportal.service;

import com.epam.reportportal.exception.GeneralReportPortalException;
import com.epam.reportportal.exception.ReportPortalException;
import com.epam.reportportal.restendpoint.http.DefaultErrorHandler;
import com.epam.reportportal.restendpoint.http.HttpMethod;
import com.epam.reportportal.restendpoint.http.exception.RestEndpointIOException;
import com.epam.reportportal.restendpoint.serializer.Serializer;
import com.epam.ta.reportportal.ws.model.ErrorRS;
import com.google.common.base.Charsets;
import com.google.common.io.ByteSource;

import java.io.IOException;
import java.net.URI;

/**
 * Report Portal Error Handler<br>
 * Converts error from Endpoint to ReportPortal-related errors
 *
 * @author Andrei Varabyeu
 */
public class ReportPortalErrorHandler extends DefaultErrorHandler {

	private Serializer serializer;

	public ReportPortalErrorHandler(Serializer serializer) {
		this.serializer = serializer;
	}

	@Override
	protected void handleError(URI requestUri, HttpMethod requestMethod, int statusCode, String statusMessage, ByteSource errorBody)
			throws RestEndpointIOException {
		try {
			//read the body
			final byte[] body = errorBody.read();

			//try to deserialize an error
			ErrorRS errorRS = deserializeError(body);
			if (null != errorRS) {

				//ok, it's known ReportPortal error
				throw new ReportPortalException(statusCode, statusMessage, errorRS);
			} else {
				//there is some unknown error since we cannot de-serialize it into default error object
				throw new GeneralReportPortalException(statusCode, statusMessage, new String(body, Charsets.UTF_8));
			}

		} catch (IOException e) {
			//cannot read the body. just throw the general error
			throw new GeneralReportPortalException(statusCode, statusMessage, "Cannot read the response");
		}

	}

	/**
	 * Try to deserialize an error body
	 *
	 * @param content content to be deserialized
	 * @return Serialized object or NULL if it's impossible
	 */
	private ErrorRS deserializeError(byte[] content) {
		try {
			if (null != content) {
				return serializer.deserialize(content, ErrorRS.class);
			} else {
				return null;
			}

		} catch (Exception e) {
			return null;
		}

	}
}
