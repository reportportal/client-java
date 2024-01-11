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

import javax.annotation.Nonnull;
import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

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
	 * Reads an {@link InputStream} into a <code>String</code>. Uses UTF-8 encoding and 10 kilobytes buffer by
	 * default.
	 *
	 * @param is a stream to read from
	 * @return the result
	 * @throws IOException in case of a read error
	 */
	@Nonnull
	public static String readInputStreamToString(@Nonnull InputStream is) throws IOException {
		byte[] bytes = readInputStreamToBytes(is);
		if (bytes.length <= 0) {
			return "";
		}

		return new String(bytes, StandardCharsets.UTF_8);
	}

	/**
	 * Reads an {@link InputStream} into an array of bytes. Uses 10 kilobytes buffer by default.
	 *
	 * @param is a stream to read from
	 * @return the result
	 * @throws IOException in case of a read error
	 */
	public static byte[] readInputStreamToBytes(@Nonnull InputStream is) throws IOException {
		return readInputStreamToBytes(is, READ_BUFFER);
	}

	/**
	 * Reads an {@link InputStream} into an array of bytes.
	 *
	 * @param is         a stream to read from
	 * @param bufferSize size of read buffer in bytes
	 * @return the result
	 * @throws IOException in case of a read error
	 */
	public static byte[] readInputStreamToBytes(@Nonnull InputStream is, int bufferSize) throws IOException {
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
	 * Reads a {@link File} into an array of bytes.
	 *
	 * @param file a file to read
	 * @return the result
	 * @throws IOException in case of a read error, or a file not found
	 */
	public static byte[] readFileToBytes(@Nonnull File file) throws IOException {
		return readInputStreamToBytes(Files.newInputStream(file.toPath()));
	}

	/**
	 * Returns an input stream for reading the specified resource.
	 *
	 * @param path resource name or path to the resource
	 * @return readable stream for the resource
	 * @throws FileNotFoundException if no resource found
	 */
	@Nonnull
	public static InputStream getResourceAsStream(@Nonnull String path) throws FileNotFoundException {
		InputStream resource = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
		if (resource == null) {
			resource = Utils.class.getResourceAsStream(path);
			if (resource == null) {
				throw new FileNotFoundException("Unable to locate file of path: " + path);
			}
		}
		return resource;
	}

	/**
	 * Finds a resource with a given name.
	 *
	 * @param path resource name or path to the resource
	 * @return location reference
	 * @throws FileNotFoundException if no resource found
	 */
	@Nonnull
	public static URL getResource(@Nonnull String path) throws FileNotFoundException {
		URL resource = Thread.currentThread().getContextClassLoader().getResource(path);
		if (resource == null) {
			resource = Utils.class.getResource(path);
			if (resource == null) {
				throw new FileNotFoundException("Unable to locate file of path: " + path);
			}
		}
		return resource;
	}

	/**
	 * Locates and reads a file either by a direct path or by a relative path in classpath.
	 *
	 * @param file a file to locate and read
	 * @return file data and type
	 * @throws IOException in case of a read error, or a file not found
	 */
	public static TypeAwareByteSource getFile(@Nonnull File file) throws IOException {
		byte[] data;
		if (file.exists() && file.isFile()) {
			data = readFileToBytes(file);
		} else {
			data = readInputStreamToBytes(getResourceAsStream(file.getPath()));
		}
		String name = file.getName();
		ByteSource byteSource = ByteSource.wrap(data);
		return new TypeAwareByteSource(byteSource, MimeTypeDetector.detect(byteSource, name));
	}
}
