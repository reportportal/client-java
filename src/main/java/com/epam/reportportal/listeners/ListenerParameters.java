/*
 * Copyright 2019 EPAM Systems
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
import com.epam.reportportal.utils.AttributeParser;
import com.epam.reportportal.utils.properties.PropertiesLoader;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import com.epam.ta.reportportal.ws.model.launch.Mode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;

import java.util.Set;

import static com.epam.reportportal.utils.properties.ListenerProperty.*;

/**
 * Report portal listeners parameters
 */
public class ListenerParameters {

	private static final int DEFAULT_REPORTING_TIMEOUT = 5 * 60;
	private static final int DEFAULT_IO_POOL_SIZE = 100;
	private static final int DEFAULT_MAX_CONNECTIONS_PER_ROUTE = 50;
	private static final int DEFAULT_MAX_CONNECTIONS_TOTAL = 100;
	private static final boolean DEFAULT_ENABLE = true;
	private static final boolean DEFAULT_SKIP_ISSUE = true;
	private static final boolean DEFAULT_CONVERT_IMAGE = false;
	private static final boolean DEFAULT_RETURN = false;
	private static final boolean DEFAULT_ASYNC_REPORTING = false;

	private String description;
	private String uuid;
	private String baseUrl;
	private String projectName;
	private String launchName;
	private Mode launchRunningMode;
	private Set<ItemAttributesRQ> attributes;
	private Boolean enable;
	private Boolean isSkippedAnIssue;
	private Integer batchLogsSize;
	private boolean convertImage;
	private Integer reportingTimeout;
	private String keystore;
	private String keystorePassword;
	private boolean rerun;
	private String rerunOf;
	private boolean asyncReporting;
	private Integer ioPoolSize;
	private Integer maxConnectionsPerRoute;
	private Integer maxConnectionsTotal;

	public ListenerParameters() {

		this.isSkippedAnIssue = DEFAULT_SKIP_ISSUE;

		this.batchLogsSize = LoggingContext.DEFAULT_BUFFER_SIZE;
		this.convertImage = DEFAULT_CONVERT_IMAGE;
		this.reportingTimeout = DEFAULT_REPORTING_TIMEOUT;

		this.attributes = Sets.newHashSet();

		this.rerun = DEFAULT_RETURN;

		this.asyncReporting = DEFAULT_ASYNC_REPORTING;

		this.ioPoolSize = DEFAULT_IO_POOL_SIZE;
		this.maxConnectionsPerRoute = DEFAULT_MAX_CONNECTIONS_PER_ROUTE;
		this.maxConnectionsTotal = DEFAULT_MAX_CONNECTIONS_TOTAL;
	}

	public ListenerParameters(PropertiesLoader properties) {
		this.description = properties.getProperty(DESCRIPTION);
		this.uuid = properties.getProperty(UUID);
		this.baseUrl = properties.getProperty(BASE_URL);
		this.projectName = properties.getProperty(PROJECT_NAME);
		this.launchName = properties.getProperty(LAUNCH_NAME);
		this.attributes = AttributeParser.parseAsSet(properties.getProperty(LAUNCH_ATTRIBUTES));
		this.launchRunningMode = parseLaunchMode(properties.getProperty(MODE));
		this.enable = properties.getPropertyAsBoolean(ENABLE, DEFAULT_ENABLE);
		this.isSkippedAnIssue = properties.getPropertyAsBoolean(SKIPPED_AS_ISSUE, DEFAULT_SKIP_ISSUE);

		this.batchLogsSize = properties.getPropertyAsInt(BATCH_SIZE_LOGS, LoggingContext.DEFAULT_BUFFER_SIZE);
		this.convertImage = properties.getPropertyAsBoolean(IS_CONVERT_IMAGE, DEFAULT_CONVERT_IMAGE);
		this.reportingTimeout = properties.getPropertyAsInt(REPORTING_TIMEOUT, DEFAULT_REPORTING_TIMEOUT);

		this.keystore = properties.getProperty(KEYSTORE_RESOURCE);
		this.keystorePassword = properties.getProperty(KEYSTORE_PASSWORD);
		this.rerun = properties.getPropertyAsBoolean(RERUN, DEFAULT_RETURN);
		this.rerunOf = properties.getProperty(RERUN_OF);

		this.asyncReporting = properties.getPropertyAsBoolean(ASYNC_REPORTING, DEFAULT_ASYNC_REPORTING);

		this.ioPoolSize = properties.getPropertyAsInt(IO_POOL_SIZE, DEFAULT_IO_POOL_SIZE);
		this.maxConnectionsPerRoute = properties.getPropertyAsInt(MAX_CONNECTIONS_PER_ROUTE, DEFAULT_MAX_CONNECTIONS_PER_ROUTE);
		this.maxConnectionsTotal = properties.getPropertyAsInt(MAX_CONNECTIONS_TOTAL, DEFAULT_MAX_CONNECTIONS_TOTAL);
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

	public Set<ItemAttributesRQ> getAttributes() {
		return attributes;
	}

	public void setAttributes(Set<ItemAttributesRQ> attributes) {
		this.attributes = attributes;
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

	public boolean isAsyncReporting() {
		return asyncReporting;
	}

	public void setAsyncReporting(boolean asyncReporting) {
		this.asyncReporting = asyncReporting;
	}

	public void setRerun(boolean rerun) {
		this.rerun = rerun;
	}

	public String getRerunOf() {
		return rerunOf;
	}

	public void setRerunOf(String rerunOf) {
		this.rerunOf = rerunOf;
	}

	public Integer getIoPoolSize() {
		return ioPoolSize;
	}

	public void setIoPoolSize(Integer ioPoolSize) {
		this.ioPoolSize = ioPoolSize;
	}

	public Integer getMaxConnectionsPerRoute() {
		return maxConnectionsPerRoute;
	}

	public void setMaxConnectionsPerRoute(Integer maxConnectionsPerRoute) {
		this.maxConnectionsPerRoute = maxConnectionsPerRoute;
	}

	public Integer getMaxConnectionsTotal() {
		return maxConnectionsTotal;
	}

	public void setMaxConnectionsTotal(Integer maxConnectionsTotal) {
		this.maxConnectionsTotal = maxConnectionsTotal;
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
		sb.append(", attributes=").append(attributes);
		sb.append(", enable=").append(enable);
		sb.append(", isSkippedAnIssue=").append(isSkippedAnIssue);
		sb.append(", batchLogsSize=").append(batchLogsSize);
		sb.append(", convertImage=").append(convertImage);
		sb.append(", reportingTimeout=").append(reportingTimeout);
		sb.append(", keystore='").append(keystore).append('\'');
		sb.append(", keystorePassword='").append(keystorePassword).append('\'');
		sb.append(", rerun=").append(rerun);
		sb.append(", rerunOf='").append(rerunOf).append('\'');
		sb.append(", asyncReporting=").append(asyncReporting);
		sb.append(", ioPoolSize=").append(ioPoolSize);
		sb.append(", maxConnectionsPerRoute=").append(maxConnectionsPerRoute);
		sb.append(", maxConnectionsTotal=").append(maxConnectionsTotal);
		sb.append('}');
		return sb.toString();
	}
}
