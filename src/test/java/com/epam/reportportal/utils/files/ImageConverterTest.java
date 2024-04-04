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
package com.epam.reportportal.utils.files;

import com.epam.reportportal.message.TypeAwareByteSource;
import com.epam.reportportal.utils.MimeTypeDetector;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Andrei Varabyeu
 */
public class ImageConverterTest {

	private static TypeAwareByteSource getTestImage() throws Exception {
		final String resourceName = "defaultUserPhoto.jpg";
		InputStream stream = ImageConverterTest.class.getClassLoader().getResourceAsStream(resourceName);
		assertThat("Image not found in path: " + resourceName, stream, notNullValue());
		ByteSource data = ByteSource.wrap(Utils.readInputStreamToBytes(stream));
		return new TypeAwareByteSource(data, MimeTypeDetector.detect(data, resourceName));
	}

	private static TypeAwareByteSource getTestFile() throws Exception {
		final String resourceName = "hello.json";
		InputStream stream = ImageConverterTest.class.getClassLoader().getResourceAsStream(resourceName);
		assertThat("File not found in path: " + resourceName, stream, notNullValue());
		ByteSource data = ByteSource.wrap(Utils.readInputStreamToBytes(stream));
		return new TypeAwareByteSource(data, MimeTypeDetector.detect(data, resourceName));
	}

	@Test
	public void isImage() throws Exception {
		assertThat("Incorrect image type detection", ImageConverter.isImage(getTestImage()), equalTo(true));
		assertThat("Incorrect image type detection", ImageConverter.isImage(getTestFile()), equalTo(false));
	}

	@Test
	public void testImageConvert() throws Exception {
		TypeAwareByteSource bwImage = ImageConverter.convertIfImage(getTestImage());
		byte[] expectedImage = IOUtils.toByteArray(Objects.requireNonNull(ImageConverterTest.class.getClassLoader()
				.getResourceAsStream("defaultUserPhoto_bw.png")));
		assertThat("Image conversion failed", bwImage, notNullValue());
		assertThat("Invalid result image", bwImage.read(), equalTo(expectedImage));
	}
}
