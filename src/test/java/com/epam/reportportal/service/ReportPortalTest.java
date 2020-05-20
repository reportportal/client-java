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

import com.epam.reportportal.exception.InternalReportPortalClientException;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRS;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.*;

import static com.epam.reportportal.test.TestUtils.shutdownExecutorService;
import static com.epam.reportportal.test.TestUtils.standardParameters;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ReportPortalTest {
	private static final String WEB_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";
	private static final String COOKIE = "AWSALB=P7cqG8g/K70xHAKOUPrWrG0XgmhG8GJNinj8lDnKVyITyubAen2lBr+fSa/e2JAoGksQphtImp49rZxc41qdqUGvAc67SdZHY1BMFIHKzc8kyWc1oQjq6oI+s39U";

	@Test
	public void noUrlResultsInException() {
		ListenerParameters listenerParameters = new ListenerParameters();
		assertThrows(InternalReportPortalClientException.class, () -> ReportPortal.builder().defaultClient(listenerParameters));
	}

	private static final class ServerCallable implements Callable<String> {

		private final ServerSocket ss;
		private Socket s;

		public ServerCallable(ServerSocket serverSocket) {
			ss = serverSocket;
		}

		@Override
		public String call() throws Exception {
			if (s == null) {
				s = ss.accept();
			}
			BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			StringBuilder builder = new StringBuilder();
			String line;
			do {
				line = in.readLine();
				if (line.equals("")) {
					break;
				}
				builder.append(line);
				builder.append(System.lineSeparator());
			} while (true);
			String rq = builder.toString();
			SimpleDateFormat sdf = new SimpleDateFormat(WEB_DATE_FORMAT);
			sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.MINUTE, 2);
			String rs = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("files/socket_response.txt"))
					.replace("{date}", sdf.format(new Date()))
					.replace("{cookie}", COOKIE)
					.replace("{expire}", sdf.format(cal.getTime()));
			IOUtils.write(rs, s.getOutputStream());
			return rq;
		}
	}

	@Test
	public void test_rp_client_saves_and_bypasses_cookies() throws IOException, ExecutionException, InterruptedException, TimeoutException {
		ExecutorService serverExecutor = Executors.newSingleThreadExecutor();
		ServerSocket ss = new ServerSocket(0);
		ListenerParameters parameters = standardParameters();
		parameters.setBaseUrl("http://localhost:" + ss.getLocalPort());
		ExecutorService clientExecutor = Executors.newSingleThreadExecutor();
		ReportPortalClient rpClient = ReportPortal.builder().buildClient(ReportPortalClient.class, parameters, clientExecutor);
		try {
			ServerCallable callable = new ServerCallable(ss);
			Future<String> future = serverExecutor.submit(callable);
			StartLaunchRS rs = rpClient.startLaunch(new StartLaunchRQ()).timeout(10, TimeUnit.SECONDS).blockingGet();
			String rq = future.get(10, TimeUnit.SECONDS);

			assertThat(rs, notNullValue());
			assertThat("First request should not contain cookie value", rq, not(containsString(COOKIE)));

			future = serverExecutor.submit(callable);
			rs = rpClient.startLaunch(new StartLaunchRQ()).timeout(10, TimeUnit.SECONDS).blockingGet();
			rq = future.get(10, TimeUnit.SECONDS);

			assertThat(rs, notNullValue());
			assertThat("Second request should contain cookie value", rq, containsString(COOKIE));
		} finally {
			rpClient.close();
			ss.close();
			shutdownExecutorService(serverExecutor);
			shutdownExecutorService(clientExecutor);
		}
	}
}
