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
import com.epam.reportportal.message.ReportPortalMessage;
import com.epam.reportportal.test.TestUtils;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Maybe;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MultipartBody;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.epam.reportportal.util.test.CommonUtils.shutdownExecutorService;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReportPortalLoggingTest {

	@Mock
	private ReportPortalClient rpClient;

	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	@AfterEach
	public void tearDown() {
		shutdownExecutorService(executor);
	}

	private static void verifySaveLogRQ(List<SaveLogRQ> logRequests, String logLevel, String launchUuid, String testUuid) {
		assertThat(logRequests, hasSize(1));
		SaveLogRQ log = logRequests.get(0);
		assertThat(log.getLevel(), equalTo(logLevel));
		assertThat(log.getLaunchUuid(), equalTo(launchUuid));
		assertThat(log.getItemUuid(), equalTo(testUuid));
	}

	private static void verifySaveLogRQ(List<SaveLogRQ> logRequests, String logLevel, String message, String launchUuid, String testUuid) {
		verifySaveLogRQ(logRequests, logLevel, launchUuid, testUuid);
		SaveLogRQ log = logRequests.get(0);
		assertThat(log.getMessage(), equalTo(message));
	}

	private static void verifySaveLogRQ(List<SaveLogRQ> logRequests, String logLevel, String message, Date logDate, String launchUuid,
			String testUuid) {
		verifySaveLogRQ(logRequests, logLevel, message, launchUuid, testUuid);
		SaveLogRQ log = logRequests.get(0);
		assertThat(log.getLogTime(), equalTo(logDate));
	}

	private static void verifySaveLogRQNullFile(List<SaveLogRQ> logRequests, String logLevel, String message, Date logDate,
			String launchUuid, String testUuid) {
		verifySaveLogRQ(logRequests, logLevel, message, logDate, launchUuid, testUuid);
		SaveLogRQ log = logRequests.get(0);
		assertThat(log.getFile(), nullValue());
	}

	@Test
	@Order(1)
	@SuppressWarnings("unchecked")
	public void verify_emitLog_no_context() {
		String logLevel = "INFO";
		String message = "message";

		assertThat("Log was logged", !ReportPortal.emitLog(message, logLevel, Calendar.getInstance().getTime()));

		ArgumentCaptor<List<MultipartBody.Part>> logCaptor = ArgumentCaptor.forClass(List.class);
		verify(rpClient, after(1000).times(0)).log(logCaptor.capture());
	}

	@Test
	@Order(1)
	@SuppressWarnings("unchecked")
	public void verify_emitLog_with_id_no_context() {
		String logLevel = "INFO";
		String message = "message";

		assertThat(
				"Log was logged", !ReportPortal.emitLog(
						Maybe.just("testUuid"),
						itemUuid -> ReportPortal.toSaveLogRQ(
								"launchUuid",
								itemUuid,
								logLevel,
								Calendar.getInstance().getTime(),
								new ReportPortalMessage(message)
						)
				)
		);

		ArgumentCaptor<List<MultipartBody.Part>> logCaptor = ArgumentCaptor.forClass(List.class);
		verify(rpClient, after(1000).times(0)).log(logCaptor.capture());
	}

	@Test
	@Order(1)
	@SuppressWarnings("unchecked")
	public void verify_emitLaunchLog_no_context() {
		String logLevel = "INFO";
		String message = "message";

		assertThat("Log was logged", !ReportPortal.emitLaunchLog(message, logLevel, Calendar.getInstance().getTime()));

		ArgumentCaptor<List<MultipartBody.Part>> logCaptor = ArgumentCaptor.forClass(List.class);
		verify(rpClient, after(1000).times(0)).log(logCaptor.capture());
	}

	@Test
	@Order(Integer.MAX_VALUE)
	@SuppressWarnings("unchecked")
	public void verify_emitLog_simple_message() {
		TestUtils.mockBatchLogging(rpClient);
		String launchUuid = "launchUuid";
		String testUuid = "testUuid";
		String logLevel = "INFO";
		String message = "message";
		LoggingContext context = LoggingContext.init(Maybe.just(launchUuid), Maybe.just(testUuid), rpClient, Schedulers.from(executor));
		Date logDate = Calendar.getInstance().getTime();

		assertThat("Log wasn't logged", ReportPortal.emitLog(message, logLevel, logDate));
		Throwable result = context.completed().blockingGet();
		assertThat(result, nullValue());

		ArgumentCaptor<List<MultipartBody.Part>> logCaptor = ArgumentCaptor.forClass(List.class);
		verify(rpClient, timeout(1000)).log(logCaptor.capture());

		// Verify basic fields
		List<SaveLogRQ> logRequests = logCaptor.getAllValues()
				.stream()
				.flatMap(rq -> TestUtils.extractJsonParts(rq).stream())
				.collect(Collectors.toList());
		verifySaveLogRQNullFile(logRequests, logLevel, message, logDate, launchUuid, testUuid);
	}

	@Test
	@Order(Order.DEFAULT)
	@SuppressWarnings("unchecked")
	public void verify_emitLog_message_with_file() throws IOException {
		TestUtils.mockBatchLogging(rpClient);
		String launchUuid = "launchUuid1";
		String testUuid = "testUuid1";
		String logLevel = "INFO";
		String message = "message";
		Date logDate = Calendar.getInstance().getTime();
		String filePath = "pug/lucky.jpg";
		File file = new File(filePath);

		LoggingContext context = LoggingContext.init(Maybe.just(launchUuid), Maybe.just(testUuid), rpClient, Schedulers.from(executor));
		assertThat("Log wasn't logged", ReportPortal.emitLog(message, logLevel, logDate, file));
		Throwable result = context.completed().blockingGet();
		assertThat(result, nullValue());

		ArgumentCaptor<List<MultipartBody.Part>> logCaptor = ArgumentCaptor.forClass(List.class);
		verify(rpClient, timeout(1000)).log(logCaptor.capture());

		// Verify basic fields
		List<SaveLogRQ> logRequests = logCaptor.getAllValues()
				.stream()
				.flatMap(rq -> TestUtils.extractJsonParts(rq).stream())
				.collect(Collectors.toList());
		verifySaveLogRQ(logRequests, logLevel, message, logDate, launchUuid, testUuid);

		// Verify file metadata
		SaveLogRQ.File loggedFile = logRequests.get(0).getFile();
		assertThat(loggedFile, notNullValue());
		assertThat(loggedFile.getName(), notNullValue());

		// Verify binary data
		List<Pair<String, byte[]>> binaries = logCaptor.getAllValues()
				.stream()
				.flatMap(rq -> TestUtils.extractBinaryParts(rq).stream())
				.collect(Collectors.toList());
		assertThat(binaries, hasSize(1));
		assertThat(binaries.get(0).getKey(), equalTo("image/jpeg"));
		assertThat(binaries.get(0).getValue(), equalTo(IOUtils.resourceToByteArray(filePath, this.getClass().getClassLoader())));
	}

	@Test
	@Order(Order.DEFAULT)
	@SuppressWarnings("unchecked")
	public void verify_emitLaunchLog_simple_message() {
		TestUtils.mockBatchLogging(rpClient);
		String launchUuid = "launchUuid2";
		String logLevel = "INFO";
		String message = "message";
		LaunchLoggingContext context = LaunchLoggingContext.init(Maybe.just(launchUuid), rpClient, Schedulers.from(executor));
		Date logDate = Calendar.getInstance().getTime();

		assertThat("Log wasn't logged", ReportPortal.emitLaunchLog(message, logLevel, logDate));
		Throwable result = context.completed().blockingGet();
		assertThat(result, nullValue());

		ArgumentCaptor<List<MultipartBody.Part>> logCaptor = ArgumentCaptor.forClass(List.class);
		verify(rpClient, timeout(1000)).log(logCaptor.capture());

		// Verify basic fields
		List<SaveLogRQ> logRequests = logCaptor.getAllValues()
				.stream()
				.flatMap(rq -> TestUtils.extractJsonParts(rq).stream())
				.collect(Collectors.toList());
		verifySaveLogRQNullFile(logRequests, logLevel, message, logDate, launchUuid, null);
	}

	@Test
	@Order(Order.DEFAULT)
	@SuppressWarnings("unchecked")
	public void verify_emitLaunchLog_message_with_file() throws IOException {
		TestUtils.mockBatchLogging(rpClient);
		String launchUuid = "launchUuid3";
		String logLevel = "INFO";
		String message = "message";
		Date logDate = Calendar.getInstance().getTime();
		String filePath = "pug/unlucky.jpg";
		File file = new File(filePath);

		LaunchLoggingContext context = LaunchLoggingContext.init(Maybe.just(launchUuid), rpClient, Schedulers.from(executor));
		assertThat("Log wasn't logged", ReportPortal.emitLaunchLog(message, logLevel, logDate, file));
		Throwable result = context.completed().blockingGet();
		assertThat(result, nullValue());

		ArgumentCaptor<List<MultipartBody.Part>> logCaptor = ArgumentCaptor.forClass(List.class);
		verify(rpClient, timeout(1000)).log(logCaptor.capture());

		// Verify basic fields
		List<SaveLogRQ> logRequests = logCaptor.getAllValues()
				.stream()
				.flatMap(rq -> TestUtils.extractJsonParts(rq).stream())
				.collect(Collectors.toList());
		verifySaveLogRQ(logRequests, logLevel, message, logDate, launchUuid, null);

		// Verify file metadata
		SaveLogRQ.File loggedFile = logRequests.get(0).getFile();
		assertThat(loggedFile, notNullValue());
		assertThat(loggedFile.getName(), notNullValue());

		// Verify binary data
		List<Pair<String, byte[]>> binaries = logCaptor.getAllValues()
				.stream()
				.flatMap(rq -> TestUtils.extractBinaryParts(rq).stream())
				.collect(Collectors.toList());
		assertThat(binaries, hasSize(1));
		assertThat(binaries.get(0).getKey(), equalTo("image/jpeg"));
		assertThat(binaries.get(0).getValue(), equalTo(IOUtils.resourceToByteArray(filePath, this.getClass().getClassLoader())));
	}

	@Test
	@Order(Order.DEFAULT)
	@SuppressWarnings("unchecked")
	public void verify_emitLog_method_with_ReportPortalMessage() throws IOException {
		TestUtils.mockBatchLogging(rpClient);
		String launchUuid = "launchUuid4";
		String testUuid = "testUuid4";
		String logLevel = "INFO";
		String message = "message";
		Date logDate = Calendar.getInstance().getTime();
		String filePath = "pug/unlucky.jpg";
		File file = new File(filePath);

		LoggingContext context = LoggingContext.init(Maybe.just(launchUuid), Maybe.just(testUuid), rpClient, Schedulers.from(executor));
		assertThat("Log wasn't logged", ReportPortal.emitLog(new ReportPortalMessage(file, message), logLevel, logDate));
		Throwable result = context.completed().blockingGet();
		assertThat(result, nullValue());

		ArgumentCaptor<List<MultipartBody.Part>> logCaptor = ArgumentCaptor.forClass(List.class);
		verify(rpClient, timeout(1000)).log(logCaptor.capture());

		// Verify basic fields
		List<SaveLogRQ> logRequests = logCaptor.getAllValues()
				.stream()
				.flatMap(rq -> TestUtils.extractJsonParts(rq).stream())
				.collect(Collectors.toList());
		verifySaveLogRQ(logRequests, logLevel, message, logDate, launchUuid, testUuid);

		// Verify file metadata
		SaveLogRQ.File loggedFile = logRequests.get(0).getFile();
		assertThat(loggedFile, notNullValue());
		assertThat(loggedFile.getName(), notNullValue());

		// Verify binary data
		List<Pair<String, byte[]>> binaries = logCaptor.getAllValues()
				.stream()
				.flatMap(rq -> TestUtils.extractBinaryParts(rq).stream())
				.collect(Collectors.toList());
		assertThat(binaries, hasSize(1));
		assertThat(binaries.get(0).getKey(), equalTo("image/jpeg"));
		assertThat(binaries.get(0).getValue(), equalTo(IOUtils.resourceToByteArray(filePath, this.getClass().getClassLoader())));
	}

	@Test
	@Order(Order.DEFAULT)
	@SuppressWarnings("unchecked")
	public void verify_emitLog_method_with_ReportPortalMessage_no_file() {
		TestUtils.mockBatchLogging(rpClient);
		String launchUuid = "launchUuid5";
		String testUuid = "testUuid5";
		String logLevel = "INFO";
		String message = "message";
		Date logDate = Calendar.getInstance().getTime();

		LoggingContext context = LoggingContext.init(Maybe.just(launchUuid), Maybe.just(testUuid), rpClient, Schedulers.from(executor));
		assertThat("Log wasn't logged", ReportPortal.emitLog(new ReportPortalMessage(message), logLevel, logDate));
		Throwable result = context.completed().blockingGet();
		assertThat(result, nullValue());

		ArgumentCaptor<List<MultipartBody.Part>> logCaptor = ArgumentCaptor.forClass(List.class);
		verify(rpClient, timeout(1000)).log(logCaptor.capture());

		// Verify basic fields
		List<SaveLogRQ> logRequests = logCaptor.getAllValues()
				.stream()
				.flatMap(rq -> TestUtils.extractJsonParts(rq).stream())
				.collect(Collectors.toList());
		verifySaveLogRQ(logRequests, logLevel, message, logDate, launchUuid, testUuid);

		// Verify file metadata
		SaveLogRQ.File loggedFile = logRequests.get(0).getFile();
		assertThat(loggedFile, nullValue());

		// Verify binary data
		List<Pair<String, byte[]>> binaries = logCaptor.getAllValues()
				.stream()
				.flatMap(rq -> TestUtils.extractBinaryParts(rq).stream())
				.collect(Collectors.toList());
		assertThat(binaries, hasSize(0));
	}

	@Test
	@Order(Order.DEFAULT)
	@SuppressWarnings("unchecked")
	public void verify_emitLaunchLog_method_with_ReportPortalMessage() throws IOException {
		TestUtils.mockBatchLogging(rpClient);
		String launchUuid = "launchUuid6";
		String logLevel = "INFO";
		String message = "message";
		Date logDate = Calendar.getInstance().getTime();
		String filePath = "pug/unlucky.jpg";
		File file = new File(filePath);

		LaunchLoggingContext context = LaunchLoggingContext.init(Maybe.just(launchUuid), rpClient, Schedulers.from(executor));
		assertThat("Log wasn't logged", ReportPortal.emitLaunchLog(new ReportPortalMessage(file, message), logLevel, logDate));
		Throwable result = context.completed().blockingGet();
		assertThat(result, nullValue());

		ArgumentCaptor<List<MultipartBody.Part>> logCaptor = ArgumentCaptor.forClass(List.class);
		verify(rpClient, timeout(1000)).log(logCaptor.capture());

		// Verify basic fields
		List<SaveLogRQ> logRequests = logCaptor.getAllValues()
				.stream()
				.flatMap(rq -> TestUtils.extractJsonParts(rq).stream())
				.collect(Collectors.toList());
		verifySaveLogRQ(logRequests, logLevel, message, logDate, launchUuid, null);

		// Verify file metadata
		SaveLogRQ.File loggedFile = logRequests.get(0).getFile();
		assertThat(loggedFile, notNullValue());
		assertThat(loggedFile.getName(), notNullValue());

		// Verify binary data
		List<Pair<String, byte[]>> binaries = logCaptor.getAllValues()
				.stream()
				.flatMap(rq -> TestUtils.extractBinaryParts(rq).stream())
				.collect(Collectors.toList());
		assertThat(binaries, hasSize(1));
		assertThat(binaries.get(0).getKey(), equalTo("image/jpeg"));
		assertThat(binaries.get(0).getValue(), equalTo(IOUtils.resourceToByteArray(filePath, this.getClass().getClassLoader())));
	}

	@Test
	@Order(Order.DEFAULT)
	@SuppressWarnings("unchecked")
	public void verify_emitLaunchLog_method_with_ReportPortalMessage_no_file() {
		TestUtils.mockBatchLogging(rpClient);
		String launchUuid = "launchUuid7";
		String logLevel = "INFO";
		String message = "message";
		Date logDate = Calendar.getInstance().getTime();

		LaunchLoggingContext context = LaunchLoggingContext.init(Maybe.just(launchUuid), rpClient, Schedulers.from(executor));
		assertThat("Log wasn't logged", ReportPortal.emitLaunchLog(new ReportPortalMessage(message), logLevel, logDate));
		Throwable result = context.completed().blockingGet();
		assertThat(result, nullValue());

		ArgumentCaptor<List<MultipartBody.Part>> logCaptor = ArgumentCaptor.forClass(List.class);
		verify(rpClient, timeout(1000)).log(logCaptor.capture());

		// Verify basic fields
		List<SaveLogRQ> logRequests = logCaptor.getAllValues()
				.stream()
				.flatMap(rq -> TestUtils.extractJsonParts(rq).stream())
				.collect(Collectors.toList());
		verifySaveLogRQ(logRequests, logLevel, message, logDate, launchUuid, null);

		// Verify file metadata
		SaveLogRQ.File loggedFile = logRequests.get(0).getFile();
		assertThat(loggedFile, nullValue());

		// Verify binary data
		List<Pair<String, byte[]>> binaries = logCaptor.getAllValues()
				.stream()
				.flatMap(rq -> TestUtils.extractBinaryParts(rq).stream())
				.collect(Collectors.toList());
		assertThat(binaries, hasSize(0));
	}

	@Test
	@Order(Order.DEFAULT)
	@SuppressWarnings("unchecked")
	public void verify_emitLog_method_with_specified_id() {
		TestUtils.mockBatchLogging(rpClient);
		String launchUuid = "launchUuid8";
		String testUuid = "testUuid8";
		String customUuid = "testUuid9";
		String logLevel = "INFO";
		String message = "message";
		Date logDate = Calendar.getInstance().getTime();

		LoggingContext context = LoggingContext.init(Maybe.just(launchUuid), Maybe.just(testUuid), rpClient, Schedulers.from(executor));
		assertThat(
				"Log wasn't logged", ReportPortal.emitLog(
						Maybe.just(customUuid),
						itemUuid -> ReportPortal.toSaveLogRQ(launchUuid, itemUuid, logLevel, logDate, new ReportPortalMessage(message))
				)
		);
		Throwable result = context.completed().blockingGet();
		assertThat(result, nullValue());

		ArgumentCaptor<List<MultipartBody.Part>> logCaptor = ArgumentCaptor.forClass(List.class);
		verify(rpClient, timeout(1000)).log(logCaptor.capture());

		// Verify basic fields
		List<SaveLogRQ> logRequests = logCaptor.getAllValues()
				.stream()
				.flatMap(rq -> TestUtils.extractJsonParts(rq).stream())
				.collect(Collectors.toList());
		verifySaveLogRQ(logRequests, logLevel, message, logDate, launchUuid, customUuid);
	}

	@Test
	@Order(Order.DEFAULT)
	@SuppressWarnings("unchecked")
	public void verify_sendStackTraceToRP_method() {
		TestUtils.mockBatchLogging(rpClient);
		String launchUuid = "launchUuid10";
		String testUuid = "testUuid10";
		String logLevel = "ERROR";
		String message = "java.lang.Throwable\n...";

		LoggingContext context = LoggingContext.init(Maybe.just(launchUuid), Maybe.just(testUuid), rpClient, Schedulers.from(executor));
		ReportPortal.sendStackTraceToRP(new Throwable());
		Throwable result = context.completed().blockingGet();
		assertThat(result, nullValue());

		ArgumentCaptor<List<MultipartBody.Part>> logCaptor = ArgumentCaptor.forClass(List.class);
		verify(rpClient, timeout(1000)).log(logCaptor.capture());

		// Verify basic fields
		List<SaveLogRQ> logRequests = logCaptor.getAllValues()
				.stream()
				.flatMap(rq -> TestUtils.extractJsonParts(rq).stream())
				.collect(Collectors.toList());
		verifySaveLogRQ(logRequests, logLevel, message, launchUuid, testUuid);
	}

	@Test
	@Order(Order.DEFAULT)
	@SuppressWarnings("unchecked")
	public void verify_sendStackTraceToRP_method_null_value() {
		TestUtils.mockBatchLogging(rpClient);
		String launchUuid = "launchUuid11";
		String testUuid = "testUuid11";
		String logLevel = "ERROR";
		String message = "Test has failed without exception";

		LoggingContext context = LoggingContext.init(Maybe.just(launchUuid), Maybe.just(testUuid), rpClient, Schedulers.from(executor));
		ReportPortal.sendStackTraceToRP(null);
		Throwable result = context.completed().blockingGet();
		assertThat(result, nullValue());

		ArgumentCaptor<List<MultipartBody.Part>> logCaptor = ArgumentCaptor.forClass(List.class);
		verify(rpClient, timeout(1000)).log(logCaptor.capture());

		// Verify basic fields
		List<SaveLogRQ> logRequests = logCaptor.getAllValues()
				.stream()
				.flatMap(rq -> TestUtils.extractJsonParts(rq).stream())
				.collect(Collectors.toList());
		verifySaveLogRQ(logRequests, logLevel, message, launchUuid, testUuid);
	}

	@Test
	@Order(Integer.MAX_VALUE)
	@SuppressWarnings("unchecked")
	public void verify_sendStackTraceToRP_method_no_format() {
		TestUtils.mockBatchLogging(rpClient);
		ListenerParameters parameters = TestUtils.standardParameters();
		parameters.setExceptionTruncate(false);
		ReportPortal reportPortal = ReportPortal.create(rpClient, parameters, executor);
		String launchUuid = "launchUuid12";
		reportPortal.withLaunch(Maybe.just(launchUuid));
		String testUuid = "testUuid12";
		String logLevel = "ERROR";
		String message = "java.lang.Throwable\n\tat com.epam";

		LoggingContext context = LoggingContext.init(Maybe.just(launchUuid), Maybe.just(testUuid), rpClient, Schedulers.from(executor));
		ReportPortal.sendStackTraceToRP(new Throwable());
		Throwable result = context.completed().blockingGet();
		assertThat(result, nullValue());

		ArgumentCaptor<List<MultipartBody.Part>> logCaptor = ArgumentCaptor.forClass(List.class);
		verify(rpClient, timeout(1000)).log(logCaptor.capture());

		// Verify basic fields
		List<SaveLogRQ> logRequests = logCaptor.getAllValues()
				.stream()
				.flatMap(rq -> TestUtils.extractJsonParts(rq).stream())
				.collect(Collectors.toList());
		verifySaveLogRQ(logRequests, logLevel, launchUuid, testUuid);
		assertThat(logRequests.get(0).getMessage(), startsWith(message));
	}
}
