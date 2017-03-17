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
package com.epam.reportportal.service;

import com.epam.reportportal.exception.ReportPortalClientException;
import com.epam.ta.reportportal.ws.model.ErrorRS;
import com.github.avarabyeu.restendpoint.http.DefaultErrorHandler;
import com.github.avarabyeu.restendpoint.http.HttpMethod;
import com.github.avarabyeu.restendpoint.http.exception.RestEndpointIOException;
import com.github.avarabyeu.restendpoint.http.exception.SerializerException;
import com.github.avarabyeu.restendpoint.serializer.Serializer;
import com.google.common.io.ByteSource;

import java.io.IOException;
import java.net.URI;

/**
 * Report Portal Error Handler<br>
 * Converts error from Endpoint to ReportPortal-related errors
 *
 * @author Andrei Varabyeu
 */
public class ReportPortalErrorHandler extends DefaultErrorHandler {

    private Serializer serializer;

    public ReportPortalErrorHandler(Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    protected void handleError(URI requestUri, HttpMethod requestMethod, int statusCode, String statusMessage,
            ByteSource errorBody) throws RestEndpointIOException {
        throw new ReportPortalClientException(statusCode, statusMessage, deserializeError(errorBody));
    }

    private ErrorRS deserializeError(ByteSource contentSource) throws RestEndpointIOException {
        byte[] content;
        try {
            content = contentSource.read();
            if (null != content) {
                return serializer.deserialize(content, ErrorRS.class);
            } else {
                return null;
            }

        } catch (IOException | RestEndpointIOException e ) {
            return null;
        }

    }
}
