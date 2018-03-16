/*
 * Copyright (C) 2018 EPAM Systems
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
package com.epam.reportportal.listeners;

import com.epam.reportportal.service.LoggingContext;
import com.epam.reportportal.utils.TagsParser;
import com.epam.reportportal.utils.properties.PropertiesLoader;
import com.epam.ta.reportportal.ws.model.launch.Mode;
import com.google.common.annotations.VisibleForTesting;

import java.util.Set;

import static com.epam.reportportal.utils.properties.ListenerProperty.*;

/**
 * Report portal listeners parameters
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
	private Integer batchLogsSize;
	private boolean convertImage;
	private Integer reportingTimeout;
	private String keystore;
	private String keystorePassword;
	private boolean rerun;

	public ListenerParameters() {

	}

	public ListenerParameters(PropertiesLoader properties) {
		this.description = properties.getProperty(DESCRIPTION);
		this.uuid = properties.getProperty(UUID);
		this.baseUrl = properties.getProperty(BASE_URL);
		this.projectName = properties.getProperty(PROJECT_NAME);
		this.launchName = properties.getProperty(LAUNCH_NAME);
		this.tags = TagsParser.parseAsSet(properties.getProperty(LAUNCH_TAGS));
		this.launchRunningMode = parseLaunchMode(properties.getProperty(MODE));
		this.enable = properties.getPropertyAsBoolean(ENABLE, true);
		this.isSkippedAnIssue = properties.getPropertyAsBoolean(SKIPPED_AS_ISSUE, true);

		this.batchLogsSize = properties.getPropertyAsInt(BATCH_SIZE_LOGS, LoggingContext.DEFAULT_BUFFER_SIZE);
		this.convertImage = properties.getPropertyAsBoolean(IS_CONVERT_IMAGE, false);
		this.reportingTimeout = properties.getPropertyAsInt(REPORTING_TIMEOUT, 5 * 60);

		this.keystore = properties.getProperty(KEYSTORE_RESOURCE);
		this.keystorePassword = properties.getProperty(KEYSTORE_PASSWORD);
		this.rerun = properties.getPropertyAsBoolean(RERUN, false);
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
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

	public Mode getLaunchRunningMode() {
		return launchRunningMode;
	}

	public void setLaunchRunningMode(Mode launchRunningMode) {
		this.launchRunningMode = launchRunningMode;
	}

	public Set<String> getTags() {
		return tags;
	}

	public void setTags(Set<String> tags) {
		this.tags = tags;
	}

	public Boolean getEnable() {
		return enable;
	}

	public void setEnable(Boolean enable) {
		this.enable = enable;
	}

	public Boolean getSkippedAnIssue() {
		return isSkippedAnIssue;
	}

	public void setSkippedAnIssue(Boolean skippedAnIssue) {
		isSkippedAnIssue = skippedAnIssue;
	}

	public Integer getBatchLogsSize() {
		return batchLogsSize;
	}

	public void setBatchLogsSize(Integer batchLogsSize) {
		this.batchLogsSize = batchLogsSize;
	}

	public boolean isConvertImage() {
		return convertImage;
	}

	public void setConvertImage(boolean convertImage) {
		this.convertImage = convertImage;
	}

	public Integer getReportingTimeout() {
		return reportingTimeout;
	}

	public String getKeystore() {
		return keystore;
	}

	public void setKeystore(String keystore) {
		this.keystore = keystore;
	}

	public void setReportingTimeout(Integer reportingTimeout) {
		this.reportingTimeout = reportingTimeout;
	}

	public String getKeystorePassword() {
		return keystorePassword;
	}

	public void setKeystorePassword(String keystorePassword) {
		this.keystorePassword = keystorePassword;
	}

	public boolean isRerun() {
		return rerun;
	}

	public void setRerun(boolean rerun) {
		this.rerun = rerun;
	}

	@VisibleForTesting
	Mode parseLaunchMode(String mode) {
		return Mode.isExists(mode) ? Mode.valueOf(mode.toUpperCase()) : Mode.DEFAULT;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("ListenerParameters{");
		sb.append("description='").append(description).append('\'');
		sb.append(", uuid='").append(uuid).append('\'');
		sb.append(", baseUrl='").append(baseUrl).append('\'');
		sb.append(", projectName='").append(projectName).append('\'');
		sb.append(", launchName='").append(launchName).append('\'');
		sb.append(", launchRunningMode=").append(launchRunningMode);
		sb.append(", tags=").append(tags);
		sb.append(", enable=").append(enable);
		sb.append(", isSkippedAnIssue=").append(isSkippedAnIssue);
		sb.append(", batchLogsSize=").append(batchLogsSize);
		sb.append(", convertImage=").append(convertImage);
		sb.append('}');
		return sb.toString();
	}
}
