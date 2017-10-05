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
package com.epam.reportportal.message;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteSource;

import java.io.IOException;
import java.io.InputStream;

/**
 * {@link ByteSource} which knows about content mime type
 *
 * @author Andrei Varabyeu
 */
public class TypeAwareByteSource extends ByteSource {

	private final ByteSource delegate;
	private final String mediaType;

	public TypeAwareByteSource(ByteSource delegate, String mediaType) {
		this.delegate = Preconditions.checkNotNull(delegate);
		this.mediaType = mediaType;
	}

	@Override
	public InputStream openStream() throws IOException {
		return delegate.openStream();
	}

	public String getMediaType() {
		return mediaType;
	}
}
