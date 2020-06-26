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
import com.epam.reportportal.service.analytics.item.AnalyticsItem;
import com.epam.reportportal.utils.properties.ClientProperties;
import com.epam.reportportal.utils.properties.DefaultProperties;
import com.epam.reportportal.utils.properties.SystemAttributesExtractor;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.schedulers.Schedulers;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;

public class AnalyticsService implements Closeable {

	private static final String CLIENT_PROPERTIES_FILE = "client.properties";
	private static final String START_LAUNCH_EVENT_ACTION = "Start launch";

	private final ExecutorService googleAnalyticsExecutor = Executors.newSingleThreadExecutor();
	private final GoogleAnalytics googleAnalytics = new GoogleAnalytics(Schedulers.from(googleAnalyticsExecutor), "UA-96321031-1");
	private final List<AnalyticsItem> analyticsItems = new CopyOnWriteArrayList<>();
	private final List<Completable> dependencies = new CopyOnWriteArrayList<>();

	private final ListenerParameters parameters;
	private final Maybe<String> launchId;

	public AnalyticsService(ListenerParameters listenerParameters, Maybe<String> launchIdMaybe) {
		this.parameters = listenerParameters;
		this.launchId = launchIdMaybe;
	}

	protected GoogleAnalytics getGoogleAnalytics() {
		return googleAnalytics;
	}

	public void send() {
		dependencies.addAll(analyticsItems.stream()
				.map(it -> launchId.flatMap(l -> getGoogleAnalytics().send(it)))
				.map(Maybe::ignoreElement)
				.collect(toList()));
	}

	public void addStartLaunchEvent(StartLaunchRQ rq) {
		AnalyticsEvent.AnalyticsEventBuilder analyticsEventBuilder = AnalyticsEvent.builder();
		analyticsEventBuilder.withAction(START_LAUNCH_EVENT_ACTION);
		SystemAttributesExtractor.extract(CLIENT_PROPERTIES_FILE, getClass().getClassLoader(), ClientProperties.CLIENT)
				.stream()
				.findFirst()
				.ifPresent(clientAttribute -> analyticsEventBuilder.withCategory(clientAttribute.getValue()));

		rq.getAttributes()
				.stream()
				.filter(attribute -> attribute.isSystem() && DefaultProperties.AGENT.getName().equalsIgnoreCase(attribute.getKey()))
				.findFirst()
				.ifPresent(agentAttribute -> analyticsEventBuilder.withLabel(agentAttribute.getValue()));
		analyticsItems.add(analyticsEventBuilder.build());
	}

	@Override
	public void close() {
		try {
			Completable.concat(dependencies).timeout(parameters.getReportingTimeout(), TimeUnit.SECONDS).blockingGet();
		} finally {
			getGoogleAnalytics().close();
			googleAnalyticsExecutor.shutdown();
			try {
				this.googleAnalyticsExecutor.awaitTermination(parameters.getReportingTimeout(), TimeUnit.SECONDS);
			} catch (InterruptedException exc) {
				//do nothing
			}
		}
	}
}
