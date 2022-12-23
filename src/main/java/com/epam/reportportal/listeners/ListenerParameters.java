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
import com.epam.reportportal.service.launch.lock.LaunchIdLockMode;
import com.epam.reportportal.utils.AttributeParser;
import com.epam.reportportal.utils.properties.ListenerProperty;
import com.epam.reportportal.utils.properties.PropertiesLoader;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import com.epam.ta.reportportal.ws.model.launch.Mode;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.epam.reportportal.utils.properties.ListenerProperty.*;
import static java.util.Optional.ofNullable;

/**
 * Report portal listeners parameters
 */
public class ListenerParameters implements Cloneable {

	private static final Logger LOGGER = LoggerFactory.getLogger(ListenerParameters.class);

	private static final String PROPERTY_WILL_BE_REMOVED = "Property '{}' will be removed in the next major version, please use '{}' instead";

	private static final int DEFAULT_REPORTING_TIMEOUT = 5 * 60;
	private static final int DEFAULT_IO_POOL_SIZE = 100;
	private static final boolean DEFAULT_ENABLE = true;
	private static final boolean DEFAULT_SKIP_ISSUE = true;
	private static final boolean DEFAULT_CONVERT_IMAGE = false;
	private static final boolean DEFAULT_RETURN = false;
	private static final boolean DEFAULT_ASYNC_REPORTING = false;
	private static final boolean DEFAULT_CALLBACK_REPORTING_ENABLED = false;
	private static final boolean DEFAULT_HTTP_LOGGING = false;
	private static final int DEFAULT_RX_BUFFER_SIZE = 128;

	private static final boolean DEFAULT_CLIENT_JOIN = true;
	private static final String DEFAULT_CLIENT_JOIN_MODE = "FILE";
	private static final String DEFAULT_LOCK_FILE_NAME = "reportportal.lock";
	private static final String DEFAULT_SYNC_FILE_NAME = "reportportal.sync";
	public static final long DEFAULT_FILE_WAIT_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(1);
	private static final long DEFAULT_CLIENT_JOIN_TIMEOUT = TimeUnit.MINUTES.toMillis(30);
	private static final String DEFAULT_CLIENT_JOIN_TIMEOUT_UNIT = "MILLISECONDS";
	private static final String DEFAULT_CLIENT_JOIN_LOCK_TIMEOUT_UNIT = DEFAULT_CLIENT_JOIN_TIMEOUT_UNIT;
	private static final int DEFAULT_CLIENT_JOIN_LOCK_PORT = 25464;

	private static final boolean DEFAULT_TRUNCATE = true;
	private static final int DEFAULT_TRUNCATE_ITEM_NAMES_LIMIT = 1024;
	private static final int DEFAULT_TRUNCATE_ATTRIBUTE_LIMIT = 128;
	private static final String DEFAULT_TRUNCATE_REPLACEMENT = "...";

	// Due to shortcoming of payload calculation mechanism this value is set to 65 million of bytes rather than 65 megabytes
	public static final long DEFAULT_BATCH_PAYLOAD_LIMIT = 65 * 1000 * 1000;

	private String description;
	private String apiKey;
	private String baseUrl;
	private String proxyUrl;
	private boolean httpLogging;
	private Duration httpCallTimeout;
	private Duration httpConnectTimeout;
	private Duration httpReadTimeout;
	private Duration httpWriteTimeout;
	private String projectName;
	private String launchName;
	private String launchUuid;
	private Mode launchRunningMode;
	private Set<ItemAttributesRQ> attributes;
	private Boolean enable;
	private Boolean isSkippedAnIssue;
	private Integer batchLogsSize;
	private Long batchPayloadLimit;
	private boolean convertImage;
	private Integer reportingTimeout;
	private String keystore;
	private String keystorePassword;
	private boolean rerun;
	private String rerunOf;
	private boolean asyncReporting;
	private boolean callbackReportingEnabled;
	private Integer ioPoolSize;

	private boolean clientJoin;
	private LaunchIdLockMode clientJoinMode;
	private String lockFileName;
	private String syncFileName;
	private long lockWaitTimeout;
	private long clientJoinTimeout;
	private int lockPortNumber;

