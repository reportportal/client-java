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

import jakarta.annotation.Nonnull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Representation of the Statistics EVENT entity
 */
public class StatisticsEvent {
	private final String name;

	private Map<String, Object> params = new HashMap<>();

	public StatisticsEvent(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setParams(@Nonnull Map<String, Object> params) {
		Objects.requireNonNull(params);
		this.params = new HashMap<>(params);
	}

	public Map<String, Object> getParams() {
		return new HashMap<>(params);
	}

	public StatisticsEvent addParam(String name, Object value) {
		params.put(name, value);
		return this;
	}
}
