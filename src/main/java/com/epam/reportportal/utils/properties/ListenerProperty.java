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
package com.epam.reportportal.utils.properties;

/**
 * Describe properties names
 */
public enum ListenerProperty {

	//@formatter:off
    BASE_URL("rp.endpoint", true),
    PROJECT_NAME("rp.project", true),
    LAUNCH_NAME("rp.launch", true),
    UUID("rp.uuid", true),
    BATCH_SIZE_LOGS("rp.batch.size.logs", false),
    LAUNCH_TAGS("rp.tags", false),
    DESCRIPTION("rp.description", false),
    IS_CONVERT_IMAGE("rp.convertimage", false),
    KEYSTORE_RESOURCE("rp.keystore.resource", false),
    KEYSTORE_PASSWORD("rp.keystore.password", false),
    REPORTING_TIMEOUT("rp.reporting.timeout", false),
    MODE("rp.mode", false),
    ENABLE("rp.enable", false),
    RERUN("rp.rerun", false),
    SKIPPED_AS_ISSUE("rp.skipped.issue", false);
    //formatter:on

    private String propertyName;

    private boolean required;

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
