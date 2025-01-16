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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.epam.reportportal.exception.InternalReportPortalClientException;
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
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.net.ssl.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.epam.reportportal.test.TestUtils.*;
import static com.epam.reportportal.util.test.CommonUtils.shutdownExecutorService;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

public class ReportPortalTest {
	private static final String TRUSTSTORE_PATH = "files/certificates/truststore.jks";
	private static final String TRUSTSTORE_PASSWORD = "changeit";

	static {
//		System.setProperty("jsse.enableSNIExtension", "false");
//		System.setProperty("javax.net.ssl.trustStore", TRUSTSTORE_PATH);
//		System.setProperty("javax.net.ssl.trustStorePassword", TRUSTSTORE_PASSWORD);
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
	}

	private static final String COOKIE = "AWSALB=P7cqG8g/K70xHAKOUPrWrG0XgmhG8GJNinj8lDnKVyITyubAen2lBr+fSa/e2JAoGksQphtImp49rZxc41qdqUGvAc67SdZHY1BMFIHKzc8kyWc1oQjq6oI+s39U";
	private static final String OVERRIDING_COOKIE = "AWSALB=4CKP4Er0pg17+DfkR2ItBJbL8bdo2pBpP1SvLyiPwllSai6sawCn2blRepEX9nwdc8Aj4itewNUZdSONcTEgXfJFN2NAWuSl5CMQ3+vKuCO0cZrCxw02hXSXPir5";

	private static final String KEYSTORE_PATH = "files/certificates/keystore.jks";
	private static final String KEYSTORE_PASSWORD = "keystorePassword";

