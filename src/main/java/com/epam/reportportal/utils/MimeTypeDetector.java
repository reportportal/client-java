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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLConnection;

/**
 * Utility stuff to detect mime type of binary data
 *
 * @author Andrei Varabyeu
 */
public class MimeTypeDetector {

	private MimeTypeDetector() {
		//statics only
	}

	public static String detect(File file) throws IOException {
		String type = URLConnection.guessContentTypeFromStream(new FileInputStream(file));
		return type == null ? URLConnection.guessContentTypeFromName(file.getName()) : type;
	}

	public static String detect(ByteSource source, String resourceName) throws IOException {
		String type = URLConnection.guessContentTypeFromStream(source.openStream());
		return type == null ? URLConnection.guessContentTypeFromName(resourceName) : type;
	}
}
