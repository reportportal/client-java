/*
 * Copyright 2016 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/client-core
 *
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.epam.reportportal.utils.files;

import com.epam.reportportal.exception.InternalReportPortalClientException;
import com.epam.reportportal.message.TypeAwareByteSource;
import com.google.common.io.ByteSource;
import com.google.common.net.MediaType;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

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
     * @throws IOException In case of IO exception
     */
    public static TypeAwareByteSource convert(ByteSource source) throws IOException {
        BufferedImage image;
        image = ImageIO.read(source.openBufferedStream());
        final BufferedImage blackAndWhiteImage = new BufferedImage(image.getWidth(null), image.getHeight(null),
                BufferedImage.TYPE_BYTE_GRAY);
        final Graphics2D graphics2D = (Graphics2D) blackAndWhiteImage.getGraphics();
        graphics2D.drawImage(image, 0, 0, null);
        graphics2D.dispose();
        return convertToInputStream(blackAndWhiteImage);
    }

    /**
     * Check is input file is image
     *
     * @param source DataSource
     */
    public static boolean isImage(TypeAwareByteSource source) {
        return isImage(source.getMediaType());
    }

    /**
     * Check is input file is image
     *
     * @param contentType ContentType
     */
    public static boolean isImage(MediaType contentType) {
        return contentType.type().equalsIgnoreCase(IMAGE_TYPE);
    }

    /**
     * Check is input file is image
     *
     * @param contentType ContentType
     */
    public static boolean isImage(String contentType) {
        return isImage(MediaType.parse(contentType));
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
        return new TypeAwareByteSource(ByteSource.wrap(byteOutputStream.toByteArray()), MediaType.PNG.toString());

    }

}
