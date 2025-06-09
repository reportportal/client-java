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

import com.epam.ta.reportportal.ws.model.attribute.ItemAttributeResource;
import com.epam.ta.reportportal.ws.model.issue.Issue;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Set;

/**
 * JSON Representation of Report Portal domain object
 */
@JsonInclude(Include.NON_NULL)
public class TestItemResource {

	@JsonProperty(value = "id")
	private Long itemId;

	@JsonProperty(value = "uuid")
	private String uuid;

	@JsonProperty(value = "name")
	private String name;

	@JsonProperty(value = "codeRef")
	private String codeRef;

	@JsonProperty(value = "description")
	private String description;

	@JsonProperty(value = "parameters")
	private List<ParameterResource> parameters;

	@JsonProperty(value = "attributes")
	private Set<ItemAttributeResource> attributes;

	@JsonProperty(value = "type")
	private String type;

	@JsonProperty(value = "startTime")
	private Comparable<?> startTime;

	@JsonProperty(value = "endTime")
	private Comparable<?> endTime;

	@JsonProperty(value = "status")
	private String status;

	@JsonProperty(value = "statistics")
	private Object statisticsResource;

	@JsonProperty(value = "parent")
	private Long parent;

	@JsonProperty(value = "pathNames")
	private Object pathNames;

	@JsonProperty(value = "launchStatus")
	private String launchStatus;

	@JsonProperty(value = "issue")
	private Issue issue;

	@JsonProperty(value = "hasChildren")
	private boolean hasChildren;

	@JsonProperty(value = "hasStats")
	private boolean hasStats;

	@JsonProperty(value = "launchId")
	private Long launchId;

	@JsonProperty(value = "uniqueId")
	private String uniqueId;

	@JsonProperty(value = "testCaseId")
	private String testCaseId;

	@JsonProperty(value = "testCaseHash")
	private Integer testCaseHash;

	@JsonProperty(value = "patternTemplates")
	private Set<String> patternTemplates;

	@JsonProperty(value = "retries")
	private List<TestItemResource> retries;

	@JsonProperty(value = "path")
	private String path;

	public List<TestItemResource> getRetries() {
		return retries;
	}

	public void setRetries(List<TestItemResource> retries) {
		this.retries = retries;
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

	public Long getItemId() {
		return itemId;
	}

	public void setItemId(Long itemId) {
		this.itemId = itemId;
	}

	public Issue getIssue() {
		return issue;
	}

	public void setIssue(Issue issue) {
		this.issue = issue;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCodeRef() {
		return codeRef;
	}

	public void setCodeRef(String codeRef) {
		this.codeRef = codeRef;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<ParameterResource> getParameters() {
		return parameters;
	}

	public void setParameters(List<ParameterResource> parameters) {
		this.parameters = parameters;
	}

	public Set<ItemAttributeResource> getAttributes() {
		return attributes;
	}

	public void setAttributes(Set<ItemAttributeResource> attributes) {
		this.attributes = attributes;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
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

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Long getParent() {
		return parent;
	}

	public void setParent(Long parent) {
		this.parent = parent;
	}

	public Object getPathNames() {
		return pathNames;
	}

	public void setPathNames(Object pathNames) {
		this.pathNames = pathNames;
	}

	public void setLaunchStatus(String value) {
		this.launchStatus = value;
	}

	public String getLaunchStatus() {
		return launchStatus;
	}

	public Object getStatisticsResource() {
		return statisticsResource;
	}

	public void setStatisticsResource(Object statisticsResource) {
		this.statisticsResource = statisticsResource;
	}

	public boolean isHasChildren() {
		return hasChildren;
	}

	public void setHasChildren(boolean hasChildren) {
		this.hasChildren = hasChildren;
	}

	public boolean isHasStats() {
		return hasStats;
	}

	public void setHasStats(boolean hasStats) {
		this.hasStats = hasStats;
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

	public Integer getTestCaseHash() {
		return testCaseHash;
	}

	public void setTestCaseHash(Integer testCaseHash) {
		this.testCaseHash = testCaseHash;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Set<String> getPatternTemplates() {
		return patternTemplates;
	}

	public void setPatternTemplates(Set<String> patternTemplates) {
		this.patternTemplates = patternTemplates;
	}
} 