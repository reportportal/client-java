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
import org.apache.http.HttpHost;
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

import static java.util.Optional.ofNullable;

/**
 * Google analytics asynchronous client. Required for sending analytics event to the resource identified by provided `trackingId`
 *
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class GoogleAnalytics implements Closeable {

	private static final Logger LOGGER = LoggerFactory.getLogger(GoogleAnalytics.class);

	private static final Function<Map<String, String>, List<NameValuePair>> PARAMETERS_CONVERTER = params -> params.entrySet()
			.stream()
			.map(entry -> new BasicNameValuePair(entry.getKey(), entry.getValue()))
			.collect(Collectors.toList());

	private static final String DEFAULT_BASE_URL = "https://www.google-analytics.com/collect";

	private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.131 Safari/537.36";

	private final String baseUrl;

	private final List<NameValuePair> defaultRequestParams = new ArrayList<>();

	private final HttpClient httpClient;

	public GoogleAnalytics(String trackingId, String proxyUrl) {
		this(trackingId, buildDefaultHttpClient(proxyUrl));
	}

	/**
	 * Adds set of mandatory parameters to the request params:
	 * de - Encoding
	 * v - Protocol version
	 * cid - Client id
	 * tid - Google analytics resource id
	 *
	 * @param trackingId ID of the `Google analytics` resource
	 * @param httpClient {@link HttpClient} instance
	 */
	public GoogleAnalytics(String trackingId, HttpClient httpClient) {
		this.baseUrl = DEFAULT_BASE_URL;
		Collections.addAll(
				defaultRequestParams,
				new BasicNameValuePair("de", "UTF-8"),
				new BasicNameValuePair("v", "1"),
				new BasicNameValuePair("cid", UUID.randomUUID().toString()),
				new BasicNameValuePair("tid", trackingId)
		);

		this.httpClient = httpClient;
	}

	private static HttpClient buildDefaultHttpClient(String proxyUrl) {
		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
		connectionManager.setDefaultMaxPerRoute(1);
		HttpClientBuilder httpClientBuilder = HttpClients.custom().setConnectionManager(connectionManager).setUserAgent(USER_AGENT);
		ofNullable(proxyUrl).ifPresent(u -> httpClientBuilder.setProxy(HttpHost.create(proxyUrl)));
		return httpClientBuilder.build();
	}

	/**
	 * Convert and send {@link AnalyticsItem} to the `Google analytics` instance. Quietly consumes exceptions to not affect reporting flow
	 *
	 * @param item {@link AnalyticsItem}
	 * @return true - if successfully send, otherwise - false wrapped in the {@link Maybe}
	 */
	public Boolean send(AnalyticsItem item) {
		try {
			HttpPost httpPost = buildPostRequest(item);
			HttpResponse response = httpClient.execute(httpPost);
			try {
				EntityUtils.consumeQuietly(response.getEntity());
			} finally {
				if (response instanceof CloseableHttpResponse) {
					((CloseableHttpResponse) response).close();
				}
			}
			return true;
		} catch (Throwable ex) {
			LOGGER.error(ex.getMessage());
			return false;
		}
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
