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
package com.epam.reportportal.message;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;

import java.io.File;
import java.net.URL;
import java.util.List;

import static com.epam.reportportal.utils.MimeTypeDetector.detect;
import static com.google.common.io.Files.asByteSource;
import static com.google.common.io.Resources.getResource;

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
            public TypeAwareByteSource toByteSource(String data) {
                File file = new File(data);
                if (!file.exists()) {
                    return null;
                }
                return new TypeAwareByteSource(asByteSource(file), detect(file));
            }
        },
        BASE64 {
            @Override
            public TypeAwareByteSource toByteSource(final String data) {
                if (data.contains(":")) {
                    final String[] parts = data.split(":");
                    String type = parts[1];
                    return new TypeAwareByteSource(ByteSource.wrap(BaseEncoding.base64().decode(parts[0])), type);

                }
                final ByteSource source = ByteSource.wrap(BaseEncoding.base64().decode(data));
                return new TypeAwareByteSource(source, detect(source, null));
            }
        },
        RESOURCE {
            @Override
            public TypeAwareByteSource toByteSource(String resourceName) {
                URL resource = getResource(resourceName);
                if (null == resource) {
                    return null;
                }
                final ByteSource source = Resources.asByteSource(resource);
                return new TypeAwareByteSource(source, detect(source, resourceName));
            }
        };

        abstract public TypeAwareByteSource toByteSource(String data);

        public static MessageType fromString(String messageType) {
            return MessageType.valueOf(messageType);
        }
    }

    private static final int CHUNKS_COUNT = 4;

    @Override
    public ReportPortalMessage parse(String message) {
        List<String> split = Splitter.on("#").limit(CHUNKS_COUNT).splitToList(message);

        // -1 because there may be no
        if (CHUNKS_COUNT != split.size()) {
            throw new RuntimeException(
                    "Incorrect message format. Chunks: " + Joiner.on("\n").join(split) + "\n count: " + split.size());
        }
        return new ReportPortalMessage(MessageType.fromString(split.get(1)).toByteSource(split.get(2)),
                split.get(3));
    }

    @Override
    public boolean supports(String message) {
        return message.startsWith(RP_MESSAGE_PREFIX);
    }
}
