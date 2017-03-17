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
package com.epam.reportportal.message;

import com.epam.reportportal.utils.MimeTypeDetector;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.common.net.MediaType;

import java.io.File;

/**
 * Report portal message wrapper. This wrapper should be used if any file <br>
 * should be attached to log message. This wrapper should be used<br>
 * only with log4j log framework.
 */
public class ReportPortalMessage {

    private TypeAwareByteSource data;

    private String message;

    public ReportPortalMessage() {
    }

    public ReportPortalMessage(String message) {
        this.message = message;
    }

    public ReportPortalMessage(final ByteSource data, String mediaType, String message) {
        this(message);
        this.data = new TypeAwareByteSource(data, MediaType.parse(mediaType));
    }

    public ReportPortalMessage(final TypeAwareByteSource data, String message) {
        this(message);
        this.data = data;
    }

    public ReportPortalMessage(File file, String message) {
        this(new TypeAwareByteSource(Files.asByteSource(file), MimeTypeDetector.detect(file)), message);
    }

    public String getMessage() {
        return message;
    }

    public TypeAwareByteSource getData() {
        return data;
    }

}
