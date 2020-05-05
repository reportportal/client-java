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

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.epam.reportportal.utils.properties.ListenerProperty.*;
import static java.util.Optional.ofNullable;

/**
 * Report portal listeners parameters
 */
public class ListenerParameters implements Cloneable {

	private static final int DEFAULT_REPORTING_TIMEOUT = 5 * 60;
	private static final int DEFAULT_IO_POOL_SIZE = 100;
	private static final int DEFAULT_MAX_CONNECTIONS_PER_ROUTE = 50;
	private static final int DEFAULT_MAX_CONNECTIONS_TOTAL = 100;
	private static final boolean DEFAULT_ENABLE = true;
	private static final boolean DEFAULT_SKIP_ISSUE = true;
	private static final boolean DEFAULT_CONVERT_IMAGE = false;
	private static final boolean DEFAULT_RETURN = false;
	private static final boolean DEFAULT_ASYNC_REPORTING = false;
	private static final boolean DEFAULT_CALLBACK_REPORTING_ENABLED = false;
	private static final int DEFAULT_MAX_CONNECTION_TIME_TO_LIVE_MS = 29900;
	private static final int DEFAULT_MAX_CONNECTION_IDLE_TIME_MS = 5 * 1000;
	private static final int DEFAULT_TRANSFER_RETRY_COUNT = 5;
	private static final boolean DEFAULT_CLIENT_JOIN_MODE = true;
	private static final String DEFAULT_LOCK_FILE_NAME = "reportportal.lock";
	private static final String DEFAULT_SYNC_FILE_NAME = "reportportal.sync";
	private static final long DEFAULT_FILE_WAIT_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(1);

	private String description;
	private String apiKey;
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
	private boolean callbackReportingEnabled;
	private Integer ioPoolSize;
	private Integer maxConnectionsPerRoute;
	private Integer maxConnectionsTotal;

	private Integer maxConnectionTtlMs;
	private Integer maxConnectionIdleTtlMs;
	private Integer transferRetries;

	private boolean clientJoin;
	private String lockFileName;
	private String syncFileName;
	private long fileWaitTimeout;

	public ListenerParameters() {

		this.isSkippedAnIssue = DEFAULT_SKIP_ISSUE;

		this.batchLogsSize = LoggingContext.DEFAULT_BUFFER_SIZE;
		this.convertImage = DEFAULT_CONVERT_IMAGE;
		this.reportingTimeout = DEFAULT_REPORTING_TIMEOUT;

		this.attributes = Sets.newHashSet();

		this.rerun = DEFAULT_RETURN;

		this.asyncReporting = DEFAULT_ASYNC_REPORTING;
		this.callbackReportingEnabled = DEFAULT_CALLBACK_REPORTING_ENABLED;

		this.ioPoolSize = DEFAULT_IO_POOL_SIZE;
		this.maxConnectionsPerRoute = DEFAULT_MAX_CONNECTIONS_PER_ROUTE;
		this.maxConnectionsTotal = DEFAULT_MAX_CONNECTIONS_TOTAL;

		this.maxConnectionTtlMs = DEFAULT_MAX_CONNECTION_TIME_TO_LIVE_MS;
		this.maxConnectionIdleTtlMs = DEFAULT_MAX_CONNECTION_IDLE_TIME_MS;
		this.transferRetries = DEFAULT_TRANSFER_RETRY_COUNT;

		this.clientJoin = DEFAULT_CLIENT_JOIN_MODE;
		this.lockFileName = DEFAULT_LOCK_FILE_NAME;
		this.syncFileName = DEFAULT_SYNC_FILE_NAME;
		this.fileWaitTimeout = DEFAULT_FILE_WAIT_TIMEOUT_MS;
	}

