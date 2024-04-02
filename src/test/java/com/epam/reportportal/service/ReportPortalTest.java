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
package com.epam.reportportal.service;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.test.TestUtils;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.reportportal.util.test.SocketUtils;
import com.epam.ta.reportportal.ws.model.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRS;
import io.reactivex.Maybe;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.epam.reportportal.test.TestUtils.*;
import static com.epam.reportportal.util.test.CommonUtils.shutdownExecutorService;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

public class ReportPortalTest {
	static {
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
	}

	private static final String COOKIE = "AWSALB=P7cqG8g/K70xHAKOUPrWrG0XgmhG8GJNinj8lDnKVyITyubAen2lBr+fSa/e2JAoGksQphtImp49rZxc41qdqUGvAc67SdZHY1BMFIHKzc8kyWc1oQjq6oI+s39U";

	@Mock
	private ReportPortalClient rpClient;

	@Test
	public void verify_no_url_results_in_null_client() {
		ListenerParameters listenerParameters = new ListenerParameters();
		assertThat(ReportPortal.builder().defaultClient(listenerParameters), nullValue());
	}

	@Test
	public void verify_correct_url_results_in_not_null_client() {
		ListenerParameters listenerParameters = new ListenerParameters();
		listenerParameters.setBaseUrl("http://localhost");
		assertThat(ReportPortal.builder().defaultClient(listenerParameters), notNullValue());
	}

	@Test
	public void verify_no_url_results_in_noop_launch() {
		ListenerParameters listenerParameters = new ListenerParameters();
		ReportPortal rp = ReportPortal.builder().withParameters(listenerParameters).build();
		Launch launch = rp.newLaunch(TestUtils.standardLaunchRequest(listenerParameters));
		assertThat(launch, sameInstance(Launch.NOOP_LAUNCH));
	}

	@Test
	public void verify_correct_url_results_in_correct_launch() {
		ListenerParameters listenerParameters = new ListenerParameters();
		listenerParameters.setBaseUrl("http://localhost");
		listenerParameters.setEnable(true);
		ReportPortal rp = ReportPortal.builder().withParameters(listenerParameters).build();
		Launch launch = rp.newLaunch(TestUtils.standardLaunchRequest(listenerParameters));
		assertThat(launch, not(sameInstance(Launch.NOOP_LAUNCH)));
	}

	@Test
	public void verify_proxy_parameter_works() throws Exception {
		ServerSocket server = SocketUtils.getServerSocketOnFreePort();
		ListenerParameters params = standardParameters();
		@SuppressWarnings("HttpUrlsUsage")
		String baseUrl = "http://example.com:8080";
		params.setBaseUrl(baseUrl);
		params.setProxyUrl("http://localhost:" + server.getLocalPort());
		OkHttpClient client = ReportPortal.builder().defaultClient(params);
		assertThat(client, notNullValue());
		Exception error = null;
		try {
			SocketUtils.ServerCallable serverCallable = new SocketUtils.ServerCallable(server,
					Collections.emptyMap(),
					"files/simple_response.txt"
			);
			Pair<List<String>, Response> result = SocketUtils.executeServerCallable(serverCallable,
					() -> client.newCall(new Request.Builder().url(baseUrl).build()).execute()
			);
			assertThat(result.getValue().code(), equalTo(200));
		} catch (Exception e) {
			error = e;
		}
		server.close();
		if (error != null) {
			throw error;
		}
	}

	@Test
	public void verify_proxy_credential_works() throws Exception {
		String userName = "user";
		String password = "password";
		String expectedAuth =
				"Authorization: Basic " + Base64.getEncoder().encodeToString((userName + ":" + password).getBytes(StandardCharsets.UTF_8))
						+ System.lineSeparator();
		ServerSocket server = SocketUtils.getServerSocketOnFreePort();
		ListenerParameters params = standardParameters();
		@SuppressWarnings("HttpUrlsUsage")
		String baseUrl = "http://example.com:8080";
		params.setBaseUrl(baseUrl);
		params.setProxyUrl("http://localhost:" + server.getLocalPort());
		params.setProxyUser(userName);
		params.setProxyPassword(password);
		OkHttpClient client = ReportPortal.builder().defaultClient(params);
		assertThat(client, notNullValue());
		Exception error = null;
		try {
			SocketUtils.ServerCallable serverCallable = new SocketUtils.ServerCallable(server,
					Collections.emptyMap(),
					Arrays.asList("files/proxy_auth_response.txt", "files/simple_response.txt")
			);
			Pair<List<String>, Response> proxyAuth = SocketUtils.executeServerCallable(serverCallable,
					() -> client.newCall(new Request.Builder().url(baseUrl).build()).execute()
			);
			assertThat(proxyAuth.getValue().code(), equalTo(200));
			assertThat(proxyAuth.getKey(), hasSize(2));
			assertThat(proxyAuth.getKey().get(1), containsString(expectedAuth));
		} catch (Exception e) {
			error = e;
		}
		server.close();
		if (error != null) {
			throw error;
		}
	}

