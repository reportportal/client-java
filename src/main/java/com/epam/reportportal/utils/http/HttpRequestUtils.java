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
import com.epam.ta.reportportal.ws.model.Constants;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteSource;
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
		//static only
	}

	public static List<MultipartBody.Part> buildLogMultiPartRequest(List<SaveLogRQ> rqs) {
		List<MultipartBody.Part> result = new ArrayList<>();
		try {
			result.add(MultipartBody.Part.createFormData(
					Constants.LOG_REQUEST_JSON_PART,
					null,
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
						RequestBody.create(type, file.getContent())
				));
			}
		}
		return result;
	}
}