	private static final String INVALID_KEYSTORE_PATH = "invalid/path/to/keystore.jks";
	private static final String INVALID_KEYSTORE_PASSWORD = "invalidPassword";

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
			SocketUtils.ServerCallable serverCallable = new SocketUtils.ServerCallable(
					server,
					Collections.emptyMap(),
					"files/simple_response.txt"
			);
			Pair<List<String>, Response> result = SocketUtils.executeServerCallable(
					serverCallable,
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
			SocketUtils.ServerCallable serverCallable = new SocketUtils.ServerCallable(
					server,
					Collections.emptyMap(),
					Arrays.asList("files/proxy_auth_response.txt", "files/simple_response.txt")
			);
			Pair<List<String>, Response> proxyAuth = SocketUtils.executeServerCallable(
					serverCallable,
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

	private static Map<String, Object> createCookieModel() {
		Map<String, Object> model = new HashMap<>();
		model.put("cookie", COOKIE);
		SimpleDateFormat sdf = new SimpleDateFormat(SocketUtils.WEB_DATE_FORMAT, Locale.US);
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		Calendar cal = Calendar.getInstance();
		model.put("date", sdf.format(cal.getTime()));
		cal.add(Calendar.MINUTE, 2);
		model.put("expire", sdf.format(cal.getTime()));
		return model;
	}

	private static <T> Pair<List<String>, T> executeWithClosingOnException(ExecutorService clientExecutor, ServerSocket ss,
			Callable<Pair<List<String>, T>> actions) throws Exception {
		Exception error;
		try {
			return actions.call();
		} catch (Exception e) {
			error = e;
		}
		ss.close();
		shutdownExecutorService(clientExecutor);
		throw error;
	}

	private static <T> Pair<List<String>, T> executeWithClosingOnException(ExecutorService clientExecutor, ServerSocket ss,
			SocketUtils.ServerCallable serverCallable, Callable<T> clientCallable) throws Exception {
		return executeWithClosingOnException(clientExecutor, ss, () -> SocketUtils.executeServerCallable(serverCallable, clientCallable));
	}

	private static Pair<List<String>, StartLaunchRS> executeWithClosing(ExecutorService clientExecutor, ServerSocket ss,
			SocketUtils.ServerCallable serverCallable, Callable<StartLaunchRS> clientCallable) throws Exception {
		Pair<List<String>, StartLaunchRS> result = executeWithClosingOnException(
				clientExecutor,
				ss,
				() -> SocketUtils.executeServerCallable(serverCallable, clientCallable)
		);
		ss.close();
		shutdownExecutorService(clientExecutor);
		return result;
	}

	private SSLServerSocket createSSLServerSocket(int port) throws Exception {
		System.setProperty("javax.net.debug", "ssl,handshake");

		KeyStore keyStore = KeyStore.getInstance("JKS");
		keyStore.load(getClass().getResourceAsStream(KEYSTORE_PATH), KEYSTORE_PASSWORD.toCharArray());

		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
				.getAlgorithm());
		kmf.init(keyStore, KEYSTORE_PASSWORD.toCharArray());

		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(kmf.getKeyManagers(), null, null);

		SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();
		String[] enabledCipherSuites = sslServerSocketFactory.getSupportedCipherSuites();
		SSLServerSocket sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(
				port,
				0,
				InetAddress.getByName("localhost")
		);
		sslServerSocket.setEnabledProtocols(new String[] { "TLSv1.2" });
		sslServerSocket.setEnabledCipherSuites(enabledCipherSuites);
		return sslServerSocket;
	}

	@Test
	public void verify_rp_client_saves_and_bypasses_cookies() throws Exception {
		ServerSocket ss = SocketUtils.getServerSocketOnFreePort();
		ListenerParameters parameters = standardParameters();
		parameters.setBaseUrl("http://localhost:" + ss.getLocalPort());
		ExecutorService clientExecutor = Executors.newSingleThreadExecutor();
		ReportPortalClient rpClient = ReportPortal.builder().buildClient(ReportPortalClient.class, parameters, clientExecutor);
		SocketUtils.ServerCallable serverCallable = new SocketUtils.ServerCallable(ss, createCookieModel(), "files/socket_response.txt");
		Callable<StartLaunchRS> clientCallable = () -> rpClient.startLaunch(new StartLaunchRQ()).timeout(5, TimeUnit.SECONDS).blockingGet();
		Pair<List<String>, StartLaunchRS> result = executeWithClosingOnException(clientExecutor, ss, serverCallable, clientCallable);

		assertThat(result.getValue(), notNullValue());
		assertThat("First request should not contain cookie value", result.getKey().get(0), not(containsString(COOKIE)));

		result = executeWithClosing(clientExecutor, ss, serverCallable, clientCallable);

		assertThat(result.getValue(), notNullValue());
		assertThat("Second request should contain cookie value", result.getKey().get(0), containsString(COOKIE));
	}

	@Test
	public void verify_rp_client_does_not_duplicate_cookies() throws Exception {
		ServerSocket ss = SocketUtils.getServerSocketOnFreePort();
		ListenerParameters parameters = standardParameters();
		parameters.setBaseUrl("http://localhost:" + ss.getLocalPort());
		ExecutorService clientExecutor = Executors.newSingleThreadExecutor();
		ReportPortalClient rpClient = ReportPortal.builder().buildClient(ReportPortalClient.class, parameters, clientExecutor);
		Map<String, Object> model = createCookieModel();
		SocketUtils.ServerCallable serverCallable = new SocketUtils.ServerCallable(ss, model, "files/socket_response.txt");
		Callable<StartLaunchRS> clientCallable = () -> rpClient.startLaunch(new StartLaunchRQ()).timeout(5, TimeUnit.SECONDS).blockingGet();
		executeWithClosingOnException(clientExecutor, ss, serverCallable, clientCallable);
		model.put("cookie", OVERRIDING_COOKIE);
		executeWithClosingOnException(clientExecutor, ss, serverCallable, clientCallable);
		Pair<List<String>, StartLaunchRS> result = executeWithClosing(clientExecutor, ss, serverCallable, clientCallable);

		assertThat(result.getValue(), notNullValue());
		assertThat("Second request should contain new cookie value", result.getKey().get(0), containsString(OVERRIDING_COOKIE));
		assertThat("Second request should not contain old cookie value", result.getKey().get(0), not(containsString(COOKIE)));
	}

	@Test
	public void verify_rp_client_http_logging() throws Exception {
		ServerSocket ss = SocketUtils.getServerSocketOnFreePort();
		ListenerParameters parameters = standardParameters();
		parameters.setHttpLogging(true);
		String host = "localhost:" + ss.getLocalPort();
		@SuppressWarnings("HttpUrlsUsage")
		String baseUrl = "http://" + host;
		parameters.setBaseUrl(baseUrl);
		ExecutorService clientExecutor = Executors.newSingleThreadExecutor();
		ReportPortalClient rpClient = ReportPortal.builder().buildClient(ReportPortalClient.class, parameters, clientExecutor);

		SocketUtils.ServerCallable serverCallable = new SocketUtils.ServerCallable(
				ss,
				Collections.emptyMap(),
				Collections.singletonList("files/simple_response.txt")
		);
		// trigger http logging to init loggers
		executeWithClosingOnException(clientExecutor, ss, serverCallable, () -> rpClient.getProjectSettings().blockingGet());

		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		List<Logger> loggerList = loggerContext.getLoggerList()
				.stream()
				.filter(l -> l.getName() != null && l.getName().endsWith("OkHttpClient"))
				.collect(Collectors.toList());
		assertThat(loggerList, hasSize(1));

		ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
		listAppender.setContext(loggerContext);
		listAppender.start();
		loggerList.get(0).addAppender(listAppender);

		serverCallable = new SocketUtils.ServerCallable(ss, createCookieModel(), Collections.singletonList("files/socket_response.txt"));
		Callable<StartLaunchRS> clientCallable = () -> rpClient.startLaunch(new StartLaunchRQ()).timeout(5, TimeUnit.SECONDS).blockingGet();
		executeWithClosingOnException(clientExecutor, ss, serverCallable, clientCallable);

		assertThat(listAppender.list, hasSize(greaterThan(10)));
		String requestTarget = "--> POST " + baseUrl + "/api/v1/unit-test/launch http/1.1";
		assertThat(listAppender.list.get(0).getMessage(), equalTo(requestTarget));
		List<String> messages = listAppender.list.stream().map(ILoggingEvent::getMessage).collect(Collectors.toList());
		assertThat(messages, hasItem("Host: " + host));
		listAppender.list.clear();

		executeWithClosing(clientExecutor, ss, serverCallable, clientCallable);

		assertThat(listAppender.list, hasSize(greaterThan(10)));
		assertThat(listAppender.list.get(0).getMessage(), equalTo(requestTarget));
		messages = listAppender.list.stream().map(ILoggingEvent::getMessage).collect(Collectors.toList());
		assertThat(messages, hasItem("Cookie: " + COOKIE));
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

			//noinspection ResultOfMethodCallIgnored
			launch.startTestItem(TestUtils.standardStartSuiteRequest()).blockingGet();

			verify(rpClient, after(1000).times(0)).startLaunch(any(StartLaunchRQ.class));

			ArgumentCaptor<StartTestItemRQ> startCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
			verify(rpClient).startTestItem(startCaptor.capture());

			StartTestItemRQ startItem = startCaptor.getValue();
			assertThat(startItem.getLaunchUuid(), equalTo(launchUuid));

			launch.finish(TestUtils.standardLaunchFinishRequest());

			verify(rpClient, after(1000).times(0)).finishLaunch(anyString(), any(FinishExecutionRQ.class));
		} catch (RuntimeException e) {
			error = e;
		}
		CommonUtils.shutdownExecutorService(executor);
		if (error != null) {
			throw error;
		}
	}

