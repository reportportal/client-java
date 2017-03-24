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
package com.epam.reportportal.exception;

/**
 * Base ReportPortal. Used for unclassified errors
 *
 * @author Andrei Varabyeu
 */
public class GeneralReportPortalException extends RuntimeException {
    private static final long serialVersionUID = -3747137063782963453L;

    /**
     * HTTP Status Code
     */
    protected int statusCode;
    /**
     * HTTP Status Message
     */
    protected String statusMessage;

    public GeneralReportPortalException(int statusCode, String statusMessage, String errorContent) {
        super(errorContent);
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;

    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    @Override
    public String getMessage() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Report Portal returned error\n")
                .append("Status code: ").append(statusCode).append("\n")
                .append("Status message: ").append(statusMessage).append("\n");
        if (null != getMessage()) {
            builder.append("Error Message: ").append(getMessage()).append("\n");
        }
        return builder.toString();
    }
}
