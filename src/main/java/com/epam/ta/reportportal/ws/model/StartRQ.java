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

package com.epam.ta.reportportal.ws.model;

import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

/**
 * Base entity for start requests
 */
@JsonInclude(Include.NON_NULL)
public class StartRQ {

	@JsonProperty(value = "name", required = true)
	protected String name;

	@JsonProperty(value = "description")
	private String description;

	@JsonProperty("attributes")
	@JsonAlias({ "attributes", "tags" })
	private Set<ItemAttributesRQ> attributes;

	@JsonProperty(required = true)
	@JsonAlias({ "startTime", "start_time" })
	private Comparable<?> startTime;

	@JsonProperty(value = "uuid")
	private String uuid;

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

	public Set<ItemAttributesRQ> getAttributes() {
		return attributes;
	}

	public void setAttributes(Set<ItemAttributesRQ> attributes) {
		this.attributes = attributes;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public Comparable<?> getStartTime() {
		return startTime;
	}

	public void setStartTime(Comparable<?> startTime) {
		this.startTime = startTime;
	}
} 