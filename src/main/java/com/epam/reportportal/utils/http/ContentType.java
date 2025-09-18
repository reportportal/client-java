/*
 * Copyright 2023 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.reportportal.utils.http;

import jakarta.annotation.Nullable;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Content-Type header constants and utility methods.
 */
@SuppressWarnings("unused")
public class ContentType {
	private static final Pattern HTTP_HEADER_DELIMITER_PATTERN = Pattern.compile("[=;,]");
	private static final String TOKEN = "[0-9A-Za-z!#$%&'*+.^_`|~-]+";
	private static final String TYPE = "(application|audio|font|example|image|message|model|multipart|text|video|x-" + TOKEN + ")";
	private static final String MEDIA_TYPE = TYPE + "/" + "(" + TOKEN + ")";
	private static final Pattern MEDIA_TYPE_PATTERN = Pattern.compile(MEDIA_TYPE);

	// Binary types
	// Images
	public static final String IMAGE_BMP = "image/bmp";
	public static final String IMAGE_GIF = "image/gif";
	public static final String IMAGE_JPEG = "image/jpeg";
	public static final String IMAGE_PNG = "image/png";
	public static final String IMAGE_TIFF = "image/tiff";
	public static final String IMAGE_WEBP = "image/webp";
	public static final String IMAGE_X_ICON = "image/x-icon";
	// Video
	public static final String VIDEO_MPEG = "video/mpeg";
	public static final String VIDEO_OGG = "video/ogg";
	public static final String VIDEO_WEBM = "video/webm";
	// Audio
	public static final String AUDIO_MIDI = "audio/midi";
	public static final String AUDIO_MPEG = "audio/mpeg";
	public static final String AUDIO_OGG = "audio/ogg";
	public static final String AUDIO_WEBM = "audio/webm";
	public static final String AUDIO_WAV = "audio/wav";
	// Archives
	public static final String APPLICATION_JAVA_ARCHIVE = "application/java-archive";
	public static final String APPLICATION_ZIP = "application/zip";
	public static final String APPLICATION_GZIP = "application/gzip";
	// Misc
	public static final String APPLICATION_PDF = "application/pdf";
	public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

	// Text types
	public static final String APPLICATION_ATOM_XML = "application/atom+xml";
	public static final String APPLICATION_JSON = "application/json";
	public static final String APPLICATION_SOAP_XML = "application/soap+xml";
	public static final String APPLICATION_SVG_XML = "application/svg+xml";
	public static final String APPLICATION_XHTML_XML = "application/xhtml+xml";
	public static final String APPLICATION_XML = "application/xml";
	public static final String IMAGE_SVG = "image/svg+xml";
	public static final String TEXT_PLAIN = "text/plain";
	public static final String TEXT_HTML = "text/html";
	public static final String TEXT_XML = "text/xml";
	public static final String TEXT_JSON = "text/json";
	public static final String RP_JSON_ITEM = "application/x.reportportal.test.v2+json";
	public static final String RP_JSON_LAUNCH = "application/x.reportportal.launch.v2+json";

	// Form types
	public static final String APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded";

	// Multipart types
	public static final String MULTIPART_FORM_DATA = "multipart/form-data";
	public static final String MULTIPART_MIXED = "multipart/mixed";
	public static final String MULTIPART_ALTERNATIVE = "multipart/alternative";
	public static final String MULTIPART_DIGEST = "multipart/digest";
	public static final String MULTIPART_PARALLEL = "multipart/parallel";

	public static final Set<String> KNOWN_TYPES;

	static {
		KNOWN_TYPES = Arrays.stream(ContentType.class.getFields())
				.filter(f -> String.class.equals(f.getType()) && Modifier.isStatic(f.getModifiers()) && Modifier.isPublic(f.getModifiers()))
				.map(f -> {
					try {
						return (String) f.get(null);
					} catch (IllegalAccessException e) {
						return null;
					}
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toUnmodifiableSet());
	}

	private ContentType() {
		throw new RuntimeException("No instances should exist for the class!");
	}

	/**
	 * Extract Media Type from a Content-Type header.
	 *
	 * @param contentType Content-Type header value
	 * @return Media Type
	 */
	@Nullable
	public static String stripMediaType(@Nullable String contentType) {
		if (contentType == null || contentType.trim().isEmpty()) {
			return null;
		}
		String trimmed = contentType.trim();
		Matcher m = HTTP_HEADER_DELIMITER_PATTERN.matcher(trimmed);
		String mimeType;
		if (m.find()) {
			mimeType = trimmed.substring(0, m.start());
		} else {
			mimeType = trimmed;
		}
		return mimeType.isEmpty() ? null : mimeType;
	}

	/**
	 * Check if the Media Type is known.
	 *
	 * @param mediaType Media Type value
	 * @return {@code true} if the Media Type is known, {@code false} otherwise
	 */
	public static boolean isKnownType(@Nullable String mediaType) {
		return KNOWN_TYPES.contains(stripMediaType(mediaType));
	}

	/**
	 * Check if the Media Type is valid.
	 *
	 * @param mediaType Media Type value
	 * @return {@code true} if the Media Type is valid, {@code false} otherwise
	 */
	public static boolean isValidType(@Nullable String mediaType) {
		if (mediaType == null || mediaType.trim().isEmpty()) {
			return false;
		}
		String trimmed = mediaType.trim();
		return MEDIA_TYPE_PATTERN.matcher(trimmed).matches();
	}
}
