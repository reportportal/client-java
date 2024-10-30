/*
 *  Copyright 2020 EPAM Systems
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

package com.epam.reportportal.service.statistics;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.statistics.item.StatisticsEvent;
import com.epam.reportportal.service.statistics.item.StatisticsItem;
import com.epam.reportportal.utils.ClientIdProvider;
import com.epam.reportportal.utils.properties.ClientProperties;
import com.epam.reportportal.utils.properties.DefaultProperties;
import com.epam.reportportal.utils.properties.SystemAttributesExtractor;
import com.epam.ta.reportportal.ws.reporting.ItemAttributeResource;
import com.epam.ta.reportportal.ws.reporting.StartLaunchRQ;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

public class StatisticsService implements Closeable {
	private static final Logger LOGGER = LoggerFactory.getLogger(StatisticsService.class);

	private static final String CLIENT_INFO = "Ry1XUDU3UlNHOFhMOjUxREVTTzQ4UV9DbmlnbVEwY2JoYmc=";
	private static final String[] DECODED_CLIENT_INFO = new String(Base64.getDecoder().decode(CLIENT_INFO),
			StandardCharsets.UTF_8
	).split(":");

	private static final String CLIENT_PROPERTIES_FILE = "client.properties";
	public static final String START_LAUNCH_EVENT_ACTION = "start_launch";
	public static final String CLIENT_NAME_PARAM = "client_name";
	public static final String CLIENT_VERSION_PARAM = "client_version";
	public static final String INTERPRETER_PARAM = "interpreter";
	public static final String INTERPRETER_FORMAT = "Java %s";
	public static final String AGENT_NAME_PARAM = "agent_name";
	public static final String AGENT_VERSION_PARAM = "agent_version";

	private static final String CLIENT_ID = ClientIdProvider.getClientId();

	private static final AtomicLong THREAD_COUNTER = new AtomicLong();
	private static final ThreadFactory THREAD_FACTORY = r -> {
		Thread t = new Thread(r);
		t.setDaemon(true);
		t.setName("rp-stat-" + THREAD_COUNTER.incrementAndGet());
		return t;
	};
	private final ExecutorService statisticsExecutor = Executors.newSingleThreadExecutor(THREAD_FACTORY);
	private final Scheduler scheduler = Schedulers.from(statisticsExecutor);
	private final Statistics statistics;
	private final List<Completable> dependencies = new CopyOnWriteArrayList<>();

	private final ListenerParameters parameters;

	public StatisticsService(ListenerParameters listenerParameters, Statistics client) {
		this.parameters = listenerParameters;
		this.statistics = client;
	}

	public StatisticsService(ListenerParameters listenerParameters) {
		this(listenerParameters,
				new StatisticsClient(DECODED_CLIENT_INFO[0], DECODED_CLIENT_INFO[1], listenerParameters));
	}

	protected Statistics getStatistics() {
		return statistics;
	}

	public void sendEvent(Maybe<String> launchIdMaybe, StartLaunchRQ rq) {
		StatisticsEvent event = new StatisticsEvent(START_LAUNCH_EVENT_ACTION);
		SystemAttributesExtractor.extract(CLIENT_PROPERTIES_FILE, getClass().getClassLoader(), ClientProperties.CLIENT)
				.stream()
				.map(ItemAttributeResource::getValue)
				.map(a -> a.split(Pattern.quote(SystemAttributesExtractor.ATTRIBUTE_VALUE_SEPARATOR)))
				.filter(a -> a.length >= 2)
				.flatMap(a -> Stream.of(Pair.of(CLIENT_NAME_PARAM, a[0]),
						Pair.of(CLIENT_VERSION_PARAM, a[1]),
						Pair.of(INTERPRETER_PARAM,
								String.format(INTERPRETER_FORMAT, System.getProperty("java.version"))
						)
				))
				.forEach(p -> event.addParam(p.getKey(), p.getValue()));

		ofNullable(rq.getAttributes()).flatMap(r -> r.stream()
						.filter(attribute -> attribute.isSystem() && DefaultProperties.AGENT.getName()
								.equalsIgnoreCase(attribute.getKey()))
						.findAny())
				.map(ItemAttributeResource::getValue)
				.map(a -> a.split(Pattern.quote(SystemAttributesExtractor.ATTRIBUTE_VALUE_SEPARATOR)))
				.filter(a -> a.length >= 2)
				.ifPresent(a -> Stream.of(Pair.of(AGENT_NAME_PARAM, a[0]), Pair.of(AGENT_VERSION_PARAM, a[1]))
						.forEach(p -> event.addParam(p.getKey(), p.getValue())));

		Maybe<Response<ResponseBody>> statisticsMaybe = launchIdMaybe.flatMap(l -> getStatistics().send(new StatisticsItem(
				CLIENT_ID).addEvent(event))).cache().subscribeOn(scheduler);
		dependencies.add(statisticsMaybe.ignoreElement());
		//noinspection ResultOfMethodCallIgnored
		statisticsMaybe.subscribe(t -> {
			ofNullable(t.body()).ifPresent(ResponseBody::close);
			getStatistics().close();
		}, t -> {
			LOGGER.error("Unable to send statistics", t);
			getStatistics().close();
		});
	}

	@Override
	public void close() {
		Throwable result = Completable.concat(dependencies)
				.timeout(parameters.getReportingTimeout(), TimeUnit.SECONDS)
				.blockingGet();
		if (result != null) {
			LOGGER.warn("Unable to complete execution of all dependencies", result);
		}
		statisticsExecutor.shutdown();
		try {
			if (!statisticsExecutor.awaitTermination(parameters.getReportingTimeout(), TimeUnit.SECONDS)) {
				statisticsExecutor.shutdownNow();
			}
		} catch (InterruptedException exc) {
			//do nothing
		}
		try {
			getStatistics().close();
		} catch (IOException ignore) {
		}
	}
}
