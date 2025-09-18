/*
 *  Copyright 2024 EPAM Systems
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.epam.reportportal.utils.files;

import jakarta.annotation.Nonnull;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;

/**
 * A readable stateful source of bytes. Use it as supplier for InputStreams.
 */
public class ByteSource {
	private volatile byte[] bytes;

	private final URL url;

	public ByteSource(@Nonnull ByteSource source) {
		this.url = source.url;
		byte[] myBytes = source.bytes;
		this.bytes = myBytes == null ? null : Arrays.copyOf(myBytes, myBytes.length);
	}

	private ByteSource(@Nonnull byte[] source) {
		this.url = null;
		this.bytes = Arrays.copyOf(source, source.length);
	}

	public ByteSource(@Nonnull URL sourceUrl) {
		this.url = sourceUrl;
		this.bytes = null;
	}

	/**
	 * Create an instance of the object from given bytes.
	 *
	 * @param source bytes to use to create the instance
	 * @return current class instance
	 */
	@Nonnull
	public static ByteSource wrap(@Nonnull byte[] source) {
		return new ByteSource(source);
	}

	/**
	 * Return new {@link InputStream} object which can be read and closed naturally.
	 *
	 * @return stream
	 * @throws IOException reading error or null byte source
	 */
	@Nonnull
	public InputStream openStream() throws IOException {
		if (bytes != null) {
			return new ByteArrayInputStream(bytes);
		}
		if (url != null) {
			return url.openStream();
		}
		throw new IOException("Unable to open a stream for null sources.");
	}

	/**
	 * Wrap result {@link InputStream} object into {@link BufferedInputStream} object.
	 *
	 * @return buffered stream
	 * @throws IOException reading error or null byte source
	 */
	@Nonnull
	public InputStream openBufferedStream() throws IOException {
		return new BufferedInputStream(openStream());
	}

	/**
	 * Read internal byte source and return it as byte array. The result will be cached if not cached already.
	 *
	 * @return read data
	 * @throws IOException reading error or null byte source
	 */
	@Nonnull
	public byte[] read() throws IOException {
		if (bytes != null) {
			return Arrays.copyOf(bytes, bytes.length);
		}
		if (url != null) {
			bytes = Utils.readInputStreamToBytes(url.openStream());
			return bytes;
		}
		throw new IOException("Unable to read null sources.");
	}
}
