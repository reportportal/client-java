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

import javax.annotation.Nullable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Content-Type header constants and utility methods.
 */
@SuppressWarnings("unused")
public class ContentType {
	private static final Pattern HTTP_HEADER_DELIMITER_PATTERN = Pattern.compile("[=;,]");

	// Binary types
	public static final String IMAGE_BMP = "image/bmp";
	public static final String IMAGE_GIF = "image/gif";
	public static final String IMAGE_JPEG = "image/jpeg";
	public static final String IMAGE_PNG = "image/png";
	public static final String IMAGE_TIFF = "image/tiff";
	public static final String IMAGE_WEBP = "image/webp";
	public static final String VIDEO_MPEG = "video/mpeg";
	public static final String VIDEO_OGG = "video/ogg";
	public static final String VIDEO_WEBM = "video/webm";
	public static final String APPLICATION_JAVA_ARCHIVE = "application/java-archive";
	public static final String APPLICATION_ZIP = "application/zip";
	public static final String APPLICATION_GZIP = "application/gzip";
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

	// Form types
	public static final String APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded";

	// Multipart types
	public static final String MULTIPART_FORM_DATA = "multipart/form-data";
	public static final String MULTIPART_MIXED = "multipart/mixed";
	public static final String MULTIPART_ALTERNATIVE = "multipart/alternative";
	public static final String MULTIPART_DIGEST = "multipart/digest";
	public static final String MULTIPART_PARALLEL = "multipart/parallel";

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
	public static String parse(@Nullable String contentType) {
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
}
