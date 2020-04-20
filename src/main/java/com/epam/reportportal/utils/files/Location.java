/*
 *  Copyright 2020 EPAM Systems
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.epam.reportportal.utils.files;

import com.epam.reportportal.message.TypeAwareByteSource;
import com.epam.reportportal.utils.MimeTypeDetector;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * File location utilities.
 */
public class Location {

	/**
	 * This is an util class and should not be instantiated.
	 */
	private Location() {
	}

	public static TypeAwareByteSource locateFile(File file) throws IOException {
		if (file.exists() && file.isFile()) {
			return new TypeAwareByteSource(Files.asByteSource(file), MimeTypeDetector.detect(file));
		}
		String path = file.getPath();
		String name = file.getName();
		InputStream resource = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
		if (resource == null) {
			throw new FileNotFoundException("Unable to locate file of path: " + file.getPath());
		}
		byte[] data = ByteStreams.toByteArray(resource);
		ByteSource byteSource = ByteSource.wrap(data);
		return new TypeAwareByteSource(byteSource, MimeTypeDetector.detect(byteSource, name));
	}
}
