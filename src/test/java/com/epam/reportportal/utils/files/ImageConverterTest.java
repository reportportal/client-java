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
import com.google.common.io.ByteSource;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Andrei Varabyeu
 */
public class ImageConverterTest {
	@Test
	public void isImage() throws Exception {
		final String resourceName = "defaultUserPhoto.jpg";
		InputStream stream = ImageConverterTest.class.getClassLoader().getResourceAsStream(resourceName);
		assertThat("Image not found in path: " + resourceName, stream, notNullValue());
		byte[] data = Utils.readInputStreamToBytes(stream);
		final ByteSource byteSource = ByteSource.wrap(data);
		boolean r = ImageConverter.isImage(new TypeAwareByteSource(byteSource, MimeTypeDetector.detect(byteSource, resourceName)));
		assertThat("Incorrect image type detection", r, equalTo(true));
	}
}
