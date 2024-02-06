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
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility stuff to detect mime type of binary data
 */
public class MimeTypeDetector {
	private static final String UNKNOWN_TYPE = "application/octet-stream";
	private static final String EXTENSION_DELIMITER = ".";

	private static final Map<String, String> ADDITIONAL_EXTENSION_MAPPING =
			Collections.unmodifiableMap(new HashMap<String, String>() {{
				put(".properties", "text/plain");
			}});

	private MimeTypeDetector() {
		throw new IllegalStateException("Static only class. No instances should exist for the class!");
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
		String type = URLConnection.guessContentTypeFromStream(Utils.getFile(file).openStream());
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
		if (resourceName != null) {
			if (type == null) {
				type = Files.probeContentType(Paths.get(resourceName));
			}
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
