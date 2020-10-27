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
    BASE_URL("rp.endpoint", true),
    /**
     * An URL of a http proxy to connect to the endpoint
     */
    HTTP_PROXY_URL("rp.http.proxy", false),
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
    MAX_CONNECTIONS_PER_ROUTE("rp.max.connections.per.route", false),
    MAX_CONNECTIONS_TOTAL("rp.max.connections.total", false),

    /**
     * The property regulates maximum time in milliseconds after which a connection will be closed and thrown away.
     */
    MAX_CONNECTION_TIME_TO_LIVE("rp.transport.connections.general.ttl.milliseconds", false),

    /**
     * The property regulates maximum idle (no any transfer) time in milliseconds after which a connection will be closed and thrown away.
     */
    MAX_CONNECTION_IDLE_TIME("rp.transport.connections.idle.ttl.milliseconds", false),

    /**
     * Maximum number of deliver attempts on transport layer. Retries IO and connection exceptions.
     */
    MAX_TRANSFER_RETRY_COUNT("rp.transport.connections.retry.count", false),

    /**
     * Run report portal client in multiple client mode. In such mode RC client will share one launch ID between all clients on the machine.
     * Such build systems as Gradle forks JVMs during parallel run, that leads to multiple launches on dashboard,
     * so using that property should allow to merge them all in one.
     */
    CLIENT_JOIN_MODE("rp.client.join", false),
    LOCK_FILE_NAME("rp.client.join.lock.file.name", false),
    SYNC_FILE_NAME("rp.client.join.sync.file.name", false),
    FILE_WAIT_TIMEOUT_MS("rp.client.join.file.wait.timeout.ms", false);
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
