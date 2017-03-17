/*
 * Copyright 2016 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/client-java-core
 *
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.epam.reportportal.listeners;

import static com.epam.reportportal.utils.properties.ListenerProperty.*;

import java.util.Properties;
import java.util.Set;

import com.epam.reportportal.utils.TagsParser;
import com.epam.ta.reportportal.ws.model.launch.Mode;

/**
 * Report portal listeners parameters
 * 
 */
public class ListenerParameters {

	private String description;
	private String uuid;
	private String baseUrl;
	private String projectName;
	private String launchName;
	private Mode launchRunningMode;
	private Set<String> tags;
	private Boolean enable;
	private Boolean isSkippedAnIssue;

	public ListenerParameters() {

	}

	public ListenerParameters(Properties properties) {
		if (properties != null) {
			this.description = properties.getProperty(DESCRIPTION.getPropertyName());
			this.uuid = properties.getProperty(UUID.getPropertyName());
			this.baseUrl = properties.getProperty(BASE_URL.getPropertyName());
			this.projectName = properties.getProperty(PROJECT_NAME.getPropertyName());
			this.launchName = properties.getProperty(LAUNCH_NAME.getPropertyName());
			this.tags = TagsParser.parseAsSet(properties.getProperty(LAUNCH_TAGS.getPropertyName()));
			this.launchRunningMode = ListenersUtils.getLaunchMode(properties.getProperty(MODE.getPropertyName()));
			this.enable = ListenersUtils.getEnable(properties.getProperty(ENABLE.getPropertyName()));
			this.isSkippedAnIssue = ListenersUtils.getEnable(properties.getProperty(SKIPPED_AS_ISSUE.getPropertyName()));
		}
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public String getLaunchName() {
		return launchName;
	}

	public void setLaunchName(String launchName) {
		this.launchName = launchName;
	}

	public Set<String> getTags() {
		return tags;
	}

	public Mode getMode() {
		return launchRunningMode;
	}

	public Boolean getEnable() {
		return enable;
	}

	public Boolean getIsSkippedAnIssue() {
		return isSkippedAnIssue;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return "ListenerParameters [description=" + description + ", uuid=" + uuid + ", baseUrl=" + baseUrl + ", projectName=" + projectName
				+ ", launchName=" + launchName + ", launchRunningMode=" + launchRunningMode + ", tags=" + tags + ", enable=" + enable
				+ ", isSkippedAnIssue=" + isSkippedAnIssue + "]";
	}
}
