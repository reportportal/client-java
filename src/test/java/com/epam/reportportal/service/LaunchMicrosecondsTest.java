/*
 * Copyright 2025 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
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
import com.epam.ta.reportportal.ws.model.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import io.reactivex.Maybe;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.epam.reportportal.util.test.CommonUtils.shutdownExecutorService;
import static java.util.Optional.ofNullable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class LaunchMicrosecondsTest {
	public static final long START_TIME_SECONDS_BASE = 1_700_000_000L;
	public static final long START_TIME_NANO_ADJUSTMENT = 123_456_000L;
	public static final long START_TIME_MILLIS_ADJUSTMENT = START_TIME_NANO_ADJUSTMENT / 1_000_000L;
	public static final long START_TIME_MILLISECONDS_BASE = (START_TIME_SECONDS_BASE * 1000) + START_TIME_MILLIS_ADJUSTMENT;

	private ExecutorService clientExecutor;

	@BeforeEach
	public void setUp() {
		clientExecutor = Executors.newSingleThreadExecutor();
	}

	@AfterEach
	public void tearDown() {
		shutdownExecutorService(clientExecutor);
	}

	private static <T> Pair<List<String>, T> executeWithClosing(ExecutorService clientExecutor, ServerSocket ss,
			SocketUtils.ServerCallable serverCallable, Callable<T> clientCallable) throws Exception {
		Pair<List<String>, T> result = SocketUtils.executeServerCallable(serverCallable, clientCallable);
		ss.close();
		shutdownExecutorService(clientExecutor);
		return result;
	}

	// We avoid OkHttp logger dependency; we assert request bodies captured by SocketUtils instead.

	private static ListenerParameters baseParameters(String baseUrl) {
		ListenerParameters parameters = TestUtils.standardParameters();
		parameters.setBaseUrl(baseUrl);
		parameters.setHttpLogging(true);
		return parameters;
	}

	private static StartLaunchRQ buildStartLaunchRq(Comparable<? extends Comparable<?>> startTime) {
		StartLaunchRQ rq = new StartLaunchRQ();
		rq.setName("microseconds-test-launch");
		rq.setStartTime(startTime);
		return rq;
	}

	private static StartTestItemRQ buildStartItemRq(String type, Comparable<? extends Comparable<?>> startTime) {
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setName(type + "-item");
		rq.setType(type);
		rq.setStartTime(startTime);
		return rq;
	}

	private static FinishTestItemRQ buildFinishItemRq(Comparable<? extends Comparable<?>> endTime) {
		FinishTestItemRQ rq = new FinishTestItemRQ();
		rq.setStatus("PASSED");
		rq.setEndTime(endTime);
		return rq;
	}

	private static FinishExecutionRQ buildFinishLaunchRq(Comparable<? extends Comparable<?>> endTime) {
		FinishExecutionRQ rq = new FinishExecutionRQ();
		rq.setEndTime(endTime);
		return rq;
	}

	private static Comparable<? extends Comparable<?>> dateOrInstant(boolean instant) {
		Instant testInstant = Instant.ofEpochSecond(START_TIME_SECONDS_BASE, 123_456_000);
		if (!instant) {
			return Date.from(testInstant);
		}
		return testInstant;
	}

	private static boolean expectString(boolean useMicroseconds, boolean isInstant) {
		return useMicroseconds && isInstant;
	}

	private static String findLastRequestWithKey(List<String> messages, String key) {
		for (int i = messages.size() - 1; i >= 0; i--) {
			String m = messages.get(i);
			if (m != null && m.contains("\"" + key + "\"")) {
				return m;
			}
		}
		return null;
	}

	private static String findLastJsonWithKey(Pair<List<String>, ?> result, String key) {
		return ofNullable(findLastRequestWithKey(result.getKey(), key)).map(r -> r.split("\\n\\n")[1]).orElse(null);
	}

	private static void assertTimeNumeric(String json, String key) {
		assertThat("Request body should be present", json, notNullValue());
		String expectedField = "\"" + key + "\":" + START_TIME_MILLISECONDS_BASE;
		assertThat("Expected numeric " + key + " in JSON: " + json, json, containsString(expectedField));
	}

	private static void assertTimeIsoMicro(String json, String key) {
		assertThat("Request body should be present", json, notNullValue());
		Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{6,9}Z\"");
		assertThat("Expected ISO instant with microseconds for " + key + " in JSON: " + json, p.matcher(json).find(), is(true));
	}

	private static SocketUtils.ServerCallable buildServerCallableForStartLaunch(ServerSocket ss, boolean micro) {
		List<String> responses = new ArrayList<>();
		// 1) GET /api/info
		responses.add(micro ? "files/responses/info_response_microseconds.txt" : "files/responses/info_response_no_microseconds.txt");
		// 2) POST /launch
		responses.add("files/responses/start_launch_response.txt");
		return new SocketUtils.ServerCallable(ss, Collections.emptyMap(), responses);
	}

	private static SocketUtils.ServerCallable buildServerCallableForStartItem(ServerSocket ss, boolean micro) {
		List<String> responses = new ArrayList<>();
		responses.add(micro ? "files/responses/info_response_microseconds.txt" : "files/responses/info_response_no_microseconds.txt");
		responses.add("files/responses/start_launch_response.txt");
		responses.add("files/responses/start_item_response.txt");
		return new SocketUtils.ServerCallable(ss, Collections.emptyMap(), responses);
	}

	private static SocketUtils.ServerCallable buildServerCallableForFinishItem(ServerSocket ss, boolean micro) {
		List<String> responses = new ArrayList<>();
		responses.add(micro ? "files/responses/info_response_microseconds.txt" : "files/responses/info_response_no_microseconds.txt");
		responses.add("files/responses/start_launch_response.txt");
		responses.add("files/responses/simple_response.txt"); // finish item response
		return new SocketUtils.ServerCallable(ss, Collections.emptyMap(), responses);
	}

	private static SocketUtils.ServerCallable buildServerCallableForFinishLaunch(ServerSocket ss, boolean micro) {
		List<String> responses = new ArrayList<>();
		responses.add(micro ? "files/responses/info_response_microseconds.txt" : "files/responses/info_response_no_microseconds.txt");
		responses.add("files/responses/start_launch_response.txt");
		responses.add("files/responses/simple_response.txt"); // finish launch response
		responses.add("files/responses/simple_response.txt"); // last log batch
		return new SocketUtils.ServerCallable(ss, Collections.emptyMap(), responses);
	}

	/* ---------------------- Launch start ---------------------- */
	@Test
	public void start_launch_useMicroseconds_false_Date_sends_numeric_time() throws Exception {
		startLaunchTimeCase(false, false);
	}

	@Test
	public void start_launch_useMicroseconds_false_Instant_sends_numeric_time() throws Exception {
		startLaunchTimeCase(false, true);
	}

	@Test
	public void start_launch_useMicroseconds_true_Instant_sends_iso_micro_time() throws Exception {
		startLaunchTimeCase(true, true);
	}

	@Test
	public void start_launch_useMicroseconds_true_Date_sends_numeric_time() throws Exception {
		startLaunchTimeCase(true, false);
	}

	private void startLaunchTimeCase(boolean micro, boolean instant) throws Exception {
		ServerSocket ss = SocketUtils.getServerSocketOnFreePort();
		String baseUrl = "http://localhost:" + ss.getLocalPort();
		ListenerParameters parameters = baseParameters(baseUrl);

		ReportPortalClient rpClient = ReportPortal.builder().buildClient(ReportPortalClient.class, parameters, clientExecutor);
		StartLaunchRQ rq = buildStartLaunchRq(dateOrInstant(instant));
		ReportPortal rp = ReportPortal.create(rpClient, parameters, clientExecutor);
		Launch launch = rp.newLaunch(rq);

		SocketUtils.ServerCallable serverCallable = buildServerCallableForStartLaunch(ss, micro);

		Pair<List<String>, String> result = executeWithClosing(
				clientExecutor,
				ss,
				serverCallable,
				() -> launch.start().timeout(10, TimeUnit.SECONDS).blockingGet()
		);

		String json = findLastJsonWithKey(result, "startTime");
		if (expectString(micro, instant)) {
			assertTimeIsoMicro(json, "startTime");
		} else {
			assertTimeNumeric(json, "startTime");
		}
	}

	/* ---------------------- startTestItem root ---------------------- */
	@Test
	public void start_item_root_useMicroseconds_false_Date_sends_numeric_time() throws Exception {
		startItemRootTimeCase(false, false);
	}

	@Test
	public void start_item_root_useMicroseconds_false_Instant_sends_numeric_time() throws Exception {
		startItemRootTimeCase(false, true);
	}

	@Test
	public void start_item_root_useMicroseconds_true_Instant_sends_iso_micro_time() throws Exception {
		startItemRootTimeCase(true, true);
	}

	@Test
	public void start_item_root_useMicroseconds_true_Date_sends_numeric_time() throws Exception {
		startItemRootTimeCase(true, false);
	}

	private void startItemRootTimeCase(boolean micro, boolean instant) throws Exception {
		ServerSocket ss = SocketUtils.getServerSocketOnFreePort();
		String baseUrl = "http://localhost:" + ss.getLocalPort();
		ListenerParameters parameters = baseParameters(baseUrl);

		ReportPortalClient rpClient = ReportPortal.builder().buildClient(ReportPortalClient.class, parameters, clientExecutor);
		StartLaunchRQ launchRq = buildStartLaunchRq(new Date());
		ReportPortal rp = ReportPortal.create(rpClient, parameters, clientExecutor);
		Launch launch = rp.newLaunch(launchRq);
		StartTestItemRQ rq = buildStartItemRq("SUITE", dateOrInstant(instant));

		SocketUtils.ServerCallable serverCallable = buildServerCallableForStartItem(ss, micro);

		Pair<List<String>, String> result = executeWithClosing(
				clientExecutor,
				ss,
				serverCallable,
				() -> launch.startTestItem(rq).timeout(10, TimeUnit.SECONDS).blockingGet()
		);

		String json = findLastJsonWithKey(result, "startTime");
		if (expectString(micro, instant)) {
			assertTimeIsoMicro(json, "startTime");
		} else {
			assertTimeNumeric(json, "startTime");
		}
	}

	/* ---------------------- startTestItem child ---------------------- */
	@Test
	public void start_item_child_useMicroseconds_false_Date_sends_numeric_time() throws Exception {
		startItemChildTimeCase(false, false);
	}

	@Test
	public void start_item_child_useMicroseconds_false_Instant_sends_numeric_time() throws Exception {
		startItemChildTimeCase(false, true);
	}

	@Test
	public void start_item_child_useMicroseconds_true_Instant_sends_iso_micro_time() throws Exception {
		startItemChildTimeCase(true, true);
	}

	@Test
	public void start_item_child_useMicroseconds_true_Date_sends_numeric_time() throws Exception {
		startItemChildTimeCase(true, false);
	}

	private void startItemChildTimeCase(boolean micro, boolean instant) throws Exception {
		ServerSocket ss = SocketUtils.getServerSocketOnFreePort();
		String baseUrl = "http://localhost:" + ss.getLocalPort();
		ListenerParameters parameters = baseParameters(baseUrl);

		ReportPortalClient rpClient = ReportPortal.builder().buildClient(ReportPortalClient.class, parameters, clientExecutor);
		StartLaunchRQ launchRq = buildStartLaunchRq(new Date());
		ReportPortal rp = ReportPortal.create(rpClient, parameters, clientExecutor);
		Launch launch = rp.newLaunch(launchRq);
		StartTestItemRQ rq = buildStartItemRq("TEST", dateOrInstant(instant));

		Maybe<String> parentId = Maybe.just("parent-item-id");

		SocketUtils.ServerCallable serverCallable = buildServerCallableForStartItem(ss, micro);

		Pair<List<String>, String> result = executeWithClosing(
				clientExecutor,
				ss,
				serverCallable,
				() -> launch.startTestItem(parentId, rq).timeout(10, TimeUnit.SECONDS).blockingGet()
		);

		String json = findLastJsonWithKey(result, "startTime");
		if (expectString(micro, instant)) {
			assertTimeIsoMicro(json, "startTime");
		} else {
			assertTimeNumeric(json, "startTime");
		}
	}

	/* ---------------------- finishTestItem ---------------------- */
	@Test
	public void finish_item_useMicroseconds_false_Date_sends_numeric_time() throws Exception {
		finishItemTimeCase(false, false);
	}

	@Test
	public void finish_item_useMicroseconds_false_Instant_sends_numeric_time() throws Exception {
		finishItemTimeCase(false, true);
	}

	@Test
	public void finish_item_useMicroseconds_true_Instant_sends_iso_micro_time() throws Exception {
		finishItemTimeCase(true, true);
	}

	@Test
	public void finish_item_useMicroseconds_true_Date_sends_numeric_time() throws Exception {
		finishItemTimeCase(true, false);
	}

	private void finishItemTimeCase(boolean micro, boolean instant) throws Exception {
		ServerSocket ss = SocketUtils.getServerSocketOnFreePort();
		String baseUrl = "http://localhost:" + ss.getLocalPort();
		ListenerParameters parameters = baseParameters(baseUrl);

		ReportPortalClient rpClient = ReportPortal.builder().buildClient(ReportPortalClient.class, parameters, clientExecutor);
		StartLaunchRQ launchRq = buildStartLaunchRq(new Date());
		ReportPortal rp = ReportPortal.create(rpClient, parameters, clientExecutor);
		Launch launch = rp.newLaunch(launchRq);
		FinishTestItemRQ rq = buildFinishItemRq(dateOrInstant(instant));

		Maybe<String> itemId = Maybe.just("some-item-id");

		SocketUtils.ServerCallable serverCallable = buildServerCallableForFinishItem(ss, micro);

		Pair<List<String>, ?> result = executeWithClosing(
				clientExecutor,
				ss,
				serverCallable,
				() -> launch.finishTestItem(itemId, rq).timeout(10, TimeUnit.SECONDS).blockingGet()
		);

		String json = findLastJsonWithKey(result, "endTime");
		if (expectString(micro, instant)) {
			assertTimeIsoMicro(json, "endTime");
		} else {
			assertTimeNumeric(json, "endTime");
		}
	}

	/* ---------------------- finish Launch ---------------------- */
	@Test
	public void finish_launch_useMicroseconds_false_Date_sends_numeric_time() throws Exception {
		finishLaunchTimeCase(false, false);
	}

	@Test
	public void finish_launch_useMicroseconds_false_Instant_sends_numeric_time() throws Exception {
		finishLaunchTimeCase(false, true);
	}

	@Test
	public void finish_launch_useMicroseconds_true_Instant_sends_iso_micro_time() throws Exception {
		finishLaunchTimeCase(true, true);
	}

	@Test
	public void finish_launch_useMicroseconds_true_Date_sends_numeric_time() throws Exception {
		finishLaunchTimeCase(true, false);
	}

	private void finishLaunchTimeCase(boolean micro, boolean instant) throws Exception {
		ServerSocket ss = SocketUtils.getServerSocketOnFreePort();
		String baseUrl = "http://localhost:" + ss.getLocalPort();
		ListenerParameters parameters = baseParameters(baseUrl);

		ReportPortalClient rpClient = ReportPortal.builder().buildClient(ReportPortalClient.class, parameters, clientExecutor);
		StartLaunchRQ launchRq = buildStartLaunchRq(new Date());
		ReportPortal rp = ReportPortal.create(rpClient, parameters, clientExecutor);
		Launch launch = rp.newLaunch(launchRq);
		FinishExecutionRQ rq = buildFinishLaunchRq(dateOrInstant(instant));

		SocketUtils.ServerCallable serverCallable = buildServerCallableForFinishLaunch(ss, micro);

		Pair<List<String>, ?> result = executeWithClosing(
				clientExecutor, ss, serverCallable, () -> {
					launch.finish(rq);
					return Boolean.TRUE;
				}
		);

		String json = findLastJsonWithKey(result, "endTime");
		if (expectString(micro, instant)) {
			assertTimeIsoMicro(json, "endTime");
		} else {
			assertTimeNumeric(json, "endTime");
		}
	}
}
