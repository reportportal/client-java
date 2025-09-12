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
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.reportportal.utils.StaticStructuresUtils;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Maybe;
import okhttp3.MultipartBody;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.*;

@SuppressWarnings({ "unchecked", "ResultOfMethodCallIgnored" })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReportPortalLoggingTest {

	private final ListenerParameters parameters = TestUtils.standardParameters();
	private ReportPortalClient rpClient;
	private ReportPortal rp;
	private ExecutorService executor;

	@BeforeEach
	public void setup() {
		executor = CommonUtils.testExecutor();
		rpClient = mock(ReportPortalClient.class);
		rp = ReportPortal.create(rpClient, parameters, executor);
	}

	@AfterEach
	public void tearDown() {
		CommonUtils.shutdownExecutorService(executor);
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
	public void verify_emitLog_no_context() {
		String logLevel = "INFO";
		String message = "message";

		assertThat("Log was logged", !ReportPortal.emitLog(message, logLevel, Calendar.getInstance().getTime()));

		ArgumentCaptor<List<MultipartBody.Part>> logCaptor = ArgumentCaptor.forClass(List.class);
		verify(rpClient, after(1000).times(0)).log(logCaptor.capture());
	}

	@Test
	@Order(1)
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
	public void verify_emitLaunchLog_no_context() {
		String logLevel = "INFO";
		String message = "message";

		assertThat("Log was logged", !ReportPortal.emitLaunchLog(message, logLevel, Calendar.getInstance().getTime()));

		ArgumentCaptor<List<MultipartBody.Part>> logCaptor = ArgumentCaptor.forClass(List.class);
		verify(rpClient, after(1000).times(0)).log(logCaptor.capture());
	}

	@Test
	@Order(Integer.MAX_VALUE)
	public void verify_emitLog_simple_message() {
		String launchUuid = "launchUuid";
		TestUtils.mockLaunch(rpClient, launchUuid);
		String testUuid = "testUuid";
		TestUtils.mockStartTestItem(rpClient, testUuid);
		TestUtils.mockBatchLogging(rpClient);
		String logLevel = "INFO";
		String message = "message";
		Date logDate = Calendar.getInstance().getTime();

		Launch launch = rp.newLaunch(TestUtils.standardLaunchRequest(parameters));
		launch.start().blockingGet();
		launch.startTestItem(TestUtils.standardStartTestRequest()).blockingGet();
		assertThat("Log wasn't logged", ReportPortal.emitLog(message, logLevel, logDate));

		launch.finish(TestUtils.standardLaunchFinishRequest());

		ArgumentCaptor<List<MultipartBody.Part>> logCaptor = ArgumentCaptor.forClass(List.class);
		verify(rpClient, timeout(1000).times(2)).log(logCaptor.capture());

		// Verify basic fields
		List<SaveLogRQ> logRequests = logCaptor.getAllValues()
				.stream()
				.flatMap(rq -> TestUtils.extractJsonParts(rq).stream())
				.filter(rq -> !StaticStructuresUtils.LAUNCH_FINISHED_MESSAGE.equals(rq.getMessage()))
				.collect(Collectors.toList());
		verifySaveLogRQNullFile(logRequests, logLevel, message, logDate, launchUuid, testUuid);
	}

	@Test
	@Order(Order.DEFAULT)
	public void verify_emitLog_message_with_file() throws IOException {
		String launchUuid = "launchUuid1";
		TestUtils.mockLaunch(rpClient, launchUuid);
		String testUuid = "testUuid1";
		TestUtils.mockStartTestItem(rpClient, testUuid);
		TestUtils.mockBatchLogging(rpClient);
		String logLevel = "INFO";
		String message = "message";
		Date logDate = Calendar.getInstance().getTime();
		String filePath = "pug/lucky.jpg";
		File file = new File(filePath);

		Launch launch = rp.newLaunch(TestUtils.standardLaunchRequest(parameters));
		launch.start().blockingGet();
		launch.startTestItem(TestUtils.standardStartTestRequest()).blockingGet();
		assertThat("Log wasn't logged", ReportPortal.emitLog(message, logLevel, logDate, file));

		launch.finish(TestUtils.standardLaunchFinishRequest());

		ArgumentCaptor<List<MultipartBody.Part>> logCaptor = ArgumentCaptor.forClass(List.class);
		verify(rpClient, timeout(1000).times(2)).log(logCaptor.capture());

		// Verify basic fields
		List<SaveLogRQ> logRequests = logCaptor.getAllValues()
				.stream()
				.flatMap(rq -> TestUtils.extractJsonParts(rq).stream())
				.filter(rq -> !StaticStructuresUtils.LAUNCH_FINISHED_MESSAGE.equals(rq.getMessage()))
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
	public void verify_emitLaunchLog_simple_message() {
		String launchUuid = "launchUuid2";
		TestUtils.mockLaunch(rpClient, launchUuid);
		TestUtils.mockBatchLogging(rpClient);
		String logLevel = "INFO";
		String message = "message";
		Launch launch = rp.newLaunch(TestUtils.standardLaunchRequest(parameters));
		Date logDate = Calendar.getInstance().getTime();

		launch.start().blockingGet();
		assertThat("Log wasn't logged", ReportPortal.emitLaunchLog(message, logLevel, logDate));

		launch.finish(TestUtils.standardLaunchFinishRequest());

		ArgumentCaptor<List<MultipartBody.Part>> logCaptor = ArgumentCaptor.forClass(List.class);
		verify(rpClient, timeout(1000).times(2)).log(logCaptor.capture());

		// Verify basic fields
		List<SaveLogRQ> logRequests = logCaptor.getAllValues()
				.stream()
				.flatMap(rq -> TestUtils.extractJsonParts(rq).stream())
				.filter(rq -> !StaticStructuresUtils.LAUNCH_FINISHED_MESSAGE.equals(rq.getMessage()))
				.collect(Collectors.toList());
		verifySaveLogRQNullFile(logRequests, logLevel, message, logDate, launchUuid, null);
	}

	@Test
	@Order(Order.DEFAULT)
	public void verify_emitLaunchLog_message_with_file() throws IOException {
		String launchUuid = "launchUuid3";
		TestUtils.mockLaunch(rpClient, launchUuid);
		TestUtils.mockBatchLogging(rpClient);
		String logLevel = "INFO";
		String message = "message";
		Date logDate = Calendar.getInstance().getTime();
		String filePath = "pug/unlucky.jpg";
		File file = new File(filePath);

		Launch launch = rp.newLaunch(TestUtils.standardLaunchRequest(parameters));
		launch.start().blockingGet();
		assertThat("Log wasn't logged", ReportPortal.emitLaunchLog(message, logLevel, logDate, file));

		launch.finish(TestUtils.standardLaunchFinishRequest());

		ArgumentCaptor<List<MultipartBody.Part>> logCaptor = ArgumentCaptor.forClass(List.class);
		verify(rpClient, timeout(1000).times(2)).log(logCaptor.capture());

		// Verify basic fields
		List<SaveLogRQ> logRequests = logCaptor.getAllValues()
				.stream()
				.flatMap(rq -> TestUtils.extractJsonParts(rq).stream())
				.filter(rq -> !StaticStructuresUtils.LAUNCH_FINISHED_MESSAGE.equals(rq.getMessage()))
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
	public void verify_emitLog_method_with_ReportPortalMessage() throws IOException {
		String launchUuid = "launchUuid4";
		TestUtils.mockLaunch(rpClient, launchUuid);
		String testUuid = "testUuid4";
		TestUtils.mockStartTestItem(rpClient, testUuid);
		TestUtils.mockBatchLogging(rpClient);
		String logLevel = "INFO";
		String message = "message";
		Date logDate = Calendar.getInstance().getTime();
		String filePath = "pug/unlucky.jpg";
		File file = new File(filePath);

		Launch launch = rp.newLaunch(TestUtils.standardLaunchRequest(parameters));
		launch.start().blockingGet();
		launch.startTestItem(TestUtils.standardStartTestRequest()).blockingGet();
		assertThat("Log wasn't logged", ReportPortal.emitLog(new ReportPortalMessage(file, message), logLevel, logDate));

		launch.finish(TestUtils.standardLaunchFinishRequest());

		ArgumentCaptor<List<MultipartBody.Part>> logCaptor = ArgumentCaptor.forClass(List.class);
		verify(rpClient, timeout(1000).times(2)).log(logCaptor.capture());

		// Verify basic fields
		List<SaveLogRQ> logRequests = logCaptor.getAllValues()
				.stream()
				.flatMap(rq -> TestUtils.extractJsonParts(rq).stream())
				.filter(rq -> !StaticStructuresUtils.LAUNCH_FINISHED_MESSAGE.equals(rq.getMessage()))
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
	public void verify_emitLog_method_with_ReportPortalMessage_no_file() {
		String launchUuid = "launchUuid5";
		TestUtils.mockLaunch(rpClient, launchUuid);
		String testUuid = "testUuid5";
		TestUtils.mockStartTestItem(rpClient, testUuid);
		TestUtils.mockBatchLogging(rpClient);
		String logLevel = "INFO";
		String message = "message";
		Date logDate = Calendar.getInstance().getTime();

		Launch launch = rp.newLaunch(TestUtils.standardLaunchRequest(parameters));
		launch.start().blockingGet();
		launch.startTestItem(TestUtils.standardStartTestRequest()).blockingGet();
		assertThat("Log wasn't logged", ReportPortal.emitLog(new ReportPortalMessage(message), logLevel, logDate));

		launch.finish(TestUtils.standardLaunchFinishRequest());

		ArgumentCaptor<List<MultipartBody.Part>> logCaptor = ArgumentCaptor.forClass(List.class);
		verify(rpClient, timeout(1000).times(2)).log(logCaptor.capture());

		// Verify basic fields
		List<SaveLogRQ> logRequests = logCaptor.getAllValues()
				.stream()
				.flatMap(rq -> TestUtils.extractJsonParts(rq).stream())
				.filter(rq -> !StaticStructuresUtils.LAUNCH_FINISHED_MESSAGE.equals(rq.getMessage()))
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
	public void verify_emitLaunchLog_method_with_ReportPortalMessage() throws IOException {
		String launchUuid = "launchUuid6";
		TestUtils.mockLaunch(rpClient, launchUuid);
		TestUtils.mockBatchLogging(rpClient);
		String logLevel = "INFO";
		String message = "message";
		Date logDate = Calendar.getInstance().getTime();
		String filePath = "pug/unlucky.jpg";
		File file = new File(filePath);

		Launch launch = rp.newLaunch(TestUtils.standardLaunchRequest(parameters));
		launch.start().blockingGet();
		assertThat("Log wasn't logged", ReportPortal.emitLaunchLog(new ReportPortalMessage(file, message), logLevel, logDate));

		launch.finish(TestUtils.standardLaunchFinishRequest());

		ArgumentCaptor<List<MultipartBody.Part>> logCaptor = ArgumentCaptor.forClass(List.class);
		verify(rpClient, timeout(1000).times(2)).log(logCaptor.capture());

		// Verify basic fields
		List<SaveLogRQ> logRequests = logCaptor.getAllValues()
				.stream()
				.flatMap(rq -> TestUtils.extractJsonParts(rq).stream())
				.filter(rq -> !StaticStructuresUtils.LAUNCH_FINISHED_MESSAGE.equals(rq.getMessage()))
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
	public void verify_emitLaunchLog_method_with_ReportPortalMessage_no_file() {
		String launchUuid = "launchUuid7";
		TestUtils.mockLaunch(rpClient, launchUuid);
		TestUtils.mockBatchLogging(rpClient);
		String logLevel = "INFO";
		String message = "message";
		Date logDate = Calendar.getInstance().getTime();

		Launch launch = rp.newLaunch(TestUtils.standardLaunchRequest(parameters));
		launch.start().blockingGet();
		assertThat("Log wasn't logged", ReportPortal.emitLaunchLog(new ReportPortalMessage(message), logLevel, logDate));

		launch.finish(TestUtils.standardLaunchFinishRequest());

		ArgumentCaptor<List<MultipartBody.Part>> logCaptor = ArgumentCaptor.forClass(List.class);
		// One log for message, one log for launch finished message
		verify(rpClient, timeout(1000).times(2)).log(logCaptor.capture());

		// Verify basic fields
		List<SaveLogRQ> logRequests = logCaptor.getAllValues()
				.stream()
				.flatMap(rq -> TestUtils.extractJsonParts(rq).stream())
				.filter(rq -> !StaticStructuresUtils.LAUNCH_FINISHED_MESSAGE.equals(rq.getMessage()))
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
	public void verify_emitLog_method_with_specified_id() {
		String launchUuid = "launchUuid8";
		TestUtils.mockLaunch(rpClient, launchUuid);
		String testUuid = "testUuid8";
		TestUtils.mockStartTestItem(rpClient, testUuid);
		String customUuid = "testUuid9";
		TestUtils.mockStartTestItem(rpClient, customUuid);
		TestUtils.mockBatchLogging(rpClient);
		String logLevel = "INFO";
		String message = "message";
		Date logDate = Calendar.getInstance().getTime();

		Launch launch = rp.newLaunch(TestUtils.standardLaunchRequest(parameters));
		launch.start().blockingGet();
		launch.startTestItem(TestUtils.standardStartTestRequest()).blockingGet();
		assertThat(
				"Log wasn't logged", ReportPortal.emitLog(
						Maybe.just(customUuid),
						itemUuid -> ReportPortal.toSaveLogRQ(launchUuid, itemUuid, logLevel, logDate, new ReportPortalMessage(message))
				)
		);

		launch.finish(TestUtils.standardLaunchFinishRequest());

		ArgumentCaptor<List<MultipartBody.Part>> logCaptor = ArgumentCaptor.forClass(List.class);
		verify(rpClient, timeout(1000).times(2)).log(logCaptor.capture());

		// Verify basic fields
		List<SaveLogRQ> logRequests = logCaptor.getAllValues()
				.stream()
				.flatMap(rq -> TestUtils.extractJsonParts(rq).stream())
				.filter(rq -> !StaticStructuresUtils.LAUNCH_FINISHED_MESSAGE.equals(rq.getMessage()))
				.collect(Collectors.toList());
		verifySaveLogRQ(logRequests, logLevel, message, logDate, launchUuid, customUuid);
	}

	@Test
	@Order(Order.DEFAULT)
	public void verify_sendStackTraceToRP_method() {
		String launchUuid = "launchUuid10";
		TestUtils.mockLaunch(rpClient, launchUuid);
		String testUuid = "testUuid10";
		TestUtils.mockStartTestItem(rpClient, testUuid);
		TestUtils.mockBatchLogging(rpClient);
		String logLevel = "ERROR";
		String message = "java.lang.Throwable\n...";

		Launch launch = rp.newLaunch(TestUtils.standardLaunchRequest(parameters));
		launch.start().blockingGet();
		launch.startTestItem(TestUtils.standardStartTestRequest()).blockingGet();
		ReportPortal.sendStackTraceToRP(new Throwable());

		launch.finish(TestUtils.standardLaunchFinishRequest());

		ArgumentCaptor<List<MultipartBody.Part>> logCaptor = ArgumentCaptor.forClass(List.class);
		verify(rpClient, timeout(1000).times(2)).log(logCaptor.capture());

		// Verify basic fields
		List<SaveLogRQ> logRequests = logCaptor.getAllValues()
				.stream()
				.flatMap(rq -> TestUtils.extractJsonParts(rq).stream())
				.filter(rq -> !StaticStructuresUtils.LAUNCH_FINISHED_MESSAGE.equals(rq.getMessage()))
				.collect(Collectors.toList());
		verifySaveLogRQ(logRequests, logLevel, message, launchUuid, testUuid);
	}

	@Test
	@Order(Order.DEFAULT)
	public void verify_sendStackTraceToRP_method_null_value() {
		String launchUuid = "launchUuid11";
		TestUtils.mockLaunch(rpClient, launchUuid);
		String testUuid = "testUuid11";
		TestUtils.mockStartTestItem(rpClient, testUuid);
		TestUtils.mockBatchLogging(rpClient);
		String logLevel = "ERROR";
		String message = "Test has failed without exception";

		Launch launch = rp.newLaunch(TestUtils.standardLaunchRequest(parameters));
		launch.start().blockingGet();
		launch.startTestItem(TestUtils.standardStartTestRequest()).blockingGet();
		ReportPortal.sendStackTraceToRP(null);

		launch.finish(TestUtils.standardLaunchFinishRequest());

		ArgumentCaptor<List<MultipartBody.Part>> logCaptor = ArgumentCaptor.forClass(List.class);
		verify(rpClient, timeout(1000).times(2)).log(logCaptor.capture());

		// Verify basic fields
		List<SaveLogRQ> logRequests = logCaptor.getAllValues()
				.stream()
				.flatMap(rq -> TestUtils.extractJsonParts(rq).stream())
				.filter(rq -> !StaticStructuresUtils.LAUNCH_FINISHED_MESSAGE.equals(rq.getMessage()))
				.collect(Collectors.toList());
		verifySaveLogRQ(logRequests, logLevel, message, launchUuid, testUuid);
	}

	@Test
	@Order(Integer.MAX_VALUE)
	public void verify_sendStackTraceToRP_method_no_format() {
		String launchUuid = "launchUuid12";
		TestUtils.mockLaunch(rpClient, launchUuid);
		String testUuid = "testUuid12";
		TestUtils.mockStartTestItem(rpClient, testUuid);
		TestUtils.mockBatchLogging(rpClient);
		ListenerParameters parameters = TestUtils.standardParameters();
		parameters.setExceptionTruncate(false);
		ReportPortal reportPortal = ReportPortal.create(rpClient, parameters, executor);
		Launch launch = reportPortal.newLaunch(TestUtils.standardLaunchRequest(parameters));
		String logLevel = "ERROR";
		String message = "java.lang.Throwable\n\tat com.epam";

		launch.start().blockingGet();
		launch.startTestItem(TestUtils.standardStartTestRequest()).blockingGet();
		ReportPortal.sendStackTraceToRP(new Throwable());

		launch.finish(TestUtils.standardLaunchFinishRequest());

		ArgumentCaptor<List<MultipartBody.Part>> logCaptor = ArgumentCaptor.forClass(List.class);
		verify(rpClient, timeout(1000).times(2)).log(logCaptor.capture());

		// Verify basic fields
		List<SaveLogRQ> logRequests = logCaptor.getAllValues()
				.stream()
				.flatMap(rq -> TestUtils.extractJsonParts(rq).stream())
				.filter(rq -> !StaticStructuresUtils.LAUNCH_FINISHED_MESSAGE.equals(rq.getMessage()))
				.collect(Collectors.toList());
		verifySaveLogRQ(logRequests, logLevel, launchUuid, testUuid);
		assertThat(logRequests.get(0).getMessage(), startsWith(message));
	}
}
