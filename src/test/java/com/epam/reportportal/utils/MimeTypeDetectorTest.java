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

import com.google.common.io.ByteSource;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;

public class MimeTypeDetectorTest {

	@SuppressWarnings("unused")
	public static Iterable<Object[]> files() {
		return Arrays.asList(new Object[] { Paths.get("src/test/resources/pug/lucky.jpg").toFile(), "image/jpeg" },
				new Object[] { new File("src/test/resources/files/simple_response.txt"), "text/plain" },
				new Object[] { new File("src/test/resources/hello.json"), "application/json" },
				new Object[] { new File("src/test/resources/logback-test.xml"), "application/xml" },
				new Object[] { new File("src/test/resources/junit-platform.properties"), "application/octet-stream" }
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
		Assertions.assertEquals(expected,
				MimeTypeDetector.detect(ByteSource.wrap(IOUtils.toByteArray(new FileInputStream(file))), file.getName())
		);
	}
}
