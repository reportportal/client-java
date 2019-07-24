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
