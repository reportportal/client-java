/*
 * Copyright 2025 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.ta.reportportal.ws.model.launch;

import com.epam.reportportal.utils.serialize.TimeDeserializer;
import com.epam.reportportal.utils.serialize.TimeSerializer;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributeResource;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Set;

@JsonInclude(Include.NON_NULL)
public class MergeLaunchesRQ {

	@JsonProperty(value = "name", required = true)
	private String name;

	@JsonProperty(value = "description")
	private String description;

	@JsonProperty("attributes")
	private Set<ItemAttributeResource> attributes;

	@JsonProperty(value = "startTime")
	@JsonSerialize(using = TimeSerializer.class)
	@JsonDeserialize(using = TimeDeserializer.class)
	private Comparable<? extends Comparable<?>> startTime;

	@JsonProperty("mode")
	private Mode mode;

	@JsonProperty(value = "launches", required = true)
	private Set<Long> launches;

	@JsonProperty(value = "endTime")
	@JsonSerialize(using = TimeSerializer.class)
	@JsonDeserialize(using = TimeDeserializer.class)
	private Comparable<? extends Comparable<?>> endTime;

	@JsonProperty("mergeType")
	private String mergeStrategyType;

	@JsonProperty(value = "extendSuitesDescription", required = true)
	private boolean extendSuitesDescription;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Set<ItemAttributeResource> getAttributes() {
		return attributes;
	}

	public void setAttributes(Set<ItemAttributeResource> attributes) {
		this.attributes = attributes;
	}

	public Comparable<? extends Comparable<?>> getStartTime() {
		return startTime;
	}

	public void setStartTime(Comparable<? extends Comparable<?>> startTime) {
		this.startTime = startTime;
	}

	public Mode getMode() {
		return mode;
	}

	public void setMode(Mode mode) {
		this.mode = mode;
	}

	public Set<Long> getLaunches() {
		return launches;
	}

	public void setLaunches(Set<Long> launches) {
		this.launches = launches;
	}

	public Comparable<? extends Comparable<?>> getEndTime() {
		return endTime;
	}

	public void setEndTime(Comparable<? extends Comparable<?>> endTime) {
		this.endTime = endTime;
	}

	public String getMergeStrategyType() {
		return mergeStrategyType;
	}

	public void setMergeStrategyType(String mergeStrategyType) {
		this.mergeStrategyType = mergeStrategyType;
	}

	public boolean isExtendSuitesDescription() {
		return extendSuitesDescription;
	}

	public void setExtendSuitesDescription(boolean extendSuitesDescription) {
		this.extendSuitesDescription = extendSuitesDescription;
	}
} 