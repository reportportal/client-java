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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Date;

/**
 * Custom deserializer for time values that handles both numeric and string representations.
 * - Long/numeric values are deserialized to Date objects (milliseconds from epoch)
 * - String values are deserialized to Instant objects using ISO time format
 */
public class TimeDeserializer extends JsonDeserializer<Comparable<?>> {

	@Override
	public Comparable<? extends Comparable<?>> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		JsonToken token = p.getCurrentToken();

		if (token == JsonToken.VALUE_NULL) {
			return null;
		}

		if (token == JsonToken.VALUE_NUMBER_INT || token == JsonToken.VALUE_NUMBER_FLOAT) {
			// Deserialize numeric value to Date (milliseconds from epoch)
			long timestamp = p.getLongValue();
			return new Date(timestamp);
		}

		if (token == JsonToken.VALUE_STRING) {
			String value = p.getText();
			if (value.isBlank()) {
				return null;
			}

			try {
				return TimeSerializer.ISO_MICRO_FORMATTER.parse(value, Instant::from);
			} catch (DateTimeParseException e1) {
				try{
					return Instant.parse(value);
				} catch (DateTimeParseException e2) {
					// If ISO parsing fails, try to parse as long timestamp
					try {
						long timestamp = Long.parseLong(value);
						return new Date(timestamp);
					} catch (NumberFormatException nfe) {
						throw new IOException("Unable to parse time value: " + value, nfe);
					}
				}
			}
		}

		throw new IOException("Unexpected token for time deserialization: " + token);
	}
}