	@Test
	public void verify_launch_uuid_creation_skip_parameter_handling() {
		simulateStartLaunchResponse(rpClient);
		simulateStartTestItemResponse(rpClient);

		String launchUuid = "test-launch-uuid";
		ListenerParameters listenerParameters = TestUtils.standardParameters();
		listenerParameters.setLaunchUuid(launchUuid);
		listenerParameters.setLaunchUuidCreationSkip(false);

		ExecutorService executor = Executors.newSingleThreadExecutor();
		RuntimeException error = null;

		try {
			ReportPortal rp = ReportPortal.create(rpClient, listenerParameters, executor);

			Launch launch = rp.newLaunch(standardLaunchRequest(listenerParameters));
			Maybe<String> launchMaybe = launch.start();
			assertThat(launchMaybe.blockingGet(), equalTo(launchUuid));

			//noinspection ResultOfMethodCallIgnored
			launch.startTestItem(TestUtils.standardStartSuiteRequest()).blockingGet();

			verify(rpClient, timeout(1000).times(1)).startLaunch(any(StartLaunchRQ.class));

			ArgumentCaptor<StartTestItemRQ> startCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
			verify(rpClient).startTestItem(startCaptor.capture());

			StartTestItemRQ startItem = startCaptor.getValue();
			assertThat(startItem.getLaunchUuid(), equalTo(launchUuid));

			launch.finish(TestUtils.standardLaunchFinishRequest());

			verify(rpClient, timeout(1000).times(1)).finishLaunch(eq(launchUuid), any(FinishExecutionRQ.class));
		} catch (RuntimeException e) {
			error = e;
		}
		CommonUtils.shutdownExecutorService(executor);
		if (error != null) {
			throw error;
		}
	}

