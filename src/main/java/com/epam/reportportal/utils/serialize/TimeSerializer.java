/*
 * Copyright 2025 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.reportportal.utils.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Custom serializer for time values that handles both Date and Instant objects.
 * - Date objects are serialized to long values (milliseconds from epoch)
 * - Instant objects are serialized to ISO time format with milliseconds
 */
public class TimeSerializer extends JsonSerializer<Object> {

	private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ")
			.withZone(ZoneOffset.UTC);

	@Override
	public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		if (value == null) {
			gen.writeNull();
			return;
		}

		if (value instanceof Date) {
			// Serialize Date to milliseconds from epoch
			Date date = (Date) value;
			gen.writeNumber(date.getTime());
		} else if (value instanceof Instant) {
			// Serialize Instant to ISO time format with milliseconds
			Instant instant = (Instant) value;
			gen.writeString(ISO_FORMATTER.format(instant));
		} else if (value instanceof Long) {
			gen.writeNumber((Long) value);
		} else {
			// Fallback for other Comparable types - convert to string
			gen.writeString(value.toString());
		}
	}
}
