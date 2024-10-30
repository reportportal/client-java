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

package com.epam.reportportal.utils.http;

import com.epam.reportportal.exception.InternalReportPortalClientException;
import com.epam.reportportal.utils.MimeTypeDetector;
import com.epam.ta.reportportal.ws.reporting.Constants;
import com.epam.ta.reportportal.ws.reporting.SaveLogRQ;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.epam.reportportal.utils.files.ByteSource;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class HttpRequestUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequestUtils.class);
	public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
	public static final ObjectMapper MAPPER;

	static {
		MAPPER = new ObjectMapper();
		MAPPER.setDateFormat(new SimpleDateFormat(DEFAULT_DATE_FORMAT));
		MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	private static final String DEFAULT_TYPE = "application/octet-stream";

	private HttpRequestUtils() {
		throw new IllegalStateException("Static only class. No instances should exist for the class!");
	}

	@SuppressWarnings("deprecation")
	public static List<MultipartBody.Part> buildLogMultiPartRequest(List<SaveLogRQ> rqs) {
		List<MultipartBody.Part> result = new ArrayList<>();
		try {
			result.add(MultipartBody.Part.createFormData(Constants.LOG_REQUEST_JSON_PART,
					null,
					// Deprecated method call left here till the very end for backward compatibility
					RequestBody.create(okhttp3.MediaType.get("application/json; charset=utf-8"),
							MAPPER.writerFor(new TypeReference<List<SaveLogRQ>>() {
							}).writeValueAsString(rqs)
					)
			));
		} catch (JsonProcessingException e) {
			throw new InternalReportPortalClientException("Unable to process JSON", e);
		}

		for (SaveLogRQ rq : rqs) {
			final SaveLogRQ.File file = rq.getFile();
			if (null != file) {
				okhttp3.MediaType type;
				try {
					type = isBlank(file.getContentType()) ?
							okhttp3.MediaType.get(MimeTypeDetector.detect(ByteSource.wrap(file.getContent()), file.getName())) :
							okhttp3.MediaType.get(file.getContentType());
				} catch (IOException | IllegalArgumentException e) {
					LOGGER.error("Unable to parse content media type, default value was used: " + DEFAULT_TYPE, e);
					type = okhttp3.MediaType.get(DEFAULT_TYPE);
				}
				result.add(MultipartBody.Part.createFormData(Constants.LOG_REQUEST_BINARY_PART,
						file.getName(),
						// Deprecated method call left here till the very end for backward compatibility
						RequestBody.create(type, file.getContent())
				));
			}
		}
		return result;
	}

	public static final String TYPICAL_MULTIPART_BOUNDARY = "--972dbca3abacfd01fb4aea0571532b52";
	public static final String TYPICAL_JSON_PART_HEADER =
			TYPICAL_MULTIPART_BOUNDARY + "\r\nContent-Disposition: form-data; name=\"json_request_part\"\r\n"
					+ "Content-Type: application/json\r\n\r\n";
	public static final String TYPICAL_FILE_PART_HEADER =
			TYPICAL_MULTIPART_BOUNDARY + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"%s\"\r\n"
					+ "Content-Type: %s\r\n\r\n";
	public static final int TYPICAL_JSON_PART_HEADER_LENGTH = TYPICAL_JSON_PART_HEADER.length();
	public static final String TYPICAL_MULTIPART_FOOTER = "\r\n" + TYPICAL_MULTIPART_BOUNDARY + "--";
	public static final int TYPICAL_MULTIPART_FOOTER_LENGTH = TYPICAL_MULTIPART_FOOTER.length();
	public static final String TYPICAL_JSON_ARRAY = "[]";
	public static final int TYPICAL_JSON_ARRAY_LENGTH = TYPICAL_JSON_ARRAY.length();
	public static final String TYPICAL_JSON_ARRAY_ELEMENT = ",";
	public static final int TYPICAL_JSON_ARRAY_ELEMENT_LENGTH = TYPICAL_JSON_ARRAY_ELEMENT.length();

	private static long calculateJsonPartSize(SaveLogRQ request) {
		long size;
		try {
			size = MAPPER.writerFor(new TypeReference<SaveLogRQ>() {
			}).writeValueAsString(request).length();
		} catch (JsonProcessingException e) {
			throw new InternalReportPortalClientException("Unable to process JSON", e);
		}
		size += TYPICAL_JSON_PART_HEADER_LENGTH;
		size += TYPICAL_JSON_ARRAY_LENGTH;
		size += TYPICAL_JSON_ARRAY_ELEMENT_LENGTH;
		return size;
	}

	private static long calculateFilePartSize(SaveLogRQ request) {
		if (request.getFile() == null || request.getFile().getContent() == null) {
			return 0;
		}
		SaveLogRQ.File file = request.getFile();
		long size = String.format(TYPICAL_FILE_PART_HEADER, file.getName(), file.getContentType()).length();
		size += file.getContent().length;
		return size;
	}

	/**
	 * Estimate HTTP request size of a {@link SaveLogRQ}. Used to limit log batch size by payload.
	 *
	 * @param request log request
	 * @return estimate size of the request
	 */
	public static long calculateRequestSize(SaveLogRQ request) {
		return calculateJsonPartSize(request) + calculateFilePartSize(request);
	}
}
