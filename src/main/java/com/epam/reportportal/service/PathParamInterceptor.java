/*
 *  Copyright 2021 EPAM Systems
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.epam.reportportal.service;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;

/**
 * OkHttp {@link Interceptor} implementation that substitutes path parameters in URL segments.
 * <p>
 * This interceptor replaces token strings in the URL path segments with specified values.
 * A path parameter is identified by the format {@code {paramName}}, where 'paramName' is the name
 * of the parameter to be substituted.
 * <p>
 * Example:
 * For a URL like "https://example.com/api/{projectName}/launch"
 * When configured with PathParamInterceptor("projectName", "testProject")
 * The resulting URL will be "https://example.com/api/testProject/launch"
 * <p>
 * This interceptor is used in the ReportPortal client to replace project name placeholders
 * in API endpoints with the actual project name specified in the client configuration.
 */
@SuppressWarnings("JavadocLinkAsPlainText")
public class PathParamInterceptor implements Interceptor {

	private static final String PARAMETER_PATTERN = "{%s}";

	private final String key;
	private final String value;

	/**
	 * Constructs a {@link PathParamInterceptor} with specified parameter name and replacement value.
	 *
	 * @param replaceKey   The name of the parameter to replace (will be wrapped in {} in the URL)
	 * @param replaceValue The value to substitute for the parameter
	 */
	public PathParamInterceptor(String replaceKey, String replaceValue) {
		key = String.format(PARAMETER_PATTERN, replaceKey);
		value = replaceValue;
	}

	/**
	 * Intercepts the HTTP request and replaces any path segments containing the specified parameter
	 * with the replacement value.
	 *
	 * @param chain The interceptor chain
	 * @return The response from the chain after processing the modified request
	 * @throws IOException If an I/O error occurs during request processing
	 */
	@Override
	@Nonnull
	public Response intercept(Chain chain) throws IOException {
		Request originalRequest = chain.request();

		List<String> segments = originalRequest.url().pathSegments();
		HttpUrl.Builder urlBuilder = originalRequest.url().newBuilder();
		IntStream.range(0, segments.size()).forEach(i -> {
			String s = segments.get(i);
			if (s.contains(key)) {
				urlBuilder.setPathSegment(i, s.replace(key, value));
			}
		});
		Request request = originalRequest.newBuilder().url(urlBuilder.build()).build();
		return chain.proceed(request);
	}
}
