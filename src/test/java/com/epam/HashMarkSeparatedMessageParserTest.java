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
package com.epam;

import java.io.IOException;

import com.epam.reportportal.message.HashMarkSeparatedMessageParser;
import com.epam.reportportal.message.MessageParser;
import com.epam.reportportal.message.ReportPortalMessage;
import org.junit.Assert;
import org.junit.Test;

/**
 * 
 * @author Andrei Varabyeu
 * 
 */
public class HashMarkSeparatedMessageParserTest {

	@Test
	public void testParser() throws IOException {
		MessageParser parser = new HashMarkSeparatedMessageParser();

		ReportPortalMessage message = parser.parse("RP_MESSAGE#FILE#c:\\somedemofile#demo test message######33");
		Assert.assertEquals("demo test message######33", message.getMessage());
		Assert.assertNotNull("Message should not be null", message);
		Assert.assertNull("Binary data should be nullMessage should be null", message.getData());
	}
}
