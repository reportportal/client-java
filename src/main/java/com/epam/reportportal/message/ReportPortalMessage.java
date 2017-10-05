/*
 * Copyright 2016 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/client-core
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

import com.google.common.io.ByteSource;

import java.io.File;
import java.io.IOException;

import static com.epam.reportportal.utils.MimeTypeDetector.detect;
import static com.google.common.io.Files.asByteSource;

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
		this.data = new TypeAwareByteSource(data, mediaType);
	}

	public ReportPortalMessage(final TypeAwareByteSource data, String message) {
		this(message);
		this.data = data;
	}

	public ReportPortalMessage(File file, String message) throws IOException {
		this(new TypeAwareByteSource(asByteSource(file), detect(file)), message);
	}

	public String getMessage() {
		return message;
	}

	public TypeAwareByteSource getData() {
		return data;
	}

}
