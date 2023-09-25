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

package com.epam.reportportal.service.statistics;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.statistics.item.StatisticsItem;
import com.epam.reportportal.utils.http.ClientUtils;
import io.reactivex.Maybe;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Optional.ofNullable;

/**
 * Statistics backend service asynchronous client. Require resource identifier by provided `trackingId` for sending statistics event.
 */
public class StatisticsClient implements Statistics {
	private static final String BASE_URL = "https://www.google-analytics.com/";

	private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.107 Safari/537.36";

	private static final AtomicLong THREAD_COUNTER = new AtomicLong();
	private static final ThreadFactory THREAD_FACTORY = r -> {
		Thread t = new Thread(r);
		t.setDaemon(true);
		t.setName("rp-stat-io-" + THREAD_COUNTER.incrementAndGet());
		return t;
	};

	private final StatisticsApiClient client;
	private final String measurementId;
	private final String apiSecret;
	private OkHttpClient httpClient;
	private ExecutorService executor;

	private static OkHttpClient buildHttpClient(ListenerParameters parameters) {
		return ClientUtils.setupProxy(new OkHttpClient.Builder(), parameters).retryOnConnectionFailure(true).build();
	}

	private static StatisticsApiClient buildClient(OkHttpClient httpClient, Scheduler scheduler) {
		RxJava2CallAdapterFactory rxFactory = RxJava2CallAdapterFactory.createWithScheduler(scheduler);
		Retrofit retrofit = new Retrofit.Builder().baseUrl(BASE_URL)
				.addConverterFactory(JacksonConverterFactory.create())
				.addCallAdapterFactory(rxFactory)
				.client(httpClient)
				.build();
		return retrofit.create(StatisticsApiClient.class);
	}

	/**
	 * Create an instance of the client, construct own HTTP client by given parameters
	 *
	 * @param measurementId ID of the statistics resource
	 * @param apiSecret     API Secret Key
	 * @param parameters    {@link ListenerParameters} ReportPortal parameters
	 */
	public StatisticsClient(String measurementId, String apiSecret, ListenerParameters parameters) {
		this.measurementId = measurementId;
		this.apiSecret = apiSecret;
		executor = Executors.newSingleThreadExecutor(THREAD_FACTORY);
		httpClient = buildHttpClient(parameters);
		client = buildClient(httpClient, Schedulers.from(executor));
	}

	/**
	 * Create an instance of the client with given HTTP client
	 *
	 * @param measurementId       ID of the statistics resource
	 * @param apiSecret           API Secret Key
	 * @param statisticsApiClient {@link StatisticsApiClient} instance
	 */
	public StatisticsClient(String measurementId, String apiSecret, StatisticsApiClient statisticsApiClient) {
		this.measurementId = measurementId;
		this.apiSecret = apiSecret;
		client = statisticsApiClient;
	}

	/**
	 * Convert and send {@link StatisticsItem} to backend statistics service. Quietly consumes exceptions to not affect reporting flow
	 *
	 * @param item {@link StatisticsItem}
	 * @return true - if successfully send, otherwise - false wrapped in the {@link Maybe}
	 */
	@Override
	public Maybe<Response<ResponseBody>> send(StatisticsItem item) {
		return client.send(USER_AGENT, measurementId, apiSecret, item);
	}

	@Override
	public void close() {
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
		httpClient = null;
		ofNullable(executor).ifPresent(ExecutorService::shutdownNow);
		executor = null;
	}
}
