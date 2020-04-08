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
import com.epam.reportportal.restendpoint.http.HttpMethod;
import com.epam.reportportal.restendpoint.http.Response;
import com.epam.reportportal.restendpoint.serializer.json.JacksonSerializer;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.io.ByteSource;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Dzianis_Shybeka
 */
public class ReportPortalErrorHandlerTest {

	private ReportPortalErrorHandler reportPortalErrorHandler;

	@BeforeEach
	public void setUp() throws Exception {

		reportPortalErrorHandler = new ReportPortalErrorHandler(new JacksonSerializer());
	}

	@Test
	public void handle_not_json() throws Exception {
		//  given:
		LinkedListMultimap<String, String> invalidHeaders = LinkedListMultimap.create();
		invalidHeaders.put(HttpHeaders.CONTENT_TYPE, ContentType.TEXT_HTML.getMimeType());

		Response<ByteSource> invalidResponse = createFakeResponse(200, invalidHeaders);

		//  when:
		Assertions.assertThrows(InternalReportPortalClientException.class, () -> reportPortalErrorHandler.handle(invalidResponse));
	}

	@Test
	public void handle_not_json_lowercase() throws Exception {
		//  given:
		LinkedListMultimap<String, String> invalidHeaders = LinkedListMultimap.create();
		invalidHeaders.put(HttpHeaders.CONTENT_TYPE.toLowerCase(), ContentType.TEXT_HTML.getMimeType());

		Response<ByteSource> invalidResponse = createFakeResponse(200, invalidHeaders);

		//  when:
		Assertions.assertThrows(InternalReportPortalClientException.class, () -> reportPortalErrorHandler.handle(invalidResponse));
	}

	@Test
	public void handle_json() throws Exception {
		//  given:
		LinkedListMultimap<String, String> invalidHeaders = LinkedListMultimap.create();
		invalidHeaders.put(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());

		Response<ByteSource> invalidResponse = createFakeResponse(201, invalidHeaders);

		//  when:
		reportPortalErrorHandler.handle(invalidResponse);
	}

	@Test
	public void handle_json_lowercase() throws Exception {
		//  given:
		LinkedListMultimap<String, String> invalidHeaders = LinkedListMultimap.create();
		invalidHeaders.put(HttpHeaders.CONTENT_TYPE.toLowerCase(), ContentType.APPLICATION_JSON.getMimeType());

		Response<ByteSource> invalidResponse = createFakeResponse(200, invalidHeaders);

		//  when:
		reportPortalErrorHandler.handle(invalidResponse);
	}

	@Test
	public void handle_error_code() throws Exception {
		//  given:
		LinkedListMultimap<String, String> invalidHeaders = LinkedListMultimap.create();
		invalidHeaders.put(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());

		Response<ByteSource> invalidResponse = createFakeResponse(500, invalidHeaders);

		//  when:
		Assertions.assertThrows(GeneralReportPortalException.class, () -> reportPortalErrorHandler.handle(invalidResponse));
	}

	@Test
	public void handle_known_error() throws Exception {
		//  given:
		LinkedListMultimap<String, String> invalidHeaders = LinkedListMultimap.create();
		invalidHeaders.put(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());

		Response<ByteSource> invalidResponse = createFakeResponse(500,
				invalidHeaders,
				"{\"error_code\": \"4004\",\"stack_trace\": \"test.com\",\"message\": \"Test message goes here\"}"
		);

		//  when:
		Assertions.assertThrows(GeneralReportPortalException.class, () -> reportPortalErrorHandler.handle(invalidResponse));
	}

	@Test
	public void hasError() throws Exception {
		//  given:
		LinkedListMultimap<String, String> validHeaders = LinkedListMultimap.create();
		validHeaders.put(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());

		LinkedListMultimap<String, String> validHeaders2 = LinkedListMultimap.create();
		validHeaders2.put(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");

		LinkedListMultimap<String, String> invalidHeaders1 = LinkedListMultimap.create();
		invalidHeaders1.put(HttpHeaders.CONTENT_TYPE, ContentType.TEXT_HTML.getMimeType());

		LinkedListMultimap<String, String> invalidHeaders2 = LinkedListMultimap.create();
		invalidHeaders2.put(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_OCTET_STREAM.getMimeType());

		//  then:
		assertThat(reportPortalErrorHandler.hasError(createFakeResponse(500, validHeaders)), equalTo(true));
		assertThat(reportPortalErrorHandler.hasError(createFakeResponse(404, validHeaders)), equalTo(true));

		assertThat(reportPortalErrorHandler.hasError(createFakeResponse(200, invalidHeaders1)), equalTo(true));
		assertThat(reportPortalErrorHandler.hasError(createFakeResponse(200, invalidHeaders2)), equalTo(true));

		//		and:
		assertThat(reportPortalErrorHandler.hasError(createFakeResponse(200, validHeaders)), equalTo(false));
		assertThat(reportPortalErrorHandler.hasError(createFakeResponse(200, validHeaders2)), equalTo(false));
	}

	private Response<ByteSource> createFakeResponse(int statusCode, LinkedListMultimap<String, String> headers) {
		return createFakeResponse(statusCode, headers, "test");
	}

	private Response<ByteSource> createFakeResponse(int statusCode, LinkedListMultimap<String, String> headers, final String value) {
		return new Response<ByteSource>(URI.create("test"), HttpMethod.GET, statusCode, "testue", headers, new ByteSource() {
			@Override
			public InputStream openStream() throws IOException {
				return new ByteArrayInputStream(value.getBytes());
			}
		});
	}
}