	@Test
	public void verify_rp_client_saves_and_bypasses_cookies() throws Exception {
		ServerSocket ss = SocketUtils.getServerSocketOnFreePort();
		ListenerParameters parameters = standardParameters();
		parameters.setHttpLogging(true);
		parameters.setBaseUrl("http://localhost:" + ss.getLocalPort());
		ExecutorService clientExecutor = Executors.newSingleThreadExecutor();
		ReportPortalClient rpClient = ReportPortal.builder().buildClient(ReportPortalClient.class, parameters, clientExecutor);
		Exception error = null;
		try {
			Map<String, Object> model = new HashMap<>();
			model.put("cookie", COOKIE);
			SimpleDateFormat sdf = new SimpleDateFormat(SocketUtils.WEB_DATE_FORMAT, Locale.US);
			sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
			Calendar cal = Calendar.getInstance();
			model.put("date", sdf.format(cal.getTime()));
			cal.add(Calendar.MINUTE, 2);
			model.put("expire", sdf.format(cal.getTime()));

			SocketUtils.ServerCallable serverCallable = new SocketUtils.ServerCallable(ss, model, "files/socket_response.txt");
			Callable<StartLaunchRS> clientCallable = () -> rpClient.startLaunch(new StartLaunchRQ())
					.timeout(5, TimeUnit.SECONDS)
					.blockingGet();
			Pair<List<String>, StartLaunchRS> result = SocketUtils.executeServerCallable(serverCallable, clientCallable);

			assertThat(result.getValue(), notNullValue());
			assertThat("First request should not contain cookie value", result.getKey().get(0), not(containsString(COOKIE)));

			result = SocketUtils.executeServerCallable(serverCallable, clientCallable);

			assertThat(result.getValue(), notNullValue());
			assertThat("Second request should contain cookie value", result.getKey().get(0), containsString(COOKIE));
		} catch (Exception e) {
			error = e;
		}
		ss.close();
		shutdownExecutorService(clientExecutor);
		if (error != null) {
			throw error;
		}
	}

	@Test
	public void verify_timeout_properties_bypass() {
		ListenerParameters listenerParameters = new ListenerParameters();
		listenerParameters.setBaseUrl("http://localhost");
		Duration defaultTimeout = Duration.ofSeconds(1);
		listenerParameters.setHttpCallTimeout(defaultTimeout);
		listenerParameters.setHttpConnectTimeout(defaultTimeout);
		listenerParameters.setHttpReadTimeout(defaultTimeout);
		listenerParameters.setHttpWriteTimeout(defaultTimeout);

		int defaultTimeoutMs = 1000;
		OkHttpClient client = ReportPortal.builder().defaultClient(listenerParameters);
		assertThat(client, notNullValue());
		assertThat(client.callTimeoutMillis(), equalTo(defaultTimeoutMs));
		assertThat(client.connectTimeoutMillis(), equalTo(defaultTimeoutMs));
		assertThat(client.readTimeoutMillis(), equalTo(defaultTimeoutMs));
		assertThat(client.writeTimeoutMillis(), equalTo(defaultTimeoutMs));
	}

	@Test
	public void verify_launch_uuid_parameter_handling() {
		simulateStartTestItemResponse(rpClient);

		String launchUuid = "test-launch-uuid";
		ListenerParameters listenerParameters = TestUtils.standardParameters();
		listenerParameters.setLaunchUuid(launchUuid);

		ExecutorService executor = Executors.newSingleThreadExecutor();
		RuntimeException error = null;

		try {
			ReportPortal rp = ReportPortal.create(rpClient, listenerParameters, executor);

			Launch launch = rp.newLaunch(standardLaunchRequest(listenerParameters));
			Maybe<String> launchMaybe = launch.start();
			assertThat(launchMaybe.blockingGet(), equalTo(launchUuid));

			//noinspection ReactiveStreamsUnusedPublisher
			launch.startTestItem(TestUtils.standardStartSuiteRequest());

			verify(rpClient, timeout(1000).times(0)).startLaunch(any(StartLaunchRQ.class));

			ArgumentCaptor<StartTestItemRQ> startCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
			verify(rpClient, timeout(1000)).startTestItem(startCaptor.capture());

			StartTestItemRQ startItem = startCaptor.getValue();
			assertThat(startItem.getLaunchUuid(), equalTo(launchUuid));

			launch.finish(TestUtils.standardLaunchFinishRequest());

			verify(rpClient, timeout(1000).times(0)).finishLaunch(anyString(), any(FinishExecutionRQ.class));
		} catch (RuntimeException e) {
			error = e;
		}
		CommonUtils.shutdownExecutorService(executor);
		if (error != null) {
			throw error;
		}
	}
}
