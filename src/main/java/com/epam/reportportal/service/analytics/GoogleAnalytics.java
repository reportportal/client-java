/*
 * Copyright 2020 EPAM Systems
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

package com.epam.reportportal.service.analytics;

import com.epam.reportportal.service.analytics.item.AnalyticsItem;
import io.reactivex.Maybe;
import io.reactivex.MaybeOnSubscribe;
import io.reactivex.Scheduler;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class GoogleAnalytics implements Closeable {

	private static final Logger LOGGER = LoggerFactory.getLogger(GoogleAnalytics.class);

	private static final Function<Map<String, String>, List<NameValuePair>> PARAMETERS_CONVERTER = params -> params.entrySet()
			.stream()
			.map(entry -> new BasicNameValuePair(entry.getKey(), entry.getValue()))
			.collect(Collectors.toList());

	private static final String DEFAULT_BASE_URL = "https://www.google-analytics.com/collect";

	private final Scheduler scheduler;

	private final String baseUrl;

	private final List<NameValuePair> defaultRequestParams = new ArrayList<>();

	private final HttpClient httpClient;

	public GoogleAnalytics(Scheduler scheduler, String trackingId) {
		this.scheduler = scheduler;
		this.baseUrl = DEFAULT_BASE_URL;
		Collections.addAll(defaultRequestParams,
				new BasicNameValuePair("de", "UTF-8"),
				new BasicNameValuePair("v", "1"),
				new BasicNameValuePair("cid", UUID.randomUUID().toString()),
				new BasicNameValuePair("tid", trackingId)
		);

		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
		connectionManager.setDefaultMaxPerRoute(1);
		HttpClientBuilder httpClientBuilder = HttpClients.custom().setConnectionManager(connectionManager);

		this.httpClient = httpClientBuilder.build();

	}

	public GoogleAnalytics(Scheduler scheduler, String trackingId, HttpClient httpClient) {
		this.scheduler = scheduler;
		this.baseUrl = DEFAULT_BASE_URL;
		Collections.addAll(defaultRequestParams,
				new BasicNameValuePair("de", "UTF-8"),
				new BasicNameValuePair("v", "1"),
				new BasicNameValuePair("cid", UUID.randomUUID().toString()),
				new BasicNameValuePair("tid", trackingId)
		);

		this.httpClient = httpClient;
	}

	public Maybe<Boolean> send(AnalyticsItem item) {
		return Maybe.create((MaybeOnSubscribe<Boolean>) emitter -> {
			HttpPost httpPost = buildPostRequest(item);
			HttpResponse response = httpClient.execute(httpPost);
			try {
				EntityUtils.consumeQuietly(response.getEntity());
				emitter.onSuccess(true);
			} finally {
				if (response instanceof CloseableHttpResponse) {
					((CloseableHttpResponse) response).close();
				}
			}
		}).subscribeOn(scheduler).cache();

	}

	private HttpPost buildPostRequest(AnalyticsItem item) {
		List<NameValuePair> nameValuePairs = PARAMETERS_CONVERTER.apply(item.getParams());
		nameValuePairs.addAll(defaultRequestParams);
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(nameValuePairs, StandardCharsets.UTF_8);
		HttpPost httpPost = new HttpPost(baseUrl);
		httpPost.setEntity(entity);
		return httpPost;
	}

	@Override
	public void close() {
		if (httpClient instanceof CloseableHttpClient) {
			try {
				((CloseableHttpClient) httpClient).close();
			} catch (Exception ex) {
				LOGGER.error(ex.getMessage());
			}
		}
	}
}
