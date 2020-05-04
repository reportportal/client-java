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
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class GoogleAnalyticsTest {

	private final Scheduler scheduler = Schedulers.from(Executors.newSingleThreadExecutor());
	private final HttpClient httpClient = mock(HttpClient.class);

	@Test
	void send() throws IOException {

		when(httpClient.execute(any(HttpPost.class))).thenReturn(new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_0,
				200,
				"OK"
		)));

		GoogleAnalytics googleAnalytics = new GoogleAnalytics(scheduler, "id", httpClient);
		HttpResponse httpResponse = googleAnalytics.send(new AnalyticsEvent(null, null, null)).blockingGet();

		verify(httpClient, times(1)).execute(any(HttpPost.class));

		assertEquals(200, httpResponse.getStatusLine().getStatusCode());
		assertEquals("OK", httpResponse.getStatusLine().getReasonPhrase());
	}
}