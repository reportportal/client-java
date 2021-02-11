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
import com.google.common.collect.Maps;
import com.google.common.net.HttpHeaders;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.reactivex.Maybe;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Google analytics asynchronous client. Required for sending analytics event to the resource identified by provided `trackingId`
 *
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class StatisticsService implements Statistics {
	private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.131 Safari/537.36";
	private static final String BASE_URL = "https://www.google-analytics.com/";

	private final ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat(
			"rp-stat-%s").setDaemon(true).build());
	private final Scheduler scheduler = Schedulers.from(executor);
	private final Map<String, String> defaultRequestParams = Maps.newHashMapWithExpectedSize(4);

	private final StatisticsClient client;

	/**
	 * Adds set of mandatory parameters to the request params:
	 * de - Encoding
	 * v - Protocol version
	 * cid - Client id
	 * tid - Google analytics resource id
	 *
	 * @param trackingId       ID of the `Google analytics` resource
	 * @param parameters {@link ListenerParameters} Report Portal client parameters
	 */
	public StatisticsService(String trackingId, ListenerParameters parameters) {
		this(trackingId, buildClient(parameters));
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
		defaultRequestParams.put("de", "UTF-8");
		defaultRequestParams.put("v", "1");
		defaultRequestParams.put("cid", UUID.randomUUID().toString());
		defaultRequestParams.put("tid", trackingId);
		client = statisticsClient;
	}


	private static StatisticsClient buildClient(ListenerParameters parameters) {
		OkHttpClient.Builder okHttpClient =	new OkHttpClient.Builder().addInterceptor(chain -> {
			Request newRequest =
					chain.request().newBuilder()
							.addHeader(HttpHeaders.USER_AGENT, USER_AGENT)
							.build();
			return chain.proceed(newRequest);
		});

		RxJava2CallAdapterFactory rxFactory = RxJava2CallAdapterFactory.createWithScheduler(scheduler);
		Retrofit retrofit = new Retrofit.Builder().baseUrl(BASE_URL)
				.addConverterFactory(JacksonConverterFactory.create())
				.addCallAdapterFactory(rxFactory)
				.client(okHttpClient.build())
				.build();
		return retrofit.create(StatisticsClient.class);
	}

	/**
	 * Convert and send {@link AnalyticsItem} to the `Google analytics` instance. Quietly consumes exceptions to not affect reporting flow
	 *
	 * @param item {@link AnalyticsItem}
	 * @return true - if successfully send, otherwise - false wrapped in the {@link Maybe}
	 */
	@Override
	public Maybe<Void> send(AnalyticsItem item) {
		return client.send(buildPostRequest(item));
	}

	private Map<String, String> buildPostRequest(AnalyticsItem item) {
		Map<String, String> nameValuePairs = new HashMap<>(item.getParams());
		nameValuePairs.putAll(defaultRequestParams);
		return nameValuePairs;
	}

	@Override
	public void close() throws IOException {

	}
}
