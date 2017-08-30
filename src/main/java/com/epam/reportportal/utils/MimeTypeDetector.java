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
package com.epam.reportportal.utils;

import com.google.common.io.ByteSource;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;

import java.io.File;
import java.io.IOException;

import static com.google.common.base.Strings.isNullOrEmpty;

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

    public static String detect(File file) throws IOException {
        final Metadata metadata = new Metadata();
        metadata.set(Metadata.RESOURCE_NAME_KEY, file.getName());
        return detect(TikaInputStream.get(file), metadata);

    }

    public static String detect(ByteSource source, String resourceName) throws IOException {

        final Metadata metadata = new Metadata();
        if (!isNullOrEmpty(resourceName)) {
            metadata.set(Metadata.RESOURCE_NAME_KEY, resourceName);
        }
        return detect(TikaInputStream.get(source.openBufferedStream()), metadata);

    }

    public static String detect(TikaInputStream is, Metadata metadata) throws IOException {
        return detector.detect(is, metadata).toString();
    }
}
