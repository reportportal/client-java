/*
 *  Copyright 2021 EPAM Systems
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.epam.reportportal.utils;

import com.epam.reportportal.utils.files.Utils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class MimeTypeDetectorTest {

	@SuppressWarnings("unused")
	public static Iterable<Object[]> files() {
		return Arrays.asList(
				new Object[] { Paths.get("src/test/resources/pug/lucky.jpg").toFile(), "image/jpeg" },
				new Object[] { new File("src/test/resources/files/simple_response.txt"), "text/plain" },
				new Object[] { new File("src/test/resources/hello.json"), "application/json" },
				new Object[] { new File("src/test/resources/logback-test.xml"), "application/xml" },
				new Object[] { new File("src/test/resources/junit-platform.properties"), "text/plain" },
				new Object[] { Paths.get("src/test/resources/files/test.bin").toFile(), "application/octet-stream" }
		);
	}

	@ParameterizedTest
	@MethodSource("files")
	public void test_mime_types_files(File file, String expected) throws IOException {
		Assertions.assertEquals(expected, MimeTypeDetector.detect(file));
	}

	@ParameterizedTest
	@MethodSource("files")
	public void test_mime_types_byte_source(File file, String expected) throws IOException {
		Assertions.assertEquals(expected, MimeTypeDetector.detect(Utils.getFileAsByteSource(file), file.getName()));
	}

	@SuppressWarnings("unused")
	public static Iterable<Object[]> binaryFiles() {
		return Arrays.asList(
				new Object[] { Paths.get("src/test/resources/pug/lucky.jpg").toFile(), "image/jpeg" },
				new Object[] { Paths.get("src/test/resources/pug/unlucky.jpg").toFile(), "image/jpeg" },
				new Object[] { Paths.get("src/test/resources/files/image.png").toFile(), "image/png" },
				new Object[] { Paths.get("src/test/resources/files/demo.zip").toFile(), "application/zip" },
				new Object[] { Paths.get("src/test/resources/files/test.jar").toFile(), "application/java-archive" },
				new Object[] { Paths.get("src/test/resources/files/test.pdf").toFile(), "application/pdf" }
		);
	}

	@ParameterizedTest
	@MethodSource("binaryFiles")
	public void test_mime_types_files_by_content_only(File file, String expected) throws IOException {
		File testFile = Files.createTempFile("test_tmp_", null).toFile();
		try (InputStream is = new FileInputStream(file)) {
			try (OutputStream os = new FileOutputStream(testFile)) {
				Utils.copyStreams(is, os);
			}
		}
		Assertions.assertEquals(expected, MimeTypeDetector.detect(testFile));
	}

	@SuppressWarnings("unused")
	public static Iterable<Object[]> binaryFilesFallback() {
		return Arrays.asList(
				new Object[] { Paths.get("src/test/resources/pug/lucky.jpg").toFile(), "image/jpeg" },
				new Object[] { Paths.get("src/test/resources/pug/unlucky.jpg").toFile(), "image/jpeg" },
				new Object[] { Paths.get("src/test/resources/files/image.png").toFile(), "image/png" }
		);
	}

	@ParameterizedTest
	@MethodSource("binaryFilesFallback")
	public void test_mime_types_files_by_content_only_fallback(File file, String expected) throws IOException {
		File testFile = Files.createTempFile("test_tmp_", null).toFile();
		try (InputStream is = new FileInputStream(file)) {
			try (OutputStream os = new FileOutputStream(testFile)) {
				Utils.copyStreams(is, os);
			}
		}
		Assertions.assertEquals(expected, MimeTypeDetector.guessContentTypeFromStream(Utils.getFileAsByteSource(testFile).openStream()));
	}
}
