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
package com.epam.reportportal.message;

import com.epam.reportportal.utils.files.ByteSource;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.epam.reportportal.utils.MimeTypeDetector.detect;
import static com.epam.reportportal.utils.files.Utils.getFile;
import static com.epam.reportportal.utils.files.Utils.getResource;

/**
 * Colon separated message parser. Expects string in the following format:<br>
 * RP_MESSAGE#FILE#FILENAME#MESSAGE_TEST<br>
 * RP_MESSAGE#BASE64#BASE_64_REPRESENTATION#MESSAGE_TEST<br>
 *
 * @author Andrei Varabyeu
 */
public class HashMarkSeparatedMessageParser implements MessageParser {

	/**
	 * Different representations of binary data
	 */
	private enum MessageType {
		FILE {
			@Override
			public TypeAwareByteSource toByteSource(String data) throws IOException {
				File file = new File(data);
				if (!file.exists()) {
					return null;
				}
				return getFile(file);
			}
		},
		BASE64 {
			@Override
			public TypeAwareByteSource toByteSource(final String data) throws IOException {
				if (data.contains(":")) {
					final String[] parts = data.split(":");
					String type = parts[1];
					return new TypeAwareByteSource(ByteSource.wrap(Base64.getDecoder().decode(parts[0])), type);

				}
				final ByteSource source = ByteSource.wrap(Base64.getDecoder().decode(data));
				return new TypeAwareByteSource(source, detect(source, null));
			}
		},
		RESOURCE {
			@Override
			public TypeAwareByteSource toByteSource(String resourceName) throws IOException {
				ByteSource source = new ByteSource(getResource(resourceName));
				return new TypeAwareByteSource(source, detect(source, resourceName));
			}
		};

		abstract public TypeAwareByteSource toByteSource(String data) throws IOException;

		public static MessageType fromString(String messageType) {
			return MessageType.valueOf(messageType);
		}
	}

	private static final int CHUNKS_COUNT = 4;
	private static final Pattern CHUNK_DELIMITER = Pattern.compile("#");

	@Override
	public ReportPortalMessage parse(String message) throws IOException {
		Matcher m = CHUNK_DELIMITER.matcher(Pattern.quote(message));
		int chunkIdx = 0;
		List<String> split = new ArrayList<>(CHUNKS_COUNT);
		int prevRegion = 0;
		while (m.find()) {
			String chunk = message.substring(prevRegion, m.start() - 2);
			prevRegion = m.start() - 1;
			if (!chunk.isEmpty()) {
				split.add(chunk);
			}
			if (++chunkIdx >= CHUNKS_COUNT - 1) {
				break;
			}
		}
		split.add(message.substring(prevRegion));

		// -1 because there may be no
		if (CHUNKS_COUNT != split.size()) {
			throw new RuntimeException("Incorrect message format. Chunks: " + String.join("\n", split) + "\n count: " + split.size());
		}
		return new ReportPortalMessage(MessageType.fromString(split.get(1)).toByteSource(split.get(2)), split.get(3));
	}

	@Override
	public boolean supports(String message) {
		return message.startsWith(RP_MESSAGE_PREFIX);
	}
}
