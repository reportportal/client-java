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
 * Finishes some entity execution in Report Portal<br>
 * May be Launch, TestSuite, Test, TestStep
 */
@JsonInclude(Include.NON_NULL)
public class FinishExecutionRQ {

	@JsonProperty(value = "endTime", required = true)
	@JsonAlias({ "endTime", "end_time" })
	private Comparable<?> endTime;

	@JsonProperty(value = "status")
	private String status;

	@JsonProperty(value = "description")
	private String description;

	@JsonProperty
	@JsonAlias({ "attributes", "tags" })
	private Set<ItemAttributesRQ> attributes;

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

	public Comparable<?> getEndTime() {
		return endTime;
	}

	public void setEndTime(Comparable<?> endTime) {
		this.endTime = endTime;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
} 