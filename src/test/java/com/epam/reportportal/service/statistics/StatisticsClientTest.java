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

package com.epam.reportportal.service.statistics;

import com.epam.reportportal.service.statistics.item.StatisticsItem;
import io.reactivex.Maybe;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import retrofit2.Response;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class StatisticsClientTest {

	@Mock
	private StatisticsApiClient httpClient;

	@Test
	void sendRequestWithoutError() {

		when(httpClient.send(anyString(), anyString(), anyString(), any(StatisticsItem.class))).thenReturn(Maybe.create(
				e -> e.onSuccess(Response.success(ResponseBody.create("", MediaType.get("text/plain"))))));

		try (StatisticsClient googleAnalytics = new StatisticsClient("id", "secret", httpClient)) {
			StatisticsItem item = new StatisticsItem("client-id");
			Maybe<Response<ResponseBody>> result = googleAnalytics.send(item);

			verify(httpClient).send(anyString(), eq("id"), eq("secret"), same(item));

			assertNotNull(result);
		}
	}

	@Test
	void sendRequestErrorShouldNotThrowException() {

		when(httpClient.send(anyString(), anyString(), anyString(), any(StatisticsItem.class))).thenReturn(Maybe.error(
				new RuntimeException("Internal error")));

		try (StatisticsClient googleAnalytics = new StatisticsClient("id", "secret", httpClient)) {
			Maybe<Response<ResponseBody>> result = googleAnalytics.send(new StatisticsItem("client-id"));

			verify(httpClient).send(anyString(), anyString(), anyString(), any(StatisticsItem.class));
			Assertions.assertThrows(RuntimeException.class, result::blockingGet);
		}
	}
}
