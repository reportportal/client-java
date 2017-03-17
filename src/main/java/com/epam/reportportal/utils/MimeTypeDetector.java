/*
 * Copyright 2016 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/client-java-core
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
package com.epam.reportportal.utils;

import com.google.common.base.Strings;
import com.google.common.io.ByteSource;
import com.google.common.net.MediaType;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Utility stuff to detect mime type of binary data
 *
 * @author Andrei Varabyeu
 */
public class MimeTypeDetector {

    private static Detector detector = new AutoDetectParser().getDetector();

    private MimeTypeDetector() {
        //statics only
    }

    public static MediaType detect(File file) {
        final Metadata metadata = new Metadata();
        metadata.set(Metadata.RESOURCE_NAME_KEY, file.getName());
        try {
            return detect(TikaInputStream.get(file), metadata);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Unable to resolve mime type", e);
        }
    }

    public static MediaType detect(ByteSource source, String resourceName) {

        final Metadata metadata = new Metadata();
        if (!Strings.isNullOrEmpty(resourceName)) {
            metadata.set(Metadata.RESOURCE_NAME_KEY, resourceName);
        }
        try {
            return detect(TikaInputStream.get(source.openBufferedStream()), metadata);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to resolve mime type", e);
        }
    }

    public static MediaType detect(TikaInputStream is, Metadata metadata) {
        try {
            return MediaType.parse(detector.detect(is, metadata).toString());
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to resolve mime type", e);
        }
    }
}
