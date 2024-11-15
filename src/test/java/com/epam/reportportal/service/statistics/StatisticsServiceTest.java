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

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.statistics.item.StatisticsEvent;
import com.epam.reportportal.service.statistics.item.StatisticsItem;
import com.epam.reportportal.test.TestUtils;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.reportportal.util.test.ProcessUtils;
import com.epam.reportportal.utils.files.Utils;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import io.reactivex.Maybe;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import retrofit2.Response;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

public class StatisticsServiceTest {

	private static class TestStatisticsService extends StatisticsService {
		private Statistics statistics;

		public TestStatisticsService(ListenerParameters listenerParameters) {
			super(listenerParameters);
		}

		public void setStatistics(Statistics statistics) {
			this.statistics = statistics;
		}

		@Override
		protected Statistics getStatistics() {
			return statistics;
		}
	}

	private static final String SEMANTIC_VERSION_PATTERN = "(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?";

	@Mock
	private StatisticsApiClient httpClient;

	@Mock
	private Statistics statistics;

	private final Maybe<String> launchMaybe = Maybe.create(emitter -> {
		Thread.sleep(CommonUtils.MINIMAL_TEST_PAUSE);
		emitter.onSuccess("launchId");
		emitter.onComplete();
	});

	private TestStatisticsService service;
	private ListenerParameters parameters;

	private void stubSend() {
		when(statistics.send(any())).thenReturn(Maybe.create(e -> e.onSuccess(Response.success(ResponseBody.create(
				"",
				MediaType.get("text/plain")
		)))));
	}

	@BeforeEach
	public void setup() {
		parameters = TestUtils.standardParameters();
		service = new TestStatisticsService(parameters);
		service.setStatistics(statistics);
	}

	@Test
	public void test_statistics_send_event_with_agent() {
		stubSend();
		StartLaunchRQ launchRq = TestUtils.standardLaunchRequest(parameters);
		launchRq.setAttributes(Collections.singleton(new ItemAttributesRQ("agent", "agent-java-testng|test-version-1", true)));

		service.sendEvent(launchMaybe, launchRq);
		service.close();

		ArgumentCaptor<StatisticsItem> argumentCaptor = ArgumentCaptor.forClass(StatisticsItem.class);
		verify(statistics, times(1)).send(argumentCaptor.capture());

		StatisticsItem item = argumentCaptor.getValue();
		assertThat(item.getClientId(), not(emptyString()));
		assertThat(item.getEvents(), hasSize(1));

		StatisticsEvent event = item.getEvents().get(0);
		assertThat(event.getName(), sameInstance(StatisticsService.START_LAUNCH_EVENT_ACTION));

		Map<String, Object> params = event.getParams();
		assertThat(params.get(StatisticsService.CLIENT_NAME_PARAM), anyOf(equalTo("${name}"), equalTo("client-java")));
		assertThat(
				params.get(StatisticsService.CLIENT_VERSION_PARAM).toString(),
				anyOf(equalTo("${version}"), matchesRegex(SEMANTIC_VERSION_PATTERN))
		);
		assertThat(params.get(StatisticsService.AGENT_NAME_PARAM), equalTo("agent-java-testng"));
		assertThat(params.get(StatisticsService.AGENT_VERSION_PARAM), equalTo("test-version-1"));
		assertThat(params.get(StatisticsService.INTERPRETER_PARAM), equalTo("Java " + System.getProperty("java.version")));
	}

	@Test
	public void test_statistics_send_event_no_agent_record() {
		stubSend();
		StartLaunchRQ launchRq = TestUtils.standardLaunchRequest(parameters);

		service.sendEvent(launchMaybe, launchRq);
		service.close();

		ArgumentCaptor<StatisticsItem> argumentCaptor = ArgumentCaptor.forClass(StatisticsItem.class);
		verify(statistics, times(1)).send(argumentCaptor.capture());

		StatisticsItem item = argumentCaptor.getValue();
		assertThat(item.getClientId(), not(emptyString()));
		assertThat(item.getEvents(), hasSize(1));

		StatisticsEvent event = item.getEvents().get(0);
		assertThat(event.getName(), sameInstance(StatisticsService.START_LAUNCH_EVENT_ACTION));

		Map<String, Object> params = event.getParams();

		assertThat(params.get(StatisticsService.CLIENT_NAME_PARAM), anyOf(equalTo("${name}"), equalTo("client-java")));
		assertThat(
				params.get(StatisticsService.CLIENT_VERSION_PARAM).toString(),
				anyOf(equalTo("${version}"), matchesRegex(SEMANTIC_VERSION_PATTERN))
		);
		assertThat(params, not(hasKey(StatisticsService.AGENT_NAME_PARAM)));
		assertThat(params, not(hasKey(StatisticsService.AGENT_VERSION_PARAM)));
		assertThat(params.get(StatisticsService.INTERPRETER_PARAM), equalTo("Java " + System.getProperty("java.version")));
	}

