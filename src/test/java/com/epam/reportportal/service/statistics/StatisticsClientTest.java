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

import com.epam.reportportal.service.statistics.item.StatisticsEvent;
import com.epam.reportportal.util.test.ProcessUtils;
import com.epam.reportportal.utils.files.Utils;
import io.reactivex.Maybe;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import retrofit2.Response;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class StatisticsClientTest {

	private final StatisticsApiClient httpClient = mock(StatisticsApiClient.class);

	@Test
	void sendRequestWithoutError() {

		when(httpClient.send(any())).thenReturn(Maybe.create(e -> e.onSuccess(Response.success(ResponseBody.create(MediaType.get(
				"text/plain"), "")))));

		StatisticsClient googleAnalytics = new StatisticsClient("id", httpClient);
		Maybe<Response<ResponseBody>> result = googleAnalytics.send(new StatisticsEvent(null, null, null));

		//noinspection unchecked
		verify(httpClient).send(any(Map.class));

		assertNotNull(result);
	}

	@Test
	void sendRequestErrorShouldNotThrowException() {

		when(httpClient.send(any())).thenReturn(Maybe.error(new RuntimeException("Internal error")));

		StatisticsClient googleAnalytics = new StatisticsClient("id", httpClient);
		Maybe<Response<ResponseBody>> result = googleAnalytics.send(new StatisticsEvent(null, null, null));

		//noinspection unchecked
		verify(httpClient).send(any(Map.class));

		//noinspection ResultOfMethodCallIgnored
		Assertions.assertThrows(RuntimeException.class, result::blockingGet);
	}

	@Test
	public void verify_client_sends_same_client_id_and_different_user_ids() {
		when(httpClient.send(any())).thenReturn(Maybe.create(e -> e.onSuccess(Response.success(ResponseBody.create(MediaType.get(
				"text/plain"), "")))));
		StatisticsClient googleAnalytics = new StatisticsClient("id", httpClient);
		Maybe<Response<ResponseBody>> result = googleAnalytics.send(new StatisticsEvent(null, null, null));
		//noinspection ResultOfMethodCallIgnored
		result.blockingGet();

		//noinspection rawtypes
		ArgumentCaptor<Map> firstCaptor = ArgumentCaptor.forClass(Map.class);
		//noinspection unchecked
		verify(httpClient).send(firstCaptor.capture());
		String cid = firstCaptor.getValue().get("cid").toString();
		String uid = firstCaptor.getValue().get("uid").toString();

		StatisticsApiClient secondClient = mock(StatisticsApiClient.class);
		when(secondClient.send(any())).thenReturn(Maybe.create(e -> e.onSuccess(Response.success(ResponseBody.create(MediaType.get(
				"text/plain"), "")))));

		googleAnalytics = new StatisticsClient("id",secondClient);
		result = googleAnalytics.send(new StatisticsEvent(null, null, null));
		//noinspection ResultOfMethodCallIgnored
		result.blockingGet();

		//noinspection rawtypes
		ArgumentCaptor<Map> secondCaptor = ArgumentCaptor.forClass(Map.class);
		//noinspection unchecked
		verify(secondClient).send(secondCaptor.capture());

		assertThat(secondCaptor.getValue().get("cid").toString(), equalTo(cid));
		assertThat(secondCaptor.getValue().get("uid").toString(), not(equalTo(uid)));
	}

	@Test
	public void verify_client_sends_same_client_id_and_different_user_ids_for_processes() throws IOException, InterruptedException {
		Process process = ProcessUtils.buildProcess(false, StatisticsIdsRunnable.class);
		assertThat("Exit code should be '0'", process.waitFor(), equalTo(0));
		String result = Utils.readInputStreamToString(process.getInputStream());
		process.destroyForcibly();
		Map<String, String> values = Arrays.stream(result.split(System.getProperty("line.separator")))
				.collect(Collectors.toMap(k -> k.substring(0, k.indexOf("=")), v -> v.substring(v.indexOf("=") + 1)));

		Process process2 = ProcessUtils.buildProcess(false, StatisticsIdsRunnable.class);
		assertThat("Exit code should be '0'", process2.waitFor(), equalTo(0));
		String result2 = Utils.readInputStreamToString(process2.getInputStream());
		Map<String, String> values2 = Arrays.stream(result2.split(System.getProperty("line.separator")))
				.collect(Collectors.toMap(k -> k.substring(0, k.indexOf("=")), v -> v.substring(v.indexOf("=") + 1)));

		assertThat(values2.get("cid"), equalTo(values.get("cid")));
		assertThat(values2.get("uid"), not(equalTo(values.get("uid"))));
	}
}
