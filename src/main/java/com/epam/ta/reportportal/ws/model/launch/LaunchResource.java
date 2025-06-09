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

import com.epam.ta.reportportal.ws.model.attribute.ItemAttributeResource;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * JSON Representation of Report Portal's Launch domain object
 */
@JsonInclude(Include.NON_NULL)
public class LaunchResource {

	@JsonProperty(value = "id", required = true)
	private Long launchId;

	@JsonProperty(value = "uuid", required = true)
	private String uuid;

	@JsonProperty(value = "name", required = true)
	private String name;

	@JsonProperty(value = "number", required = true)
	private Long number;

	@JsonProperty(value = "description")
	private String description;

	@JsonProperty(value = "startTime", required = true)
	private Comparable<?> startTime;

	@JsonProperty(value = "endTime")
	private Comparable<?> endTime;

	@JsonProperty(value = "lastModified")
	private Object lastModified;

	@JsonProperty(value = "status", required = true)
	private String status;

	@JsonProperty(value = "statistics")
	private Object statisticsResource;

	@JsonProperty(value = "attributes")
	private Set<ItemAttributeResource> attributes;

	@JsonProperty(value = "mode")
	private Mode mode;

	@JsonProperty(value = "analysing")
	private Set<String> analyzers = new LinkedHashSet<>();

	@JsonProperty(value = "approximateDuration")
	private double approximateDuration;

	@JsonProperty(value = "hasRetries")
	private boolean hasRetries;

	private boolean rerun;

	public double getApproximateDuration() {
		return approximateDuration;
	}

	public void setApproximateDuration(double approximateDuration) {
		this.approximateDuration = approximateDuration;
	}

	public Long getLaunchId() {
		return launchId;
	}

	public void setLaunchId(Long launchId) {
		this.launchId = launchId;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getNumber() {
		return number;
	}

	public void setNumber(Long number) {
		this.number = number;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Comparable<?> getStartTime() {
		return startTime;
	}

	public void setStartTime(Comparable<?> startTime) {
		this.startTime = startTime;
	}

	public Comparable<?> getEndTime() {
		return endTime;
	}

	public void setEndTime(Comparable<?> endTime) {
		this.endTime = endTime;
	}

	public Object getLastModified() {
		return lastModified;
	}

	public void setLastModified(Object lastModified) {
		this.lastModified = lastModified;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Object getStatisticsResource() {
		return statisticsResource;
	}

	public void setStatisticsResource(Object statisticsResource) {
		this.statisticsResource = statisticsResource;
	}

	public Set<ItemAttributeResource> getAttributes() {
		return attributes;
	}

	public void setAttributes(Set<ItemAttributeResource> attributes) {
		this.attributes = attributes;
	}

	public Mode getMode() {
		return mode;
	}

	public void setMode(Mode mode) {
		this.mode = mode;
	}

	public Set<String> getAnalyzers() {
		return analyzers;
	}

	public void setAnalyzers(Set<String> analyzers) {
		this.analyzers = analyzers;
	}

	public boolean isHasRetries() {
		return hasRetries;
	}

	public boolean getHasRetries() {
		return hasRetries;
	}

	public void setHasRetries(boolean hasRetries) {
		this.hasRetries = hasRetries;
	}

	public boolean isRerun() {
		return rerun;
	}

	public void setRerun(boolean rerun) {
		this.rerun = rerun;
	}
} 