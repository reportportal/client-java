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
package com.epam.reportportal.utils;

import com.epam.reportportal.utils.files.Utils;
import com.google.common.io.ByteSource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Utility stuff to detect mime type of binary data
 *
 * @author Andrei Varabyeu
 */
public class MimeTypeDetector {
	private static final String UNKNOWN_TYPE = "application/octet-stream";

	private MimeTypeDetector() {
		//statics only
	}

	@Nonnull
	public static String detect(@Nonnull final File file) throws IOException {
		String type = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(Utils.readInputStreamToBytes(new FileInputStream(
				file))));
		if (type == null) {
			type = Files.probeContentType(file.toPath());
		}
		if (type == null) {
			type = URLConnection.guessContentTypeFromName(file.getName());
		}
		return type == null ? UNKNOWN_TYPE : type;
	}

	@Nonnull
	public static String detect(@Nonnull final ByteSource source, @Nullable final String resourceName) throws IOException {
		String type = URLConnection.guessContentTypeFromStream(source.openStream());
		if (type == null && resourceName != null) {
			type = Files.probeContentType(Paths.get(resourceName));
		}
		if (type == null && resourceName != null) {
			type = URLConnection.guessContentTypeFromName(resourceName);
		}
		return type == null ? UNKNOWN_TYPE : type;
	}
}