	public ListenerParameters(PropertiesLoader properties) {
		this.description = properties.getProperty(DESCRIPTION);
		this.apiKey = ofNullable(properties.getProperty(API_KEY, properties.getProperty(UUID))).map(String::trim).orElse(null);
		this.baseUrl = properties.getProperty(BASE_URL) != null ? properties.getProperty(BASE_URL).trim() : null;
		this.projectName = properties.getProperty(PROJECT_NAME) != null ? properties.getProperty(PROJECT_NAME).trim() : null;
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
		this.callbackReportingEnabled = properties.getPropertyAsBoolean(CALLBACK_REPORTING_ENABLED, DEFAULT_CALLBACK_REPORTING_ENABLED);

		this.ioPoolSize = properties.getPropertyAsInt(IO_POOL_SIZE, DEFAULT_IO_POOL_SIZE);
		this.maxConnectionsPerRoute = properties.getPropertyAsInt(MAX_CONNECTIONS_PER_ROUTE, DEFAULT_MAX_CONNECTIONS_PER_ROUTE);
		this.maxConnectionsTotal = properties.getPropertyAsInt(MAX_CONNECTIONS_TOTAL, DEFAULT_MAX_CONNECTIONS_TOTAL);

		this.maxConnectionTtlMs = properties.getPropertyAsInt(MAX_CONNECTION_TIME_TO_LIVE, DEFAULT_MAX_CONNECTION_TIME_TO_LIVE_MS);
		this.maxConnectionIdleTtlMs = properties.getPropertyAsInt(MAX_CONNECTION_IDLE_TIME, DEFAULT_MAX_CONNECTION_IDLE_TIME_MS);
		this.transferRetries = properties.getPropertyAsInt(MAX_TRANSFER_RETRY_COUNT, DEFAULT_TRANSFER_RETRY_COUNT);

		this.clientJoin = properties.getPropertyAsBoolean(CLIENT_JOIN_MODE, DEFAULT_CLIENT_JOIN_MODE);
		this.lockFileName = properties.getProperty(LOCK_FILE_NAME, DEFAULT_LOCK_FILE_NAME);
		this.syncFileName = properties.getProperty(SYNC_FILE_NAME, DEFAULT_SYNC_FILE_NAME);
		this.fileWaitTimeout = properties.getPropertyAsInt(FILE_WAIT_TIMEOUT_MS, (int) DEFAULT_FILE_WAIT_TIMEOUT_MS);
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
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

	public boolean isCallbackReportingEnabled() {
		return callbackReportingEnabled;
	}

	public void setCallbackReportingEnabled(boolean callbackReportingEnabled) {
		this.callbackReportingEnabled = callbackReportingEnabled;
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

	public Integer getMaxConnectionTtlMs() {
		return maxConnectionTtlMs;
	}

	public void setMaxConnectionTtlMs(Integer maxConnectionTtlMs) {
		this.maxConnectionTtlMs = maxConnectionTtlMs;
	}

	public Integer getMaxConnectionIdleTtlMs() {
		return maxConnectionIdleTtlMs;
	}

	public void setMaxConnectionIdleTtlMs(Integer maxConnectionIdleTtlMs) {
		this.maxConnectionIdleTtlMs = maxConnectionIdleTtlMs;
	}

	public Integer getTransferRetries() {
		return transferRetries;
	}

	public void setTransferRetries(Integer transferRetries) {
		this.transferRetries = transferRetries;
	}

	public boolean getClientJoin() {
		return clientJoin;
	}

	public void setClientJoin(boolean mode) {
		this.clientJoin = mode;
	}

	public String getLockFileName() {
		return lockFileName;
	}

	public void setLockFileName(String fileName) {
		this.lockFileName = fileName;
	}

	public String getSyncFileName() {
		return syncFileName;
	}

	public void setSyncFileName(String fileName) {
		this.syncFileName = fileName;
	}

	public long getFileWaitTimeout() {
		return fileWaitTimeout;
	}

	public void setFileWaitTimeout(long timeout) {
		this.fileWaitTimeout = timeout;
	}

	@VisibleForTesting
	Mode parseLaunchMode(String mode) {
		return Mode.isExists(mode) ? Mode.valueOf(mode.toUpperCase()) : Mode.DEFAULT;
	}

	@Override
	public ListenerParameters clone() {
		final ListenerParameters clone = new ListenerParameters();
		Arrays.stream(getClass().getDeclaredFields()).forEach(f -> {
			if (Modifier.isFinal(f.getModifiers())) {
				return; // skip constants
			}
			try {
				f.set(clone, f.get(this));
			} catch (IllegalAccessException e) {
				// actually, we are calling field set from within the class, so that should not happen, unless we try to set final fields
				throw new IllegalStateException(e);
			}
		});
		return clone;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("ListenerParameters{");
		sb.append("description='").append(description).append('\'');
		sb.append(", apiKey='").append(apiKey).append('\'');
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
		sb.append(", callbackReportingEnabled=").append(callbackReportingEnabled);
		sb.append(", maxConnectionsPerRoute=").append(maxConnectionsPerRoute);
		sb.append(", maxConnectionsTotal=").append(maxConnectionsTotal);
		sb.append(", maxConnectionTtlMs=").append(maxConnectionTtlMs);
		sb.append(", maxConnectionIdleTtlMs=").append(maxConnectionIdleTtlMs);
		sb.append(", transferRetries=").append(transferRetries);
		sb.append(", clientJoin=").append(clientJoin);
		sb.append(", lockFileName=").append(lockFileName);
		sb.append(", syncFileName=").append(syncFileName);
		sb.append(", fileWaitTimeout=").append(fileWaitTimeout);
		sb.append('}');
		return sb.toString();
	}
}
