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

import com.epam.reportportal.utils.files.ByteSource;
import com.epam.reportportal.utils.files.Utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility stuff to detect mime type of binary data
 */
public class MimeTypeDetector {
	private static final String UNKNOWN_TYPE = "application/octet-stream";
	private static final String EXTENSION_DELIMITER = ".";
	private static final int BYTES_TO_READ_FOR_DETECTION = 20;

	private static final Map<String, String> ADDITIONAL_EXTENSION_MAPPING = Collections.unmodifiableMap(new HashMap<String, String>() {{
		put(".properties", "text/plain");
		put(".json", "application/json");
	}});

	private MimeTypeDetector() {
		throw new IllegalStateException("Static only class. No instances should exist for the class!");
	}

	private static int[] readDetectionBytes(@Nonnull InputStream is) throws IOException {
		if (!is.markSupported()) {
			// Trigger UnsupportedOperationException before reading the stream, no users should get there unless they hack with reflections
			is.reset();
		}
		int[] bytes = new int[BYTES_TO_READ_FOR_DETECTION];
		int readNum = 0;
		int read;
		while (((read = is.read()) != -1) && readNum < BYTES_TO_READ_FOR_DETECTION) {
			bytes[readNum++] = read;
		}
		if (readNum < BYTES_TO_READ_FOR_DETECTION - 1) {
			bytes = Arrays.copyOf(bytes, readNum);
		}
		is.reset();
		return bytes;
	}

	@Nullable
	static String guessContentTypeFromStream(@Nonnull InputStream is) throws IOException {
		int[] bytes = readDetectionBytes(is);
		if (bytes.length >= 8) {
			if (bytes[0] == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47 // 4 bytes break
					&& bytes[4] == 0x0d && bytes[5] == 0x0a && bytes[6] == 0x1a && bytes[7] == 0x0a) {
				return "image/png";
			}
		}
		if (bytes.length >= 4) {
			if (bytes[0] == 0x50 && bytes[1] == 0x4b && bytes[2] == 0x03 && bytes[3] == 0x04) {
				// ZIPs
				if (bytes.length >= 7 && bytes[4] == 0x14 && bytes[5] == 0x00 && bytes[6] == 0x08) {
					return "application/java-archive";
				}
				return "application/zip";
			}
			if (bytes[0] == 0x25 && bytes[1] == 0x50 && bytes[2] == 0x44 && bytes[3] == 0x46) {
				return "application/pdf";
			}
			if (bytes[0] == 0xFF && bytes[1] == 0xD8 && bytes[2] == 0xFF) {
				// JPEGs
				if (bytes[3] == 0xE0 || bytes[3] == 0xE1 || bytes[3] == 0xE8) {
					// E0 - JPEG/JFIF; E1 - EXIF; E8 - SPIFF
					return "image/jpeg";
				}
			}
		}
		return null;
	}

	private static String detectByExtensionInternal(String name) {
		int extensionIndex = name.lastIndexOf(EXTENSION_DELIMITER);
		if (extensionIndex >= 0) {
			return ADDITIONAL_EXTENSION_MAPPING.get(name.substring(extensionIndex));
		}
		return null;
	}

	@Nonnull
	public static String detect(@Nonnull final File file) throws IOException {
		ByteSource source = Utils.getFileAsByteSource(file);
		String type = URLConnection.guessContentTypeFromStream(source.openStream());
		if (type == null) {
			type = guessContentTypeFromStream(source.openStream());
		}
		if (type == null) {
			type = Files.probeContentType(file.toPath());
		}
		if (type == null) {
			type = URLConnection.guessContentTypeFromName(file.getName());
		}
		if (type == null) {
			type = detectByExtensionInternal(file.getName());
		}
		return type == null ? UNKNOWN_TYPE : type;
	}

	@Nonnull
	public static String detect(@Nonnull final ByteSource source, @Nullable final String resourceName) throws IOException {
		String type = URLConnection.guessContentTypeFromStream(source.openStream());
		if (type == null) {
			type = guessContentTypeFromStream(source.openStream());
		}
		if (resourceName != null && type == null) {
			type = Files.probeContentType(Paths.get(resourceName));
			if (type == null) {
				type = URLConnection.guessContentTypeFromName(resourceName);
			}
			if (type == null) {
				type = detectByExtensionInternal(resourceName);
			}
		}
		return type == null ? UNKNOWN_TYPE : type;
	}
}
