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

package com.epam.reportportal.service.statistics;

import com.epam.reportportal.service.statistics.item.StatisticsItem;
import com.epam.reportportal.test.TestUtils;
import io.reactivex.Maybe;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.mockito.ArgumentCaptor;
import retrofit2.Response;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class StatisticsIdsRunnable {
	public static void main(String... args) {

		StatisticsApiClient api = mock(StatisticsApiClient.class);
		when(api.send(
				anyString(),
				anyString(),
				anyString(),
				any(StatisticsItem.class)
		)).thenReturn(Maybe.create(e -> e.onSuccess(Response.success(ResponseBody.create("", MediaType.get("text/plain"))))));

		try (StatisticsClient client = new StatisticsClient("id", "secret", api)) {
			try (StatisticsService service = new StatisticsService(TestUtils.standardParameters(), client)) {
				service.sendEvent(Maybe.just("launch_id"),
						TestUtils.standardLaunchRequest(TestUtils.standardParameters())
				);
			}
			ArgumentCaptor<StatisticsItem> captor = ArgumentCaptor.forClass(StatisticsItem.class);
			verify(api).send(anyString(), anyString(), anyString(), captor.capture());

			System.out.println("cid=" + captor.getValue().getClientId());
		}
	}
}
