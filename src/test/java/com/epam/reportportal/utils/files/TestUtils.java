/*
 * Copyright 2024 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.reportportal.utils.files;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class TestUtils {
	public static final String FILE_PATH = "src/test/resources/files/image.png";
	public static final String FILE_CLASSPATH_URI = "classpath:files/image.png";
	public static final String FILE_ABSOLUTE_NO_HOST_PATH_URI = "file:" + System.getProperty("user.dir") + File.separator + FILE_PATH;
	public static final String FILE_ABSOLUTE_OMIT_HOST_PATH_URI = "file://" + System.getProperty("user.dir") + File.separator + FILE_PATH;

	@Test
	public void test_copy_files() throws IOException {
		File file = File.createTempFile("rp-test", ".png");
		Utils.copyFiles(new File(FILE_PATH), file);

		assertThat(
				IOUtils.toByteArray(Files.newInputStream(file.toPath())),
				equalTo(IOUtils.toByteArray(Files.newInputStream(Paths.get(FILE_PATH))))
		);
	}

	public static Iterable<Object[]> fileUriProvider() {
		return Arrays.asList(
				new Object[] { FILE_ABSOLUTE_NO_HOST_PATH_URI },
				new Object[] { FILE_ABSOLUTE_OMIT_HOST_PATH_URI },
				new Object[] { FILE_CLASSPATH_URI }
		);
	}

	@ParameterizedTest
	@MethodSource("fileUriProvider")
	public void test_get_file_by_uri(String uri) throws IOException {
		assertThat(Utils.getFile(URI.create(uri)).read(), equalTo(IOUtils.toByteArray(Files.newInputStream(Paths.get(FILE_PATH)))));
	}
}
