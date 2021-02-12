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

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.analytics.item.AnalyticsItem;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HttpHeaders;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.reactivex.Maybe;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Google analytics asynchronous client. Required for sending analytics event to the resource identified by provided `trackingId`
 *
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class StatisticsService implements Statistics {
	private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.131 Safari/537.36";
	private static final String BASE_URL = "https://www.google-analytics.com/";

	private static final Map<String, String> CONSTANT_REQUEST_PARAMS = ImmutableMap.<String, String>builder().put("de", "UTF-8")
			.put("v", "1")
			.build();

	private final Map<String, String> commonParameters;
	private final StatisticsClient client;
	private OkHttpClient httpClient;
	private ExecutorService executor;

	private static OkHttpClient buildHttpClient(ListenerParameters parameters) {
		OkHttpClient.Builder okHttpClient = new OkHttpClient.Builder().addInterceptor(chain -> {
			Request newRequest = chain.request().newBuilder().addHeader(HttpHeaders.USER_AGENT, USER_AGENT).build();
			return chain.proceed(newRequest);
		});
		String proxyStr = parameters.getProxyUrl();

		if (isNotBlank(proxyStr)) {
			try {
				URL proxyUrl = new URL(proxyStr);
				String host = proxyUrl.getHost();
				int port = proxyUrl.getPort();
				InetSocketAddress address = InetSocketAddress.createUnresolved(host, port >= 0 ? port : proxyUrl.getDefaultPort());
				okHttpClient.proxy(new Proxy(Proxy.Type.HTTP, address));
			} catch (MalformedURLException ignore) {
			}
		}
		okHttpClient.retryOnConnectionFailure(true);
		return okHttpClient.build();
	}

	private static StatisticsClient buildClient(OkHttpClient httpClient, Scheduler scheduler) {
		RxJava2CallAdapterFactory rxFactory = RxJava2CallAdapterFactory.createWithScheduler(scheduler);
		Retrofit retrofit = new Retrofit.Builder().baseUrl(BASE_URL)
				.addConverterFactory(JacksonConverterFactory.create())
				.addCallAdapterFactory(rxFactory)
				.client(httpClient)
				.build();
		return retrofit.create(StatisticsClient.class);
	}

	private static Map<String, String> buildParams(String trackingId) {
		return ImmutableMap.<String, String>builder().putAll(CONSTANT_REQUEST_PARAMS)
				.put("cid", UUID.randomUUID().toString())
				.put("tid", trackingId)
				.build();
	}

	/**
	 * Adds set of mandatory parameters to the request params:
	 * de - Encoding
	 * v - Protocol version
	 * cid - Client id
	 * tid - Google analytics resource id
	 *
	 * @param trackingId ID of the `Google analytics` resource
	 * @param parameters {@link ListenerParameters} Report Portal client parameters
	 */
	public StatisticsService(String trackingId, ListenerParameters parameters) {
		commonParameters = buildParams(trackingId);
		executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("rp-stat-%s").setDaemon(true).build());
		httpClient = buildHttpClient(parameters);
		client = buildClient(httpClient, Schedulers.from(executor));
	}

	/**
	 * Adds set of mandatory parameters to the request params:
	 * de - Encoding
	 * v - Protocol version
	 * cid - Client id
	 * tid - Google analytics resource id
	 *
	 * @param trackingId       ID of the `Google analytics` resource
	 * @param statisticsClient {@link ListenerParameters} Report Portal client parameters
	 */
	public StatisticsService(String trackingId, StatisticsClient statisticsClient) {
		commonParameters = buildParams(trackingId);
		client = statisticsClient;
	}

	/**
	 * Convert and send {@link AnalyticsItem} to the `Google analytics` instance. Quietly consumes exceptions to not affect reporting flow
	 *
	 * @param item {@link AnalyticsItem}
	 * @return true - if successfully send, otherwise - false wrapped in the {@link Maybe}
	 */
	@Override
	public Maybe<Response<ResponseBody>> send(AnalyticsItem item) {
		return client.send(buildPostRequest(item));
	}

	private Map<String, String> buildPostRequest(AnalyticsItem item) {
		Map<String, String> nameValuePairs = new HashMap<>(item.getParams());
		nameValuePairs.putAll(commonParameters);
		return nameValuePairs;
	}

	@Override
	public void close() {
		ofNullable(executor).ifPresent(e -> {
			e.shutdown();
			try {
				if (!e.awaitTermination(10, TimeUnit.SECONDS)) {
					e.shutdownNow();
				}
			} catch (InterruptedException ignore) {
			}
		});
		ofNullable(httpClient).ifPresent(c -> {
			ExecutorService e = c.dispatcher().executorService();
			e.shutdown();
			try {
				if (!e.awaitTermination(10, TimeUnit.SECONDS)) {
					e.shutdownNow();
				}
			} catch (InterruptedException ignore) {
			}
			c.connectionPool().evictAll();
		});
	}
}