	@Test
	public void verify_https_parameters_work_with_self_signed_certificate() throws Exception {
		ServerSocket ss = SocketUtils.getServerSocketOnFreePort();
		int port = ss.getLocalPort();
		ss.close();
		String baseUrl = "https://localhost:" + port;

		SSLServerSocket sslServerSocket = createSSLServerSocket(port);

		SocketUtils.ServerCallable serverCallable = new SocketUtils.ServerCallable(
				sslServerSocket,
				Collections.emptyMap(),
				"files/socket_response.txt"
		);

		ListenerParameters parameters = TestUtils.standardParameters();
		parameters.setBaseUrl(baseUrl);
		parameters.setKeystore(KEYSTORE_PATH);
		parameters.setKeystorePassword(KEYSTORE_PASSWORD);

		ExecutorService clientExecutor = Executors.newSingleThreadExecutor();
		ReportPortalClient rpClient = ReportPortal.builder().buildClient(ReportPortalClient.class, parameters, clientExecutor);

		Callable<StartLaunchRS> clientCallable = () -> rpClient.startLaunch(new StartLaunchRQ())
				.timeout(20, TimeUnit.SECONDS)
				.blockingGet();
		Pair<List<String>, StartLaunchRS> result = executeWithClosing(clientExecutor, sslServerSocket, serverCallable, clientCallable);
		assertThat(result.getValue(), notNullValue());
		StartLaunchRS startLaunchRS = result.getValue();
		assertThat(startLaunchRS.getId(), notNullValue());
	}

	@Test
	public void verify_invalid_keystore_path() {
		ListenerParameters parameters = TestUtils.standardParameters();
		parameters.setBaseUrl("https://localhost:8443");
		parameters.setKeystore(INVALID_KEYSTORE_PATH);
		parameters.setKeystorePassword(KEYSTORE_PASSWORD);

		ExecutorService clientExecutor = Executors.newSingleThreadExecutor();
		assertThrows(
				InternalReportPortalClientException.class,
				() -> ReportPortal.builder().buildClient(ReportPortalClient.class, parameters, clientExecutor)
		);
	}

	@Test
	public void verify_invalid_keystore_password() {
		ListenerParameters parameters = TestUtils.standardParameters();
		parameters.setBaseUrl("https://localhost:8443");
		parameters.setKeystore(KEYSTORE_PATH);
		parameters.setKeystorePassword(INVALID_KEYSTORE_PASSWORD);

		ExecutorService clientExecutor = Executors.newSingleThreadExecutor();
		assertThrows(
				InternalReportPortalClientException.class,
				() -> ReportPortal.builder().buildClient(ReportPortalClient.class, parameters, clientExecutor)
		);
	}
}
