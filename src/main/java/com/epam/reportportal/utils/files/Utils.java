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

import javax.validation.constraints.NotNull;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;

/**
 * File utilities.
 */
public class Utils {

	/**
	 * This is an util class and should not be instantiated.
	 */
	private Utils() {
	}

	private static final int KILOBYTE = 2 ^ 10;

	private static final int READ_BUFFER = 10 * KILOBYTE;

	/**
	 * Reads an <code>InputStream</code> into a <code>String</code>. Uses UTF-8 encoding and 10 kilobytes buffer by
	 * default.
	 *
	 * @param is a stream to read from
	 * @return the result
	 */
	public static String readInputStreamToString(@NotNull InputStream is) throws IOException {
		byte[] bytes = readInputStreamToBytes(is);
		if (bytes.length <= 0) {
			return "";
		}

		try {
			return new String(bytes, StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			// Most likely impossible case unless you run these tests on embedded controllers
			return null;
		}
	}

	/**
	 * Reads an <code>InputStream</code> into an array of bytes. Uses 10 kilobytes buffer by default.
	 *
	 * @param is a stream to read from
	 * @return the result
	 */
	public static byte[] readInputStreamToBytes(InputStream is) throws IOException {
		return readInputStreamToBytes(is, READ_BUFFER);
	}

	/**
	 * Reads an <code>InputStream</code> into an array of bytes.
	 *
	 * @param is         a stream to read from
	 * @param bufferSize size of read buffer in bytes
	 * @return the result
	 */
	public static byte[] readInputStreamToBytes(InputStream is, int bufferSize) throws IOException {
		ReadableByteChannel channel = Channels.newChannel(is);
		ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int read;
		while ((read = channel.read(buffer)) > 0) {
			baos.write(buffer.array(), 0, read);
			buffer.clear();
		}

		return baos.toByteArray();
	}

	/**
	 * Locates and reads a file either by a direct path or by a relative path in classpath.
	 *
	 * @param file a file to locate and read
	 * @return file data and type
	 * @throws IOException in case of read error or file not found
	 */
	public static TypeAwareByteSource getFile(File file) throws IOException {
		byte[] data;
		if (file.exists() && file.isFile()) {
			data = readInputStreamToBytes(new FileInputStream(file));
		} else {
			String path = file.getPath();
			InputStream resource = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
			if (resource == null) {
				throw new FileNotFoundException("Unable to locate file of path: " + file.getPath());
			}
			data = readInputStreamToBytes(resource);
		}
		String name = file.getName();
		ByteSource byteSource = ByteSource.wrap(data);
		return new TypeAwareByteSource(byteSource, MimeTypeDetector.detect(byteSource, name));
	}
}
