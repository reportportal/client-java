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

import com.epam.reportportal.service.LoggingContext;
import com.epam.reportportal.utils.TagsParser;
import com.epam.reportportal.utils.properties.PropertiesLoader;
import com.epam.ta.reportportal.ws.model.launch.Mode;

import java.util.Set;

import static com.epam.reportportal.utils.properties.ListenerProperty.BASE_URL;
import static com.epam.reportportal.utils.properties.ListenerProperty.BATCH_SIZE_LOGS;
import static com.epam.reportportal.utils.properties.ListenerProperty.DESCRIPTION;
import static com.epam.reportportal.utils.properties.ListenerProperty.ENABLE;
import static com.epam.reportportal.utils.properties.ListenerProperty.IS_CONVERT_IMAGE;
import static com.epam.reportportal.utils.properties.ListenerProperty.LAUNCH_NAME;
import static com.epam.reportportal.utils.properties.ListenerProperty.LAUNCH_TAGS;
import static com.epam.reportportal.utils.properties.ListenerProperty.MODE;
import static com.epam.reportportal.utils.properties.ListenerProperty.PROJECT_NAME;
import static com.epam.reportportal.utils.properties.ListenerProperty.REPORTING_TIMEOUT;
import static com.epam.reportportal.utils.properties.ListenerProperty.SKIPPED_AS_ISSUE;
import static com.epam.reportportal.utils.properties.ListenerProperty.UUID;

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

    public ListenerParameters() {

    }

    public ListenerParameters(PropertiesLoader properties) {
        this.description = properties.getProperty(DESCRIPTION);
        this.uuid = properties.getProperty(UUID);
        this.baseUrl = properties.getProperty(BASE_URL);
        this.projectName = properties.getProperty(PROJECT_NAME);
        this.launchName = properties.getProperty(LAUNCH_NAME);
        this.tags = TagsParser.parseAsSet(properties.getProperty(LAUNCH_TAGS));
        this.launchRunningMode = ListenersUtils.getLaunchMode(properties.getProperty(MODE));
        this.enable = properties.getPropertyAsBoolean(ENABLE, true);
        this.isSkippedAnIssue = properties.getPropertyAsBoolean(SKIPPED_AS_ISSUE, true);

        this.batchLogsSize = properties.getPropertyAsInt(BATCH_SIZE_LOGS, LoggingContext.DEFAULT_BUFFER_SIZE);
        this.convertImage = properties.getPropertyAsBoolean(IS_CONVERT_IMAGE, false);
        this.reportingTimeout = properties.getPropertyAsInt(REPORTING_TIMEOUT, 5 * 60);
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
