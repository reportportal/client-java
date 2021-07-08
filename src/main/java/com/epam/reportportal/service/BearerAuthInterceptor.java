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

import com.google.common.net.HttpHeaders;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

/**
 * Adds Bearer TOKEN to the request headers
 */
public class BearerAuthInterceptor implements Interceptor {

	private final String uuid;

	public BearerAuthInterceptor(String uuid) {
		this.uuid = uuid;
	}

	@Override
	public Response intercept(Chain chain)  throws IOException {
		Request rq = chain.request().newBuilder().addHeader(HttpHeaders.AUTHORIZATION, "bearer " + uuid).build();
		return chain.proceed(rq);
	}
}
