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

package com.epam.reportportal.service.analytics;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.analytics.item.AnalyticsEvent;
import com.epam.reportportal.utils.properties.ClientProperties;
import com.epam.reportportal.utils.properties.DefaultProperties;
import com.epam.reportportal.utils.properties.SystemAttributesExtractor;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributeResource;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static java.util.Optional.ofNullable;

public class AnalyticsService implements Closeable {
	public static final String ANALYTICS_PROPERTY = "AGENT_NO_ANALYTICS";

	private static final String CLIENT_PROPERTIES_FILE = "client.properties";
	private static final String START_LAUNCH_EVENT_ACTION = "Start launch";
	private static final String CLIENT_VALUE_FORMAT = "Client name \"%s\", version \"%s\"";
	private static final String AGENT_VALUE_FORMAT = "Agent name \"%s\", version \"%s\"";

	private final ExecutorService googleAnalyticsExecutor = Executors.newSingleThreadExecutor();
	private final Scheduler scheduler = Schedulers.from(googleAnalyticsExecutor);
	private final Analytics analytics;
	private final List<Completable> dependencies = new CopyOnWriteArrayList<>();

	private final ListenerParameters parameters;

	public AnalyticsService(ListenerParameters listenerParameters) {
		this.parameters = listenerParameters;
		boolean isDisabled = System.getenv(ANALYTICS_PROPERTY) != null;
		analytics = isDisabled ? new DummyAnalytics() : new Statistics("UA-173456809-1", parameters.getProxyUrl());
	}

	protected Analytics getAnalytics() {
		return analytics;
	}

	public void sendEvent(Maybe<String> launchIdMaybe, StartLaunchRQ rq) {
		AnalyticsEvent.AnalyticsEventBuilder analyticsEventBuilder = AnalyticsEvent.builder().withAction(START_LAUNCH_EVENT_ACTION);
		SystemAttributesExtractor.extract(CLIENT_PROPERTIES_FILE, getClass().getClassLoader(), ClientProperties.CLIENT)
				.stream()
				.map(ItemAttributeResource::getValue)
				.map(a -> a.split(Pattern.quote(SystemAttributesExtractor.ATTRIBUTE_VALUE_SEPARATOR)))
				.filter(a -> a.length >= 2)
				.findFirst()
				.ifPresent(clientAttribute -> analyticsEventBuilder.withCategory(String.format(CLIENT_VALUE_FORMAT,
						(Object[]) clientAttribute
				)));

		ofNullable(rq.getAttributes()).flatMap(r -> r.stream()
				.filter(attribute -> attribute.isSystem() && DefaultProperties.AGENT.getName().equalsIgnoreCase(attribute.getKey()))
				.findAny())
				.map(ItemAttributeResource::getValue)
				.map(a -> a.split(Pattern.quote(SystemAttributesExtractor.ATTRIBUTE_VALUE_SEPARATOR)))
				.filter(a -> a.length >= 2)
				.ifPresent(agentAttribute -> analyticsEventBuilder.withLabel(String.format(AGENT_VALUE_FORMAT, (Object[]) agentAttribute)));
		Maybe<Boolean> analyticsMaybe = launchIdMaybe.map(l -> getAnalytics().send(analyticsEventBuilder.build()))
				.cache()
				.subscribeOn(scheduler);
		dependencies.add(analyticsMaybe.ignoreElement());
		//noinspection ResultOfMethodCallIgnored
		analyticsMaybe.subscribe(t -> {
		});
	}

	@Override
	public void close() {
		try {
			Throwable result = Completable.concat(dependencies).timeout(parameters.getReportingTimeout(), TimeUnit.SECONDS).blockingGet();
			if (result != null) {
				throw new RuntimeException("Unable to complete execution of all dependencies", result);
			}
		} finally {
			googleAnalyticsExecutor.shutdown();
			try {
				if (!googleAnalyticsExecutor.awaitTermination(parameters.getReportingTimeout(), TimeUnit.SECONDS)) {
					googleAnalyticsExecutor.shutdownNow();
				}
			} catch (InterruptedException exc) {
				//do nothing
			}
			try {
				getAnalytics().close();
			} catch (IOException ignore) {
			}
		}
	}
}
