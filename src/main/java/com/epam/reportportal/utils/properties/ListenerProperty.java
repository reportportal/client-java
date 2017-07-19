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
    SKIPPED_AS_ISSUE("rp.skipped.issue", false),
    // http proxy
    HTTP_PROXY_HOST("http.proxyHost", false),
    HTTP_PROXY_PORT("http.proxyPort", false),
    HTTP_NON_PROXY_HOST("http.nonProxyHosts", false),
    // https proxy
    HTTPS_PROXY_HOST("https.proxyHost", false),
    HTTPS_PROXY_PORT("https.proxyPort", false),
    // ftp proxy
    FTP_PROXY_HOST("ftp.proxyHost", false),
    FTP_PROXY_PORT("ftp.proxyPort", false),
    FTP_NON_PROXY_HOSTS("ftp.nonProxyHosts", false),
    // socks proxy
    SOCKS_PROXY_HOST("socksProxyHost", false),
    SOCKS_PROXY_PORT("socksProxyPort", false),
    // proxy credentials
    PROXY_USER("http.proxyUser", false),
    PROXY_PASSWORD("http.proxyPassword", false);
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