	@Test
	public void test_statistics_send_event_async() {
		stubSend();
		StartLaunchRQ launchRq = TestUtils.standardLaunchRequest(parameters);
		service.sendEvent(launchMaybe, launchRq);
		verify(statistics, timeout(2000).times(1)).send(any());
	}

	@Test
	public void verify_service_sends_same_client_id() {
		when(httpClient.send(anyString(), anyString(), anyString(), any(StatisticsItem.class))).thenReturn(Maybe.create(e -> e.onSuccess(
				Response.success(ResponseBody.create("", MediaType.get("text/plain"))))));
		String cid;
		try (StatisticsClient client = new StatisticsClient("id", "secret", httpClient)) {
			try (StatisticsService service = new StatisticsService(TestUtils.standardParameters(), client)) {
				service.sendEvent(launchMaybe, TestUtils.standardLaunchRequest(TestUtils.standardParameters()));
			}
			ArgumentCaptor<StatisticsItem> firstCaptor = ArgumentCaptor.forClass(StatisticsItem.class);
			verify(httpClient).send(anyString(), anyString(), anyString(), firstCaptor.capture());

			StatisticsItem item1 = firstCaptor.getValue();
			cid = item1.getClientId();
		}
		StatisticsApiClient secondClient = mock(StatisticsApiClient.class);
		when(secondClient.send(anyString(), anyString(), anyString(), any())).thenReturn(Maybe.create(e -> e.onSuccess(Response.success(
				ResponseBody.create("", MediaType.get("text/plain"))))));

		try (StatisticsClient client = new StatisticsClient("id", "secret", secondClient)) {
			try (StatisticsService service = new StatisticsService(TestUtils.standardParameters(), client)) {
				service.sendEvent(launchMaybe, TestUtils.standardLaunchRequest(TestUtils.standardParameters()));
			}

			ArgumentCaptor<StatisticsItem> secondCaptor = ArgumentCaptor.forClass(StatisticsItem.class);
			verify(secondClient).send(anyString(), anyString(), anyString(), secondCaptor.capture());

			StatisticsItem item2 = secondCaptor.getValue();

			assertThat(item2.getClientId(), allOf(notNullValue(), equalTo(cid)));
		}
	}

	@Test
	public void verify_service_sends_same_client_id_for_processes() throws IOException, InterruptedException {
		Map<String, String> homeProperty = Collections.singletonMap(
				"user.home",
				System.getProperty("user.dir") + File.separator + "same_client_id_test"
		);
		Process process = ProcessUtils.buildProcess(false, StatisticsIdsRunnable.class, null, homeProperty);
		assertThat("Exit code should be '0'", process.waitFor(), equalTo(0));
		String result = Utils.readInputStreamToString(process.getInputStream());
		process.destroyForcibly();
		Map<String, String> values = Arrays.stream(result.split(System.lineSeparator()))
				.collect(Collectors.toMap(k -> k.substring(0, k.indexOf("=")), v -> v.substring(v.indexOf("=") + 1)));

		Process process2 = ProcessUtils.buildProcess(false, StatisticsIdsRunnable.class, null, homeProperty);
		assertThat("Exit code should be '0'", process2.waitFor(), equalTo(0));
		String result2 = Utils.readInputStreamToString(process2.getInputStream());
		Map<String, String> values2 = Arrays.stream(result2.split(System.lineSeparator()))
				.collect(Collectors.toMap(k -> k.substring(0, k.indexOf("=")), v -> v.substring(v.indexOf("=") + 1)));

		assertThat(values2.get("cid"), equalTo(values.get("cid")));
	}
}
