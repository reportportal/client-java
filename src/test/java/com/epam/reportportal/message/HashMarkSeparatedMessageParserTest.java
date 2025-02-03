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
package com.epam.reportportal.message;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * @author Andrei Varabyeu
 */
public class HashMarkSeparatedMessageParserTest {

	@Test
	public void testParser() throws IOException {
		MessageParser parser = new HashMarkSeparatedMessageParser();

		ReportPortalMessage message = parser.parse("RP_MESSAGE#FILE#c:\\somedemofile#demo test message######33");
		assertThat(message, notNullValue());
		assertThat(message.getMessage(), equalTo("demo test message######33"));
		assertThat(message.getData(), nullValue());
	}

	@Test
	public void testParserEmptyMessage() throws IOException {
		MessageParser parser = new HashMarkSeparatedMessageParser();

		ReportPortalMessage message = parser.parse("RP_MESSAGE#FILE#c:\\somedemofile#");
		assertThat(message, notNullValue());
		assertThat(message.getMessage(), emptyString());
		assertThat(message.getData(), nullValue());
	}

	@Test
	public void testParserNullMessage() {
		MessageParser parser = new HashMarkSeparatedMessageParser();

		Assertions.assertThrows(RuntimeException.class, () -> parser.parse("RP_MESSAGE#FILE#c:\\somedemofile"));
	}

	@Test
	public void testParserBase64() throws IOException {
		MessageParser parser = new HashMarkSeparatedMessageParser();

		// Read the file and encode it to Base64
		File file = new File("src/test/resources/pug/lucky.jpg");
		byte[] fileContent = Files.readAllBytes(file.toPath());
		String base64Content = Base64.getEncoder().encodeToString(fileContent);

		// Create the message string
		String message = "RP_MESSAGE#BASE64#" + base64Content + "#demo test message";

		// Parse the message
		ReportPortalMessage reportPortalMessage = parser.parse(message);

		// Validate the parsed message
		assertThat(reportPortalMessage, notNullValue());
		assertThat(reportPortalMessage.getMessage(), equalTo("demo test message"));
		assertThat(reportPortalMessage.getData(), notNullValue());
		assertThat(reportPortalMessage.getData().read(), equalTo(fileContent));
	}

	@Test
	public void testParserBase64WithColon() throws IOException {
		MessageParser parser = new HashMarkSeparatedMessageParser();

		// Read the file and encode it to Base64
		File file = new File("src/test/resources/pug/lucky.jpg");
		byte[] fileContent = Files.readAllBytes(file.toPath());
		String base64Content = Base64.getEncoder().encodeToString(fileContent) + ":image/jpeg";

		// Create the message string
		String message = "RP_MESSAGE#BASE64#" + base64Content + "#demo test message with colon";

		// Parse the message
		ReportPortalMessage reportPortalMessage = parser.parse(message);

		// Validate the parsed message
		assertThat(reportPortalMessage, notNullValue());
		assertThat(reportPortalMessage.getMessage(), equalTo("demo test message with colon"));
		assertThat(reportPortalMessage.getData(), notNullValue());
		assertThat(reportPortalMessage.getData().read(), equalTo(fileContent));
		assertThat(reportPortalMessage.getData().getMediaType(), equalTo("image/jpeg"));
	}
}
