/*
 * Copyright 2020 EPAM Systems
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

package com.epam.reportportal.service.analytics;

import com.epam.reportportal.service.analytics.item.AnalyticsEvent;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class GoogleAnalyticsTest {

	private final HttpClient httpClient = mock(HttpClient.class);

	@Test
	void sendRequestWithoutError() throws IOException {

		when(httpClient.execute(any(HttpPost.class))).thenReturn(new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_0,
				200,
				"OK"
		)));

		StatisticsService googleAnalytics = new StatisticsService("id", httpClient);
		Boolean result = googleAnalytics.send(new AnalyticsEvent(null, null, null));

		verify(httpClient, times(1)).execute(any(HttpPost.class));

		assertTrue(result);
	}

	@Test
	void sendRequestErrorShouldNotThrowException() throws IOException {

		when(httpClient.execute(any(HttpPost.class))).thenThrow(new RuntimeException("Internal error"));

		StatisticsService googleAnalytics = new StatisticsService("id", httpClient);
		Boolean result = googleAnalytics.send(new AnalyticsEvent(null, null, null));

		verify(httpClient, times(1)).execute(any(HttpPost.class));

		assertFalse(result);
	}
}