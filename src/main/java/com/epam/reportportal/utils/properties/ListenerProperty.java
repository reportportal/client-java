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
package com.epam.reportportal.utils.properties;

import com.epam.reportportal.annotations.ExternalIssue;
import com.epam.reportportal.annotations.Issue;

/**
 * Describe properties names
 */
public enum ListenerProperty {

	//@formatter:off
    /**
     * ReportPortal instance URL
     */
    BASE_URL("rp.endpoint", true),
    /**
     * A URL of a http proxy to connect to the endpoint
     */
    HTTP_PROXY_URL("rp.http.proxy", false),
    /**
     * A username for used proxy, works only if {@link #HTTP_PROXY_URL} is set
     */
    HTTP_PROXY_USER("rp.http.proxy.username", false),
    /**
     * Password for proxy, works only if {@link #HTTP_PROXY_URL} and {@link #HTTP_PROXY_USER} are set
     */
    HTTP_PROXY_PASSWORD("rp.http.proxy.password", false),
    /**
     * Enable / Disable raw HTTP requests logging
     */
    HTTP_LOGGING("rp.http.logging", false),

    // HTTP timeouts till the next blank line
    HTTP_CALL_TIMEOUT_VALUE("rp.http.timeout.call.value", false),
    HTTP_CALL_TIMEOUT_UNIT("rp.http.timeout.call.unit", false),
    HTTP_CONNECT_TIMEOUT_VALUE("rp.http.timeout.connect.value", false),
    HTTP_CONNECT_TIMEOUT_UNIT("rp.http.timeout.connect.unit", false),
    HTTP_READ_TIMEOUT_VALUE("rp.http.timeout.read.value", false),
    HTTP_READ_TIMEOUT_UNIT("rp.http.timeout.read.unit", false),
    HTTP_WRITE_TIMEOUT_VALUE("rp.http.timeout.write.value", false),
    HTTP_WRITE_TIMEOUT_UNIT("rp.http.timeout.write.unit", false),

    PROJECT_NAME("rp.project", true),
    LAUNCH_NAME("rp.launch", true),
    /**
     * Use predefined Launch UUID.
     */
    LAUNCH_UUID("rp.launch.uuid", false),
    /**
     * Do not create new launch and report to predefined Launch UUID.
     */
    LAUNCH_UUID_CREATION_SKIP("rp.launch.uuid.creation.skip", false),
    /**
     * Print Launch UUID after start in a format: `ReportPortal Launch UUID: {UUID}`.
     */
    LAUNCH_UUID_PRINT("rp.launch.uuid.print", false),
    /**
     * Launch UUID printing stream. Possible values: 'stdout', 'stderr'.
     */
    LAUNCH_UUID_PRINT_OUTPUT("rp.launch.uuid.print.output", false),
    UUID("rp.uuid", false),
    API_KEY("rp.api.key", true),
    BATCH_SIZE_LOGS("rp.batch.size.logs", false),
    BATCH_PAYLOAD_LIMIT("rp.batch.payload.limit", false),
    LAUNCH_ATTRIBUTES("rp.attributes", false),
    DESCRIPTION("rp.description", false),
    IS_CONVERT_IMAGE("rp.convertimage", false),
    KEYSTORE_RESOURCE("rp.keystore.resource", false),
    KEYSTORE_PASSWORD("rp.keystore.password", false),
    REPORTING_TIMEOUT("rp.reporting.timeout", false),
    MODE("rp.mode", false),
    ENABLE("rp.enable", false),
    RERUN("rp.rerun", false),
    RERUN_OF("rp.rerun.of", false),
    ASYNC_REPORTING("rp.reporting.async", false),
    CALLBACK_REPORTING_ENABLED("rp.reporting.callback", false),
    SKIPPED_AS_ISSUE("rp.skipped.issue", false),
    IO_POOL_SIZE("rp.io.pool.size", false),

    /**
     * Run ReportPortal client in multiple client mode. In such mode RC client will share one launch ID between all
     * clients on the machine. Such build systems as Gradle forks JVMs during parallel run, that leads to multiple
     * launches on dashboard, so using that property should allow to merge them all in one.
     */
    CLIENT_JOIN_MODE("rp.client.join", false),
    CLIENT_JOIN_MODE_VALUE("rp.client.join.mode", false),
    FILE_LOCK_NAME("rp.client.join.file.lock.name", false),
    FILE_SYNC_NAME("rp.client.join.file.sync.name", false),
    CLIENT_JOIN_LOCK_PORT("rp.client.join.port", false),

    /**
     * General Launch Lock timeout value. For how long the primary launch will wait for secondary launches until finish and exit.
     */
    CLIENT_JOIN_TIMEOUT_VALUE("rp.client.join.timeout.value", false),
    CLIENT_JOIN_TIMEOUT_UNIT("rp.client.join.timeout.unit", false),

    /**
     * Lock timeout. For how long a launch instance will try to obtain a lock.
     */
    CLIENT_JOIN_LOCK_TIMEOUT_VALUE("rp.client.join.lock.timeout.value", false),
    CLIENT_JOIN_LOCK_TIMEOUT_UNIT("rp.client.join.lock.timeout.unit", false),

    /**
     * Timeout of waiting for the Primary launch to start. If the primary launch does not start within this timeout, the secondary launch
     * will exit.
     */
    CLIENT_JOIN_LAUNCH_TIMEOUT_VALUE("rp.client.join.launch.timeout.value", false),
    CLIENT_JOIN_LAUNCH_TIMEOUT_UNIT("rp.client.join.launch.timeout.unit", false),

    RX_BUFFER_SIZE("rp.rx.buffer.size", false),

    TRUNCATE_FIELDS("rp.truncation.field", false),
    TRUNCATE_REPLACEMENT("rp.truncation.replacement", false),
    TRUNCATE_ITEM_NAME_LIMIT("rp.truncation.item.name.limit", false),
    TRUNCATE_ATTRIBUTE_LIMIT("rp.truncation.attribute.limit", false),

    /**
     * Enable/Disable the feature to truncate Stack Traces of exceptions that being logged to ReportPortal. Default value: <code>true</code>.
     */
    EXCEPTION_TRUNCATE("rp.truncation.exception", false),

    // Issue reporting properties
    /**
     * Bug Tracking System Project name to use along with {@link ExternalIssue} annotation. Should be the same as in corresponding
     * integration.
     */
    BTS_PROJECT("rp.bts.project", false),
    /**
     * Bug Tracking System base URL to use along with {@link ExternalIssue} annotation. Should be the same as in corresponding integration.
     */
    BTS_URL("rp.bts.url", false),
    /**
     * Bug Tracking System URL Pattern for Issues to use along with {@link ExternalIssue} annotation. Use <code>{issue_id}</code> and
     * <code>{bts_project}</code> placeholders to mark a place where to put Issue ID and Bug Tracking System Project name. The result URL
     * should point on the Issue.
     */
    BTS_ISSUE_URL("rp.bts.issue.url", false),
    /**
     * Fail tests marked with {@link Issue} annotation if they passed. Default value: <code>true</code>. Designed to not miss the moment
     * when the issue got fixed but test is still marked by annotation.
     */
    BTS_ISSUE_FAIL("rp.bts.issue.fail", false);
    //formatter:on

    private final String propertyName;

    private final boolean required;

    ListenerProperty(String propertyName, boolean required) {
        this.propertyName = propertyName;
        this.required = required;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public boolean isRequired() {
        return required;
    }
}
