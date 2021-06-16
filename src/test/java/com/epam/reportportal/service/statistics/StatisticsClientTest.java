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
import io.reactivex.Maybe;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import retrofit2.Response;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class StatisticsClientTest {

	private final StatisticsClient httpClient = mock(StatisticsClient.class);

	@Test
	void sendRequestWithoutError() {

		when(httpClient.send(any())).thenReturn(Maybe.create(e-> e.onSuccess(Response.success(ResponseBody.create(MediaType.get("text/plain"), "")))));

		StatisticsService googleAnalytics = new StatisticsService("id", httpClient);
		Maybe<Response<ResponseBody>> result = googleAnalytics.send(new AnalyticsEvent(null, null, null));

		verify(httpClient).send(any(Map.class));

		assertNotNull(result);
	}

	@Test
	void sendRequestErrorShouldNotThrowException() {

		when(httpClient.send(any())).thenReturn(Maybe.error(new RuntimeException("Internal error")));

		StatisticsService googleAnalytics = new StatisticsService("id", httpClient);
		Maybe<Response<ResponseBody>> result = googleAnalytics.send(new AnalyticsEvent(null, null, null));

		verify(httpClient, times(1)).send(any(Map.class));

		Assertions.assertThrows(RuntimeException.class, () -> result.blockingGet());
	}
}