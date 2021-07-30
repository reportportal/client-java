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

import com.epam.reportportal.service.statistics.item.StatisticsEvent;
import io.reactivex.Maybe;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.mockito.ArgumentCaptor;
import retrofit2.Response;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class StatisticsIdsRunnable {
	public static void main(String... args) {

		StatisticsApiClient api = mock(StatisticsApiClient.class);
		when(api.send(anyString(), any())).thenReturn(Maybe.create(e -> e.onSuccess(Response.success(ResponseBody.create(
				MediaType.get("text/plain"),
				""
		)))));

		StatisticsClient client = new StatisticsClient("tid", api);
		Maybe<Response<ResponseBody>> result = client.send(new StatisticsEvent(null, null, null));
		//noinspection ResultOfMethodCallIgnored
		result.blockingGet();

		//noinspection rawtypes
		ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
		//noinspection unchecked
		verify(api).send(anyString(), captor.capture());

		System.out.println("cid=" + captor.getValue().get("cid").toString());
		System.out.println("uid=" + captor.getValue().get("uid").toString());
	}
}