	private int rxBufferSize;

	private boolean truncateFields;
	private int truncateItemNamesLimit;
	private String truncateReplacement;
	private int attributeLengthLimit;

	@Nonnull
	private static ChronoUnit toChronoUnit(@Nonnull TimeUnit t) {
		switch (t) {
			case NANOSECONDS:
				return ChronoUnit.NANOS;
			case MICROSECONDS:
				return ChronoUnit.MICROS;
			case MILLISECONDS:
				return ChronoUnit.MILLIS;
			case SECONDS:
				return ChronoUnit.SECONDS;
			case MINUTES:
				return ChronoUnit.MINUTES;
			case HOURS:
				return ChronoUnit.HOURS;
			case DAYS:
				return ChronoUnit.DAYS;
			default:
				throw new AssertionError();
		}
	}

	@Nullable
	private static Duration getDurationProperty(@Nonnull PropertiesLoader properties, @Nonnull ListenerProperty value,
			@Nonnull ListenerProperty unit) {
		return ofNullable(properties.getProperty(value)).map(Long::parseLong).map(t -> Duration.of(t,
				ofNullable(properties.getProperty(unit)).map(u -> toChronoUnit(TimeUnit.valueOf(u)))
						.orElse(ChronoUnit.MILLIS)
		)).orElse(null);
	}

	public ListenerParameters() {

		this.isSkippedAnIssue = DEFAULT_SKIP_ISSUE;

		this.batchLogsSize = LoggingContext.DEFAULT_LOG_BATCH_SIZE;
		this.batchPayloadLimit = DEFAULT_BATCH_PAYLOAD_LIMIT;
		this.convertImage = DEFAULT_CONVERT_IMAGE;
		this.reportingTimeout = DEFAULT_REPORTING_TIMEOUT;
		this.httpLogging = DEFAULT_HTTP_LOGGING;

		this.attributes = Collections.emptySet();

		this.rerun = DEFAULT_RETURN;

		this.asyncReporting = DEFAULT_ASYNC_REPORTING;
		this.callbackReportingEnabled = DEFAULT_CALLBACK_REPORTING_ENABLED;

		this.ioPoolSize = DEFAULT_IO_POOL_SIZE;

		this.clientJoin = DEFAULT_CLIENT_JOIN;
		this.clientJoinMode = LaunchIdLockMode.valueOf(DEFAULT_CLIENT_JOIN_MODE);
		this.clientJoinTimeout = DEFAULT_CLIENT_JOIN_TIMEOUT;
		this.lockFileName = DEFAULT_LOCK_FILE_NAME;
		this.syncFileName = DEFAULT_SYNC_FILE_NAME;
		this.lockWaitTimeout = DEFAULT_FILE_WAIT_TIMEOUT_MS;
		this.lockPortNumber = DEFAULT_CLIENT_JOIN_LOCK_PORT;

		this.rxBufferSize = DEFAULT_RX_BUFFER_SIZE;

		this.truncateFields = DEFAULT_TRUNCATE;
		this.truncateItemNamesLimit = DEFAULT_TRUNCATE_ITEM_NAMES_LIMIT;
		this.truncateReplacement = DEFAULT_TRUNCATE_REPLACEMENT;
		this.attributeLengthLimit = DEFAULT_TRUNCATE_ATTRIBUTE_LIMIT;
	}

