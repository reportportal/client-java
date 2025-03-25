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
import com.epam.reportportal.listeners.LogLevel;
import com.epam.reportportal.test.TestUtils;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.BatchSaveOperatingRS;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.FlowableSubscriber;
import okhttp3.MultipartBody;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static com.epam.reportportal.utils.http.HttpRequestUtils.TYPICAL_FILE_PART_HEADER;
import static com.epam.reportportal.utils.http.HttpRequestUtils.TYPICAL_MULTIPART_FOOTER_LENGTH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class LaunchLoggingTest {

	private ExecutorService executor;
	private ListenerParameters parameters;

	@BeforeEach
	public void setUp() {
		parameters = TestUtils.standardParameters();
		parameters.setBatchLogsSize(2);
		executor = Executors.newSingleThreadExecutor();
	}

	@AfterEach
	public void tearDown() {
		CommonUtils.shutdownExecutorService(executor);
	}

	private static void emitLogs(Launch launch, int timesToSend) {
		Date logDate = Calendar.getInstance().getTime();

		IntStream.range(0, timesToSend).forEach(i -> launch.log(launchUuid -> {
			SaveLogRQ result = new SaveLogRQ();
			result.setLaunchUuid(launchUuid);
			result.setItemUuid("itemUuid");
			result.setLevel(LogLevel.INFO.name());
			result.setLogTime(logDate);
			result.setMessage("Log message number: " + i);
			return result;
		}));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void test_log_batch_send_by_length() {
		ReportPortalClient client = mock(ReportPortalClient.class);
		TestUtils.mockStartLaunch(client, "launchUuid");
		TestUtils.mockBatchLogging(client);
		ListenerParameters myParameters = new ListenerParameters();
		Launch launch = new LaunchImpl(client, myParameters, TestUtils.standardLaunchRequest(myParameters), executor);

		emitLogs(launch, ListenerParameters.DEFAULT_LOG_BATCH_SIZE);

		verify(client, timeout(10000)).log(any(List.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void test_log_batch_not_send_by_length() {
		ReportPortalClient client = mock(ReportPortalClient.class);
		TestUtils.mockStartLaunch(client, "launchUuid");
		ListenerParameters myParameters = new ListenerParameters();
		Launch launch = new LaunchImpl(client, myParameters, TestUtils.standardLaunchRequest(myParameters), executor);

		emitLogs(launch, ListenerParameters.DEFAULT_LOG_BATCH_SIZE - 1);

		verify(client, timeout(100).times(0)).log(any(List.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void test_log_batch_send_by_stop() {
		ReportPortalClient client = mock(ReportPortalClient.class);
		TestUtils.mockLaunch(client, "launchUuid");
		TestUtils.mockBatchLogging(client);
		ListenerParameters myParameters = new ListenerParameters();
		Launch launch = new LaunchImpl(client, myParameters, TestUtils.standardLaunchRequest(myParameters), executor);

		emitLogs(launch, ListenerParameters.DEFAULT_LOG_BATCH_SIZE - 1);
		launch.finish(TestUtils.positiveFinishRequest());

		verify(client, timeout(10000)).log(any(List.class));
	}

	private static final String TEST_ATTACHMENT_NAME = "test_file.bin";
	private static final String TEST_ATTACHMENT_TYPE = "application/zip";

	@Test
	@SuppressWarnings("unchecked")
	public void test_log_batch_not_send_by_size() {
		ReportPortalClient client = mock(ReportPortalClient.class);
		TestUtils.mockStartLaunch(client, "launchUuid");
		ListenerParameters myParameters = new ListenerParameters();
		Launch launch = new LaunchImpl(client, myParameters, TestUtils.standardLaunchRequest(myParameters), executor);

		int headersSize =
				TYPICAL_MULTIPART_FOOTER_LENGTH - String.format(TYPICAL_FILE_PART_HEADER, TEST_ATTACHMENT_NAME, TEST_ATTACHMENT_TYPE)
						.length();
		int attachmentSize = (int) ListenerParameters.DEFAULT_BATCH_PAYLOAD_LIMIT - headersSize - 1024;
		byte[] randomByteArray = new byte[attachmentSize];
		ThreadLocalRandom.current().nextBytes(randomByteArray);

		SaveLogRQ request = new SaveLogRQ();
		SaveLogRQ.File file = new SaveLogRQ.File();
		request.setFile(file);
		file.setContent(randomByteArray);
		file.setName(TEST_ATTACHMENT_NAME);
		file.setContentType(TEST_ATTACHMENT_TYPE);

		Date logDate = Calendar.getInstance().getTime();
		launch.log(launchUuid -> {
			request.setLaunchUuid(launchUuid);
			request.setItemUuid("item_id");
			request.setLevel(LogLevel.INFO.name());
			request.setLogTime(logDate);
			request.setMessage("Log message");
			return request;
		});
		emitLogs(launch, 1);

		verify(client, timeout(100).times(0)).log(any(List.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void test_log_batch_send_by_size() throws IOException {
		ReportPortalClient client = mock(ReportPortalClient.class);
		TestUtils.mockStartLaunch(client, "launchUuid");
		TestUtils.mockBatchLogging(client);
		Launch launch = new LaunchImpl(client, parameters, TestUtils.standardLaunchRequest(parameters), executor);

		byte[] randomByteArray = new byte[(int) ListenerParameters.DEFAULT_BATCH_PAYLOAD_LIMIT];
		ThreadLocalRandom.current().nextBytes(randomByteArray);

		SaveLogRQ request = new SaveLogRQ();
		SaveLogRQ.File file = new SaveLogRQ.File();
		request.setFile(file);
		file.setContent(randomByteArray);
		file.setName(TEST_ATTACHMENT_NAME);
		file.setContentType(TEST_ATTACHMENT_TYPE);

		Date logDate = Calendar.getInstance().getTime();
		launch.log(launchUuid -> {
			request.setLaunchUuid(launchUuid);
			request.setItemUuid("item_id");
			request.setLevel(LogLevel.INFO.name());
			request.setLogTime(logDate);
			request.setMessage("Log message");
			return request;
		});
		emitLogs(launch, 1);

		ArgumentCaptor<List<MultipartBody.Part>> captor = ArgumentCaptor.forClass(List.class);
		verify(client, timeout(10000)).log(captor.capture());

		assertThat(captor.getValue(), hasSize(2));
		assertThat(captor.getValue().get(1).body().contentLength(), equalTo(ListenerParameters.DEFAULT_BATCH_PAYLOAD_LIMIT));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void test_log_batch_triggers_previous_request_to_send() {
		ReportPortalClient client = mock(ReportPortalClient.class);
		TestUtils.mockStartLaunch(client, "launchUuid");
		TestUtils.mockBatchLogging(client);
		Launch launch = new LaunchImpl(client, parameters, TestUtils.standardLaunchRequest(parameters), executor);

		emitLogs(launch, 1);
		verify(client, timeout(100).times(0)).log(any(List.class));

		byte[] randomByteArray = new byte[(int) ListenerParameters.DEFAULT_BATCH_PAYLOAD_LIMIT];
		ThreadLocalRandom.current().nextBytes(randomByteArray);

		SaveLogRQ request = new SaveLogRQ();
		SaveLogRQ.File file = new SaveLogRQ.File();
		request.setFile(file);
		file.setContent(randomByteArray);
		file.setName(TEST_ATTACHMENT_NAME);
		file.setContentType(TEST_ATTACHMENT_TYPE);

		Date logDate = Calendar.getInstance().getTime();
		launch.log(launchUuid -> {
			request.setLaunchUuid(launchUuid);
			request.setItemUuid("item_id");
			request.setLevel(LogLevel.INFO.name());
			request.setLogTime(logDate);
			request.setMessage("Log message");
			return request;
		});

		ArgumentCaptor<List<MultipartBody.Part>> captor = ArgumentCaptor.forClass(List.class);
		verify(client, timeout(10000)).log(captor.capture());

		assertThat(captor.getValue(), hasSize(1));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void test_log_batch_failure_logging() {
		ReportPortalClient client = mock(ReportPortalClient.class);
		TestUtils.mockStartLaunch(client, "launchUuid");
		RuntimeException exc = new IllegalStateException("test");
		when(client.log(any(List.class))).thenThrow(exc);
		FlowableSubscriber<BatchSaveOperatingRS> subscriber = mock(FlowableSubscriber.class);
		ListenerParameters myParameters = TestUtils.standardParameters();
		Launch launch = new LaunchImpl(client, myParameters, TestUtils.standardLaunchRequest(myParameters), executor, subscriber);

		emitLogs(launch, 10);
		verify(client, timeout(10000)).log(any(List.class));

		ArgumentCaptor<RuntimeException> exceptionCaptor = ArgumentCaptor.forClass(RuntimeException.class);
		verify(subscriber, timeout(1000)).onError(exceptionCaptor.capture());

		assertThat(exceptionCaptor.getValue(), sameInstance(exc));
	}
}
