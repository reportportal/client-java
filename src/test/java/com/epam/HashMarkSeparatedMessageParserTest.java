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
package com.epam;

import com.epam.reportportal.message.HashMarkSeparatedMessageParser;
import com.epam.reportportal.message.MessageParser;
import com.epam.reportportal.message.ReportPortalMessage;
import org.junit.jupiter.api.Test;

import java.io.IOException;

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
}
