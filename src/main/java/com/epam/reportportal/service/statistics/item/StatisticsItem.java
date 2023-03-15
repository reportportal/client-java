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

package com.epam.reportportal.service.statistics.item;

import com.epam.reportportal.service.statistics.StatisticsClient;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Interface for mapping any Statistics entity to the parameters mapping
 * that will be used in the {@link StatisticsClient#send(StatisticsItem)}
 *
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class StatisticsItem {

	private final String clientId;
	private List<StatisticsEvent> events = new ArrayList<>();

	public StatisticsItem(String clientId) {
		this.clientId = clientId;
	}

	public String getClientId() {
		return clientId;
	}

	public void setEvents(@Nonnull List<StatisticsEvent> events) {
		Objects.requireNonNull(events);
		this.events = new ArrayList<>(events);
	}

	public List<StatisticsEvent> getEvents() {
		return new ArrayList<>(events);
	}

	public StatisticsItem addEvent(StatisticsEvent event) {
		events.add(event);
		return this;
	}
}
