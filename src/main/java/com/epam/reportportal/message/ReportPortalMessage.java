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
