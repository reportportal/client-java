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

import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;

public class PathParamInterceptor implements Interceptor {

	private static final String PARAMETER_PATTERN = "{%s}";

	private final String key;
	private final String value;

	public PathParamInterceptor(String replaceKey, String replaceValue) {
		key = String.format(PARAMETER_PATTERN, replaceKey);
		value = replaceValue;
	}

	@Override
	public Response intercept(Chain chain) throws IOException {
		Request originalRequest = chain.request();

		List<String> segments = originalRequest.url().pathSegments();
		HttpUrl.Builder urlBuilder = originalRequest.url().newBuilder();
		IntStream.range(0, segments.size()).forEach(i->{
			String s = segments.get(i);
			if (s.contains(key)) {
				urlBuilder.setPathSegment(i, s.replace(key, value));
			}
		});
		Request request = originalRequest.newBuilder()
				.url(urlBuilder.build())
				.build();
		return chain.proceed(request);
	}
}
