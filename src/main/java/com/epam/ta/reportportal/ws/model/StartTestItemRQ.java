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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(Include.NON_NULL)
public class StartTestItemRQ extends StartRQ {

	@JsonProperty(value = "codeRef")
	private String codeRef;

	@JsonProperty(value = "parameters")
	private List<ParameterResource> parameters;

	@JsonProperty(value = "uniqueId")
	private String uniqueId;

	@JsonProperty(value = "testCaseId")
	private String testCaseId;

	@JsonAlias({ "launchUuid", "launch_id" })
	private String launchUuid;

	@JsonProperty(value = "type", required = true)
	private String type;

	@JsonProperty(value = "retry")
	private Boolean retry;

	@JsonProperty(value = "hasStats")
	private boolean hasStats = true;

	@JsonProperty(value = "retryOf")
	private String retryOf;

	public String getCodeRef() {
		return codeRef;
	}

	public void setCodeRef(String codeRef) {
		this.codeRef = codeRef;
	}

	public Boolean isRetry() {
		return retry;
	}

	public void setRetry(Boolean retry) {
		this.retry = retry;
	}

	public boolean isHasStats() {
		return hasStats;
	}

	public void setHasStats(boolean hasStats) {
		this.hasStats = hasStats;
	}

	public String getRetryOf() {
		return retryOf;
	}

	public void setRetryOf(String retryOf) {
		this.retryOf = retryOf;
	}

	public String getLaunchUuid() {
		return launchUuid;
	}

	public void setLaunchUuid(String launchUuid) {
		this.launchUuid = launchUuid;
	}

	public List<ParameterResource> getParameters() {
		return parameters;
	}

	public void setParameters(List<ParameterResource> parameters) {
		this.parameters = parameters;
	}

	public String getUniqueId() {
		return uniqueId;
	}

	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}

	public String getTestCaseId() {
		return testCaseId;
	}

	public void setTestCaseId(String testCaseId) {
		this.testCaseId = testCaseId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
} 