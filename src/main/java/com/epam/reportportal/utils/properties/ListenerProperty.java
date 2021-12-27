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

/**
 * Describe properties names
 */
public enum ListenerProperty {

	//@formatter:off
    /**
     * Report Portal instance URL
     */
    BASE_URL("rp.endpoint", true),
    /**
     * A URL of a http proxy to connect to the endpoint
     */
    HTTP_PROXY_URL("rp.http.proxy", false),
    /**
     * Enable / Disable raw HTTP requests logging
     */
    HTTP_LOGGING("rp.http.logging", false),
    /**
     * Different HTTP timeouts
     */
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
    UUID("rp.uuid", false),
    API_KEY("rp.api.key", true),
    BATCH_SIZE_LOGS("rp.batch.size.logs", false),
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
     * Run report portal client in multiple client mode. In such mode RC client will share one launch ID between all clients on the machine.
     * Such build systems as Gradle forks JVMs during parallel run, that leads to multiple launches on dashboard,
     * so using that property should allow to merge them all in one.
     */
    CLIENT_JOIN_MODE("rp.client.join", false),
    CLIENT_JOIN_MODE_VALUE("rp.client.join.mode", false),
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    LOCK_FILE_NAME("rp.client.join.lock.file.name", false),
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    SYNC_FILE_NAME("rp.client.join.sync.file.name", false),
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    FILE_WAIT_TIMEOUT_MS("rp.client.join.file.wait.timeout.ms", false),
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

    RX_BUFFER_SIZE("rp.rx.buffer.size", false),

    TRUNCATE_ITEM_NAMES("rp.item.name.truncate", false),
    TRUNCATE_ITEM_LIMIT("rp.item.name.truncate.limit", false),
    TRUNCATE_ITEM_REPLACEMENT("rp.item.name.truncate.replacement", false);
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
