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
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Maybe;
import okhttp3.MultipartBody;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;

import javax.annotation.Nullable;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LoggingContextTest {

	private ListenerParameters parameters;
	private ExecutorService executor;

	@BeforeEach
	public void setUp() {
		parameters = TestUtils.standardParameters();
		executor = Executors.newSingleThreadExecutor();
	}

	@AfterEach
	public void tearDown() {
		CommonUtils.shutdownExecutorService(executor);
	}

	@Test
	@Order(1)
	public void test_logging_context_current_null_safety() {
		assertNull(LoggingContext.context());
	}

	@Test
	public void test_logging_context_init() {
		LoggingContext context = LoggingContext.init(Maybe.just("item_id"));
		assertThat(LoggingContext.context(), sameInstance(context));
	}

	@Test
	public void test_second_logging_context_init_appends_instance_to_deque() {
		LoggingContext.init(Maybe.just("item_id"));
		LoggingContext context2 = LoggingContext.init(Maybe.just("item_id2"));
		assertThat(LoggingContext.context(), sameInstance(context2));
	}

	@Test
	public void test_dispose_method_removes_context() {
		LoggingContext context = LoggingContext.init(Maybe.just("item_id"));
		LoggingContext.dispose();
		assertThat(LoggingContext.context(), anyOf(nullValue(), not(sameInstance(context))));
	}

	@SuppressWarnings("unchecked")
	public void test_emit_with_item_uuid_passed_request_to_launch_log(Consumer<LoggingContext> emitCall,
			@Nullable String expectedItemUuid) {
		// Mock client
		ReportPortalClient client = mock(ReportPortalClient.class);
		String launchUuid = "launchUuid-test";
		TestUtils.mockStartLaunch(client, launchUuid);
		TestUtils.mockBatchLogging(client);

		// Create launch
		new LaunchImpl(client, parameters, TestUtils.standardLaunchRequest(parameters), executor);

		// Create LoggingContext
		String contextItemUuid = "context-item-uuid";
		LoggingContext context = LoggingContext.init(Maybe.just(contextItemUuid));

		// Emit log
		emitCall.accept(context);

		// Verify log request
		ArgumentCaptor<List<MultipartBody.Part>> logCaptor = ArgumentCaptor.forClass(List.class);
		verify(client, timeout(1000)).log(logCaptor.capture());

		// Extract and verify log contents
		List<SaveLogRQ> logRequests = logCaptor.getAllValues()
				.stream()
				.flatMap(rq -> TestUtils.extractJsonParts(rq).stream())
				.collect(Collectors.toList());

		assertThat(logRequests, hasSize(1));
		SaveLogRQ capturedRequest = logRequests.get(0);
		// Should use the explicit item UUID instead of the context item UUID
		assertThat(capturedRequest.getItemUuid(), equalTo(expectedItemUuid == null ? contextItemUuid : expectedItemUuid));
		assertThat(capturedRequest.getLaunchUuid(), equalTo(launchUuid));
		assertThat(capturedRequest.getLevel(), equalTo(LogLevel.INFO.name()));
		assertThat(capturedRequest.getMessage(), equalTo("Test log message"));

		// Clean up
		context.disposed();
	}

	@Test
	public void test_emit_without_item_uuid_passes_request_to_launch_log() {
		test_emit_with_item_uuid_passed_request_to_launch_log(
				context -> context.emit(uuid -> {
					SaveLogRQ rq = new SaveLogRQ();
					rq.setItemUuid(uuid);
					rq.setLevel(LogLevel.INFO.name());
					rq.setLogTime(Calendar.getInstance().getTime());
					rq.setMessage("Test log message");
					return rq;
				}), null
		);
	}

	@Test
	public void test_emit_with_item_uuid_passed_request_to_launch_log() {
		// Define explicit item UUID to use
		String explicitItemUuid = "explicit-item-uuid";
		Maybe<String> itemUuidMaybe = Maybe.just(explicitItemUuid);

		// Emit log with explicit item UUID
		test_emit_with_item_uuid_passed_request_to_launch_log(
				context -> context.emit(
						itemUuidMaybe, uuid -> {
							SaveLogRQ rq = new SaveLogRQ();
							rq.setItemUuid(uuid);
							rq.setLevel(LogLevel.INFO.name());
							rq.setLogTime(Calendar.getInstance().getTime());
							rq.setMessage("Test log message");
							return rq;
						}
				), explicitItemUuid
		);
	}
}
