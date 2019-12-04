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
package com.epam.reportportal.service;

import com.epam.reportportal.exception.GeneralReportPortalException;
import com.epam.reportportal.exception.InternalReportPortalClientException;
import com.epam.reportportal.exception.ReportPortalException;
import com.epam.reportportal.restendpoint.http.DefaultErrorHandler;
import com.epam.reportportal.restendpoint.http.Response;
import com.epam.reportportal.restendpoint.http.exception.RestEndpointIOException;
import com.epam.reportportal.restendpoint.serializer.Serializer;
import com.epam.ta.reportportal.ws.model.ErrorRS;
import com.google.common.io.ByteSource;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

/**
 * Report Portal Error Handler<br>
 * Converts error from Endpoint to ReportPortal-related errors
 *
 * @author Andrei Varabyeu
 */
public class ReportPortalErrorHandler extends DefaultErrorHandler {

	private static final Logger LOG = LoggerFactory.getLogger(ReportPortalErrorHandler.class);

	private final Serializer serializer;

	public ReportPortalErrorHandler(Serializer serializer) {
		this.serializer = serializer;
	}

	@Override
	public void handle(Response<ByteSource> rs) throws RestEndpointIOException {

		if (!hasError(rs)) {
			return;
		}

		handleError(rs);
	}

	@Override
	public boolean hasError(Response<ByteSource> rs) {

		return super.hasError(rs) || isNotJson(rs);
	}

	private void handleError(Response<ByteSource> rs) throws RestEndpointIOException {
		try {

			ByteSource errorBody = rs.getBody();
			int statusCode = rs.getStatus();
			String statusMessage = rs.getReason();

			//read the body
			final byte[] body = errorBody.read();

			//try to deserialize an error
			ErrorRS errorRS = deserializeError(body);
			if (null != errorRS) {

				//ok, it's known ReportPortal error
				throw new ReportPortalException(statusCode, statusMessage, errorRS);
			} else {

				if (isNotJson(rs)) {

					throw new InternalReportPortalClientException("Report portal is not functioning correctly. Response is not json");
				} else {

					//there is some unknown error since we cannot de-serialize it into default error object
					throw new GeneralReportPortalException(statusCode, statusMessage, new String(body, StandardCharsets.UTF_8));
				}
			}

		} catch (IOException e) {
			//cannot read the body. just throw the general error
			throw new GeneralReportPortalException(rs.getStatus(), rs.getReason(), "Cannot read the response");
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

	private boolean isNotJson(Response<ByteSource> rs) {

		boolean result = true;

		Collection<String> contentTypes = rs.getHeaders().get(HttpHeaders.CONTENT_TYPE);
		if (contentTypes.isEmpty() && rs.getHeaders().containsKey(HttpHeaders.CONTENT_TYPE.toLowerCase())) {
			contentTypes = rs.getHeaders().get(HttpHeaders.CONTENT_TYPE.toLowerCase());
		}

		if (null != contentTypes) {
			for (String contentType : contentTypes) {

				boolean isJson = contentType.contains(ContentType.APPLICATION_JSON.getMimeType());
				if (isJson) {
					result = false;
					break;
				}
			}
		}

		return result;
	}
}
