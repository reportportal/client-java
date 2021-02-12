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
import com.epam.reportportal.util.test.SocketUtils;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRS;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.epam.reportportal.test.TestUtils.standardParameters;
import static com.epam.reportportal.util.test.CommonUtils.shutdownExecutorService;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ReportPortalTest {
	private static final String COOKIE = "AWSALB=P7cqG8g/K70xHAKOUPrWrG0XgmhG8GJNinj8lDnKVyITyubAen2lBr+fSa/e2JAoGksQphtImp49rZxc41qdqUGvAc67SdZHY1BMFIHKzc8kyWc1oQjq6oI+s39U";

	@Test
	public void no_url_results_in_null_client() {
		ListenerParameters listenerParameters = new ListenerParameters();
		assertThat(ReportPortal.builder().defaultClient(listenerParameters), nullValue());
	}

	@Test
	public void correct_url_results_in_not_null_client() {
		ListenerParameters listenerParameters = new ListenerParameters();
		listenerParameters.setBaseUrl("http://localhost");
		assertThat(ReportPortal.builder().defaultClient(listenerParameters), notNullValue());
	}

	@Test
	public void no_url_results_in_noop_launch() {
		ListenerParameters listenerParameters = new ListenerParameters();
		ReportPortal rp = ReportPortal.builder().withParameters(listenerParameters).build();
		Launch launch = rp.newLaunch(TestUtils.standardLaunchRequest(listenerParameters));
		assertThat(launch, sameInstance(Launch.NOOP_LAUNCH));
	}

	@Test
	public void correct_url_results_in_correct_launch() {
		ListenerParameters listenerParameters = new ListenerParameters();
		listenerParameters.setBaseUrl("http://localhost");
		listenerParameters.setEnable(true);
		ReportPortal rp = ReportPortal.builder().withParameters(listenerParameters).build();
		Launch launch = rp.newLaunch(TestUtils.standardLaunchRequest(listenerParameters));
		assertThat(launch, not(sameInstance(Launch.NOOP_LAUNCH)));
	}

	@Test
	public void proxyParamBypass() throws Exception {
		String baseUrl = "http://example.com:8080";
		ServerSocket server = SocketUtils.getServerSocketOnFreePort();
		ListenerParameters params = standardParameters();
		params.setBaseUrl(baseUrl);
		params.setProxyUrl("http://localhost:" + server.getLocalPort());
		HttpClient client = ReportPortal.builder().defaultClient(params);
		try {
			SocketUtils.ServerCallable serverCallable = new SocketUtils.ServerCallable(server,
					Collections.emptyMap(),
					"files/simple_response.txt"
			);
			Pair<String, HttpResponse> result = SocketUtils.executeServerCallable(serverCallable,
					() -> client.execute(new HttpGet(baseUrl))
			);
			assertThat(result.getValue().getStatusLine().getStatusCode(), equalTo(200));
		} finally {
			server.close();
		}
	}

	@Test
	public void test_rp_client_saves_and_bypasses_cookies() throws Exception {
		ServerSocket ss = SocketUtils.getServerSocketOnFreePort();
		ListenerParameters parameters = standardParameters();
		parameters.setBaseUrl("http://localhost:" + ss.getLocalPort());
		ExecutorService clientExecutor = Executors.newSingleThreadExecutor();
		ReportPortalClient rpClient = ReportPortal.builder().buildClient(ReportPortalClient.class, parameters, clientExecutor);
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
			Pair<String, StartLaunchRS> result = SocketUtils.executeServerCallable(serverCallable, clientCallable);

			assertThat(result.getValue(), notNullValue());
			assertThat("First request should not contain cookie value", result.getKey(), not(containsString(COOKIE)));

			result = SocketUtils.executeServerCallable(serverCallable, clientCallable);

			assertThat(result.getValue(), notNullValue());
			assertThat("Second request should contain cookie value", result.getKey(), containsString(COOKIE));
		} finally {
			ss.close();
			shutdownExecutorService(clientExecutor);
		}
	}
}
