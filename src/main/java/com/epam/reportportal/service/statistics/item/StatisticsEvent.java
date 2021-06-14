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

import com.google.common.collect.Maps;
import io.reactivex.annotations.Nullable;

import java.util.Map;

/**
 * Representation of the `Google analytics` EVENT entity
 *
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 * @see <a href="https://support.google.com/analytics/answer/1033068">Google analytics event</a>
 */
public class StatisticsEvent implements StatisticsItem {

	private static final String TYPE = "event";

	private final Map<String, String> params;

	public StatisticsEvent(@Nullable String eventCategory, @Nullable String eventAction, @Nullable String eventLabel) {
		params = Maps.newHashMapWithExpectedSize(4);
		params.put("t", TYPE);
		params.put("ec", eventCategory);
		params.put("ea", eventAction);
		params.put("el", eventLabel);
	}

	private StatisticsEvent(Map<String, String> params) {
		this.params = params;
	}

	public static StatisticsEventBuilder builder() {
		return new StatisticsEventBuilder();
	}

	@Override
	public Map<String, String> getParams() {
		return params;
	}

	public static class StatisticsEventBuilder {

		private final Map<String, String> params;

		public StatisticsEventBuilder() {
			params = Maps.newHashMapWithExpectedSize(4);
			params.put("t", TYPE);
		}

		public StatisticsEventBuilder withCategory(String category) {
			params.put("ec", category);
			return this;
		}

		public StatisticsEventBuilder withAction(String action) {
			params.put("ea", action);
			return this;
		}

		public StatisticsEventBuilder withLabel(String label) {
			params.put("el", label);
			return this;
		}

		public StatisticsEvent build() {
			return new StatisticsEvent(params);
		}
	}
}