	public ListenerParameters(PropertiesLoader properties) {
		this.description = properties.getProperty(DESCRIPTION);
		this.apiKey = ofNullable(properties.getProperty(API_KEY, properties.getProperty(UUID))).map(String::trim)
				.orElse(null);
		this.baseUrl = properties.getProperty(BASE_URL) != null ? properties.getProperty(BASE_URL).trim() : null;
		this.proxyUrl = properties.getProperty(HTTP_PROXY_URL);
		this.httpLogging = properties.getPropertyAsBoolean(HTTP_LOGGING, DEFAULT_HTTP_LOGGING);

		this.httpCallTimeout = getDurationProperty(properties, HTTP_CALL_TIMEOUT_VALUE, HTTP_CALL_TIMEOUT_UNIT);
		this.httpConnectTimeout = getDurationProperty(properties,
				HTTP_CONNECT_TIMEOUT_VALUE,
				HTTP_CONNECT_TIMEOUT_UNIT
		);
		this.httpReadTimeout = getDurationProperty(properties, HTTP_READ_TIMEOUT_VALUE, HTTP_READ_TIMEOUT_UNIT);
		this.httpWriteTimeout = getDurationProperty(properties, HTTP_WRITE_TIMEOUT_VALUE, HTTP_WRITE_TIMEOUT_UNIT);

		this.projectName =
				properties.getProperty(PROJECT_NAME) != null ? properties.getProperty(PROJECT_NAME).trim() : null;
		this.launchName = properties.getProperty(LAUNCH_NAME);
		this.launchUuid = properties.getProperty(LAUNCH_UUID);
		this.attributes = Collections.unmodifiableSet(AttributeParser.parseAsSet(properties.getProperty(
				LAUNCH_ATTRIBUTES)));
		this.launchRunningMode = parseLaunchMode(properties.getProperty(MODE));
		this.enable = properties.getPropertyAsBoolean(ENABLE, DEFAULT_ENABLE);
		this.isSkippedAnIssue = properties.getPropertyAsBoolean(SKIPPED_AS_ISSUE, DEFAULT_SKIP_ISSUE);

		this.batchLogsSize = properties.getPropertyAsInt(BATCH_SIZE_LOGS, LoggingContext.DEFAULT_LOG_BATCH_SIZE);
		this.batchPayloadLimit = properties.getPropertyAsLong(BATCH_PAYLOAD_LIMIT, DEFAULT_BATCH_PAYLOAD_LIMIT);
		this.convertImage = properties.getPropertyAsBoolean(IS_CONVERT_IMAGE, DEFAULT_CONVERT_IMAGE);
		this.reportingTimeout = properties.getPropertyAsInt(REPORTING_TIMEOUT, DEFAULT_REPORTING_TIMEOUT);

		this.keystore = properties.getProperty(KEYSTORE_RESOURCE);
		this.keystorePassword = properties.getProperty(KEYSTORE_PASSWORD);
		this.rerun = properties.getPropertyAsBoolean(RERUN, DEFAULT_RETURN);
		this.rerunOf = properties.getProperty(RERUN_OF);

		this.asyncReporting = properties.getPropertyAsBoolean(ASYNC_REPORTING, DEFAULT_ASYNC_REPORTING);
		this.callbackReportingEnabled = properties.getPropertyAsBoolean(CALLBACK_REPORTING_ENABLED,
				DEFAULT_CALLBACK_REPORTING_ENABLED
		);

		this.ioPoolSize = properties.getPropertyAsInt(IO_POOL_SIZE, DEFAULT_IO_POOL_SIZE);

		clientJoin = properties.getPropertyAsBoolean(CLIENT_JOIN_MODE, DEFAULT_CLIENT_JOIN);
		clientJoinMode = LaunchIdLockMode.valueOf(properties.getProperty(CLIENT_JOIN_MODE_VALUE,
				DEFAULT_CLIENT_JOIN_MODE
		));

		clientJoinTimeout = ofNullable(properties.getProperty(CLIENT_JOIN_TIMEOUT_VALUE)).map(t -> TimeUnit.valueOf(
						properties.getProperty(CLIENT_JOIN_TIMEOUT_UNIT, DEFAULT_CLIENT_JOIN_TIMEOUT_UNIT))
				.toMillis(Long.parseLong(t))).orElse(DEFAULT_CLIENT_JOIN_TIMEOUT);

		lockFileName = properties.getProperty(LOCK_FILE_NAME);
		if (lockFileName != null) {
			LOGGER.warn(PROPERTY_WILL_BE_REMOVED, LOCK_FILE_NAME.getPropertyName(), FILE_LOCK_NAME.getPropertyName());
			lockFileName = properties.getProperty(FILE_LOCK_NAME, lockFileName);
		} else {
			lockFileName = properties.getProperty(FILE_LOCK_NAME, DEFAULT_LOCK_FILE_NAME);
		}
		syncFileName = properties.getProperty(SYNC_FILE_NAME);
		if (syncFileName != null) {
			LOGGER.warn(PROPERTY_WILL_BE_REMOVED, SYNC_FILE_NAME.getPropertyName(), FILE_SYNC_NAME.getPropertyName());
			syncFileName = properties.getProperty(FILE_SYNC_NAME, syncFileName);
		} else {
			syncFileName = properties.getProperty(FILE_SYNC_NAME, DEFAULT_SYNC_FILE_NAME);
		}

		String fileWaitTimeoutStr = properties.getProperty(FILE_WAIT_TIMEOUT_MS);
		if (fileWaitTimeoutStr != null) {
			LOGGER.warn(PROPERTY_WILL_BE_REMOVED,
					FILE_WAIT_TIMEOUT_MS.getPropertyName(),
					CLIENT_JOIN_LOCK_TIMEOUT_VALUE.getPropertyName() + ","
							+ CLIENT_JOIN_LOCK_TIMEOUT_UNIT.getPropertyName()
			);
			lockWaitTimeout = Long.parseLong(fileWaitTimeoutStr);
		}
		String waitTimeoutStr = properties.getProperty(CLIENT_JOIN_LOCK_TIMEOUT_VALUE);
		if (waitTimeoutStr != null) {
			TimeUnit waitTimeUnit = TimeUnit.valueOf(properties.getProperty(CLIENT_JOIN_LOCK_TIMEOUT_UNIT,
					DEFAULT_CLIENT_JOIN_LOCK_TIMEOUT_UNIT
			));
			lockWaitTimeout = waitTimeUnit.toMillis(Long.parseLong(waitTimeoutStr));
		}
		if (fileWaitTimeoutStr == null && waitTimeoutStr == null) {
			lockWaitTimeout = DEFAULT_FILE_WAIT_TIMEOUT_MS;
		}

		lockPortNumber = properties.getPropertyAsInt(CLIENT_JOIN_LOCK_PORT, DEFAULT_CLIENT_JOIN_LOCK_PORT);

		this.rxBufferSize = properties.getPropertyAsInt(RX_BUFFER_SIZE, DEFAULT_RX_BUFFER_SIZE);

		String truncateLegacy = properties.getProperty(TRUNCATE_ITEM_NAMES);
		if (StringUtils.isNotBlank(truncateLegacy)) {
			LOGGER.warn(PROPERTY_WILL_BE_REMOVED,
					TRUNCATE_ITEM_NAMES.getPropertyName(),
					TRUNCATE_FIELDS.getPropertyName()
			);
			this.truncateFields = properties.getPropertyAsBoolean(TRUNCATE_FIELDS,
					Boolean.parseBoolean(truncateLegacy)
			);
		} else {
			this.truncateFields = properties.getPropertyAsBoolean(TRUNCATE_FIELDS, DEFAULT_TRUNCATE);
		}

		String legacyTruncateItemNamesLimit = properties.getProperty(TRUNCATE_ITEM_LIMIT);
		if (StringUtils.isNotBlank(legacyTruncateItemNamesLimit)) {
			LOGGER.warn(PROPERTY_WILL_BE_REMOVED,
					TRUNCATE_ITEM_LIMIT.getPropertyName(),
					TRUNCATE_ITEM_NAME_LIMIT.getPropertyName()
			);
			this.truncateItemNamesLimit = properties.getPropertyAsInt(TRUNCATE_ITEM_NAME_LIMIT,
					Integer.parseInt(legacyTruncateItemNamesLimit)
			);
		} else {
			this.truncateItemNamesLimit = properties.getPropertyAsInt(TRUNCATE_ITEM_NAME_LIMIT,
					DEFAULT_TRUNCATE_ITEM_NAMES_LIMIT
			);
		}

		String legacyTruncateReplacement = properties.getProperty(TRUNCATE_ITEM_REPLACEMENT);
		if (StringUtils.isNotBlank(legacyTruncateReplacement)) {
			LOGGER.warn(PROPERTY_WILL_BE_REMOVED,
					TRUNCATE_ITEM_REPLACEMENT.getPropertyName(),
					TRUNCATE_REPLACEMENT.getPropertyName()
			);
			this.truncateReplacement = properties.getProperty(TRUNCATE_REPLACEMENT, legacyTruncateReplacement);
		} else {
			this.truncateReplacement = properties.getProperty(TRUNCATE_REPLACEMENT, DEFAULT_TRUNCATE_REPLACEMENT);
		}

		this.attributeLengthLimit = properties.getPropertyAsInt(
				TRUNCATE_ATTRIBUTE_LIMIT,
				DEFAULT_TRUNCATE_ATTRIBUTE_LIMIT
		);
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

	public String getProxyUrl() {
		return proxyUrl;
	}

	public void setProxyUrl(String proxyUrl) {
		this.proxyUrl = proxyUrl;
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

	@Nullable
	public String getLaunchUuid() {
		return launchUuid;
	}

	public void setLaunchUuid(@Nullable String launchUuid) {
		this.launchUuid = launchUuid;
	}

	public Mode getLaunchRunningMode() {
		return launchRunningMode;
	}

	public void setLaunchRunningMode(Mode launchRunningMode) {
		this.launchRunningMode = launchRunningMode;
	}

	public Set<ItemAttributesRQ> getAttributes() {
		return Collections.unmodifiableSet(attributes);
	}

	public void setAttributes(Set<ItemAttributesRQ> attributes) {
		this.attributes = new HashSet<>(attributes);
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

	public Long getBatchPayloadLimit() {
		return batchPayloadLimit;
	}

	public void setBatchPayloadLimit(Long batchPayloadLimit) {
		this.batchPayloadLimit = batchPayloadLimit;
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

	public boolean getClientJoin() {
		return clientJoin;
	}

	public void setClientJoin(boolean mode) {
		this.clientJoin = mode;
	}

	public LaunchIdLockMode getClientJoinMode() {
		return clientJoinMode;
	}

	public void setClientJoinMode(LaunchIdLockMode clientJoinMode) {
		this.clientJoinMode = clientJoinMode;
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

	public long getClientJoinTimeout() {
		return clientJoinTimeout;
	}

	public void setClientJoinTimeout(long clientJoinTimeout) {
		this.clientJoinTimeout = clientJoinTimeout;
	}

	public long getLockWaitTimeout() {
		return lockWaitTimeout;
	}

	public void setLockWaitTimeout(long timeout) {
		this.lockWaitTimeout = timeout;
	}

	public int getLockPortNumber() {
		return lockPortNumber;
	}

	public void setLockPortNumber(int lockPortNumber) {
		this.lockPortNumber = lockPortNumber;
	}

	public boolean isHttpLogging() {
		return httpLogging;
	}

	public void setHttpLogging(boolean httpLogging) {
		this.httpLogging = httpLogging;
	}

	public int getRxBufferSize() {
		return ofNullable(System.getProperty("rx2.buffer-size")).map(Integer::valueOf)
				.map(s -> Math.max(1, s))
				.orElse(rxBufferSize);
	}

	public void setRxBufferSize(int size) {
		rxBufferSize = size;
	}

	/**
	 * @return to truncate or not truncate
	 * @deprecated use {@link #isTruncateFields} instead
	 */
	@Deprecated
	public boolean isTruncateItemNames() {
		return truncateFields;
	}

	/**
	 * @param truncate to truncate or not truncate
	 * @deprecated use {@link #setTruncateFields} instead
	 */
	@Deprecated
	public void setTruncateItemNames(boolean truncate) {
		this.truncateFields = truncate;
	}

	public boolean isTruncateFields() {
		return truncateFields;
	}

	public void setTruncateFields(boolean truncateFields) {
		this.truncateFields = truncateFields;
	}

	public int getTruncateItemNamesLimit() {
		return truncateItemNamesLimit;
	}

	public void setTruncateItemNamesLimit(int limit) {
		this.truncateItemNamesLimit = limit;
	}

	/**
	 * @return truncation replacement
	 * @deprecated Use {@link #getTruncateReplacement} instead
	 */
	@Deprecated
	public String getTruncateItemNamesReplacement() {
		return truncateReplacement;
	}

	/**
	 * @param replacement truncation replacement
	 * @deprecated Use {@link #setTruncateReplacement} instead
	 */
	@Deprecated
	public void setTruncateItemNamesReplacement(String replacement) {
		this.truncateReplacement = replacement;
	}

	public String getTruncateReplacement() {
		return truncateReplacement;
	}

	public void setTruncateReplacement(String replacement) {
		this.truncateReplacement = replacement;
	}

	public int getAttributeLengthLimit() {
		return attributeLengthLimit;
	}

	public void setAttributeLengthLimit(int attributeLengthLimit) {
		this.attributeLengthLimit = attributeLengthLimit;
	}

	public void setHttpCallTimeout(@Nullable Duration httpCallTimeout) {
		this.httpCallTimeout = httpCallTimeout;
	}

	@Nullable
	public Duration getHttpCallTimeout() {
		return httpCallTimeout;
	}

	public void setHttpConnectTimeout(@Nullable Duration httpConnectTimeout) {
		this.httpConnectTimeout = httpConnectTimeout;
	}

	@Nullable
	public Duration getHttpConnectTimeout() {
		return httpConnectTimeout;
	}

	public void setHttpReadTimeout(@Nullable Duration httpReadTimeout) {
		this.httpReadTimeout = httpReadTimeout;
	}

	@Nullable
	public Duration getHttpReadTimeout() {
		return httpReadTimeout;
	}

	public void setHttpWriteTimeout(@Nullable Duration httpWriteTimeout) {
		this.httpWriteTimeout = httpWriteTimeout;
	}

	@Nullable
	public Duration getHttpWriteTimeout() {
		return httpWriteTimeout;
	}

	@VisibleForTesting
	Mode parseLaunchMode(String mode) {
		return Mode.isExists(mode) ? Mode.valueOf(mode.toUpperCase()) : Mode.DEFAULT;
	}

	@Override
	@Nonnull
	public ListenerParameters clone() {
		ListenerParameters clonedParent;
		try {
			clonedParent = (ListenerParameters) super.clone();
		} catch (CloneNotSupportedException exc) {
			clonedParent = new ListenerParameters();
		}
		final ListenerParameters clone = clonedParent;
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
	@Nonnull
	public String toString() {
		@SuppressWarnings("StringBufferReplaceableByString")
		final StringBuilder sb = new StringBuilder("ListenerParameters{");
		sb.append("description='").append(description).append('\'');
		sb.append(", apiKey='").append(apiKey).append('\'');
		sb.append(", baseUrl='").append(baseUrl).append('\'');
		sb.append(", proxyUrl='").append(proxyUrl).append('\'');
		sb.append(", httpLogging='").append(httpLogging).append('\'');
		sb.append(", httpCallTimeout='").append(httpCallTimeout).append('\'');
		sb.append(", httpConnectTimeout='").append(httpConnectTimeout).append('\'');
		sb.append(", httpReadTimeout='").append(httpReadTimeout).append('\'');
		sb.append(", httpWriteTimeout='").append(httpWriteTimeout).append('\'');
		sb.append(", projectName='").append(projectName).append('\'');
		sb.append(", launchName='").append(launchName).append('\'');
		sb.append(", launchUuid='").append(launchUuid).append('\'');
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
		sb.append(", clientJoin=").append(clientJoin);
		sb.append(", clientJoinMode=").append(ofNullable(clientJoinMode).map(Enum::name).orElse(null));
		sb.append(", clientJoinTimeout=").append(clientJoinTimeout);
		sb.append(", lockFileName=").append(lockFileName);
		sb.append(", syncFileName=").append(syncFileName);
		sb.append(", lockWaitTimeout=").append(lockWaitTimeout);
		sb.append(", lockPortNumber=").append(lockPortNumber);
		sb.append(", rxBufferSize=").append(rxBufferSize);
		sb.append('}');
		return sb.toString();
	}
}
