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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class TestUtils {
	public static final String FILE_PATH = "src/test/resources/files/image.png";

	@Test
	public void test_copy_files() throws IOException {
		File file = File.createTempFile("rp-test", ".png");
		Utils.copyFiles(new File(FILE_PATH), file);

		assertThat(
				IOUtils.toByteArray(Files.newInputStream(file.toPath())),
				equalTo(IOUtils.toByteArray(Files.newInputStream(Paths.get(FILE_PATH))))
		);
	}

}
