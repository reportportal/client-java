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

import com.epam.reportportal.exception.InternalReportPortalClientException;
import com.epam.reportportal.message.TypeAwareByteSource;
import com.epam.reportportal.utils.http.ContentType;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static java.util.Optional.ofNullable;

/**
 * This class contains functionality for converting images to Black and white
 * colors
 *
 * @author Aliaksei_Makayed
 */
public class ImageConverter {

	public static final String IMAGE_TYPE = "image";

	public static TypeAwareByteSource convertIfImage(TypeAwareByteSource content) {
		try {
			return isImage(content) ? convert(content) : content;
		} catch (IOException e) {
			throw new InternalReportPortalClientException("Unable to read screenshot file. " + e);
		}
	}

	/**
	 * Convert image to black and white colors
	 *
	 * @param source Data Source
	 * @return {@link TypeAwareByteSource}
	 * @throws IOException In case of IO exception
	 */
	public static TypeAwareByteSource convert(ByteSource source) throws IOException {
		BufferedImage image = ImageIO.read(source.openBufferedStream());
		final BufferedImage blackAndWhiteImage = new BufferedImage(image.getWidth(null),
				image.getHeight(null),
				BufferedImage.TYPE_BYTE_GRAY
		);
		final Graphics2D graphics2D = (Graphics2D) blackAndWhiteImage.getGraphics();
		graphics2D.drawImage(image, 0, 0, null);
		graphics2D.dispose();
		return convertToInputStream(blackAndWhiteImage);
	}

	/**
	 * Check is input file is image
	 *
	 * @param source DataSource
	 * @return true if image
	 */
	public static boolean isImage(TypeAwareByteSource source) {
		return isImage(source.getMediaType());
	}

	/**
	 * Check is input file is image
	 *
	 * @param contentType ContentType
	 * @return true if image
	 */
	public static boolean isImage(String contentType) {
		return ofNullable(ContentType.stripMediaType(contentType)).map(type -> type.startsWith(IMAGE_TYPE)).orElse(false);
	}

	/**
	 * Convert BufferedImage to input stream
	 *
	 * @param image Image to be converted
	 */
	private static TypeAwareByteSource convertToInputStream(BufferedImage image) {
		ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
		try {
			ImageIO.write(image, "png", byteOutputStream);
		} catch (IOException e) {
			throw new InternalReportPortalClientException("Unable to transform file to byte array.", e);
		}
		return new TypeAwareByteSource(ByteSource.wrap(byteOutputStream.toByteArray()), ContentType.IMAGE_PNG);
	}
}
