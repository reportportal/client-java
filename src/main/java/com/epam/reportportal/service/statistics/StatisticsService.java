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
import com.epam.reportportal.utils.properties.ClientProperties;
import com.epam.reportportal.utils.properties.DefaultProperties;
import com.epam.reportportal.utils.properties.SystemAttributesExtractor;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributeResource;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static java.util.Optional.ofNullable;

public class StatisticsService implements Closeable {
	private static final Logger LOGGER = LoggerFactory.getLogger(StatisticsService.class);

	public static final String DISABLE_PROPERTY = "AGENT_NO_ANALYTICS";

	private static final String CLIENT_PROPERTIES_FILE = "client.properties";
	private static final String START_LAUNCH_EVENT_ACTION = "Start launch";
	private static final String CATEGORY_VALUE_FORMAT = "Client name \"%s\", version \"%s\", interpreter \"Java %s\"";
	private static final String LABEL_VALUE_FORMAT = "Agent name \"%s\", version \"%s\"";

	private final ExecutorService statisticsExecutor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat(
			"rp-stat-%s").setDaemon(true).build());
	private final Scheduler scheduler = Schedulers.from(statisticsExecutor);
	private final Statistics statistics;
	private final List<Completable> dependencies = new CopyOnWriteArrayList<>();

	private final ListenerParameters parameters;

	public StatisticsService(ListenerParameters listenerParameters) {
		this.parameters = listenerParameters;
		boolean isDisabled = System.getenv(DISABLE_PROPERTY) != null;
		statistics = isDisabled ? new DummyClient() : new StatisticsClient("UA-173456809-1", parameters);
	}

	protected Statistics getStatistics() {
		return statistics;
	}

	public void sendEvent(Maybe<String> launchIdMaybe, StartLaunchRQ rq) {
		StatisticsEvent.StatisticsEventBuilder statisticsEventBuilder = StatisticsEvent.builder().withAction(START_LAUNCH_EVENT_ACTION);
		SystemAttributesExtractor.extract(CLIENT_PROPERTIES_FILE, getClass().getClassLoader(), ClientProperties.CLIENT)
				.stream()
				.map(ItemAttributeResource::getValue)
				.map(a -> a.split(Pattern.quote(SystemAttributesExtractor.ATTRIBUTE_VALUE_SEPARATOR)))
				.filter(a -> a.length >= 2)
				.map(a -> {
					Object[] r = new Object[a.length + 1];
					System.arraycopy(a, 0, r, 0, a.length);
					r[a.length] = System.getProperty("java.version");
					return r;
				})
				.findFirst()
				.ifPresent(clientAttribute -> statisticsEventBuilder.withCategory(String.format(CATEGORY_VALUE_FORMAT, clientAttribute)));

		ofNullable(rq.getAttributes()).flatMap(r -> r.stream()
						.filter(attribute -> attribute.isSystem() && DefaultProperties.AGENT.getName().equalsIgnoreCase(attribute.getKey()))
						.findAny())
				.map(ItemAttributeResource::getValue)
				.map(a -> a.split(Pattern.quote(SystemAttributesExtractor.ATTRIBUTE_VALUE_SEPARATOR)))
				.filter(a -> a.length >= 2)
				.ifPresent(agentAttribute -> statisticsEventBuilder.withLabel(String.format(LABEL_VALUE_FORMAT,
						(Object[]) agentAttribute
				)));
		Maybe<Response<ResponseBody>> statisticsMaybe = launchIdMaybe.flatMap(l -> getStatistics().send(statisticsEventBuilder.build()))
				.cache()
				.subscribeOn(scheduler);
		dependencies.add(statisticsMaybe.ignoreElement());
		//noinspection ResultOfMethodCallIgnored
		statisticsMaybe.subscribe(t -> {
			ofNullable(t.body()).ifPresent(ResponseBody::close);
		}, t -> LOGGER.error("Unable to send statistics", t));
	}

	@Override
	public void close() {
		try {
			Throwable result = Completable.concat(dependencies).timeout(parameters.getReportingTimeout(), TimeUnit.SECONDS).blockingGet();
			if (result != null) {
				LOGGER.warn("Unable to complete execution of all dependencies", result);
			}
		} finally {
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
}
