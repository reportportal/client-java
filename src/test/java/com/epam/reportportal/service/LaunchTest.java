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

package com.epam.reportportal.service;

import com.epam.reportportal.annotations.Step;
import com.epam.reportportal.aspect.StepAspect;
import com.epam.reportportal.aspect.StepAspectCommon;
import com.epam.reportportal.exception.ReportPortalException;
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.statistics.StatisticsService;
import com.epam.reportportal.service.step.StepReporter;
import com.epam.reportportal.utils.ObjectUtils;
import com.epam.reportportal.utils.properties.DefaultProperties;
import com.epam.ta.reportportal.ws.model.*;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import io.reactivex.Maybe;
import org.apache.commons.lang3.RandomStringUtils;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.epam.reportportal.test.TestUtils.*;
import static com.epam.reportportal.util.test.CommonUtils.shutdownExecutorService;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class LaunchTest {

	private static final ErrorRS START_ERROR_RS;
	private static final ErrorRS FINISH_ERROR_RS;

	static {
		START_ERROR_RS = new ErrorRS();
		START_ERROR_RS.setErrorType(ErrorType.INCORRECT_REQUEST);
		START_ERROR_RS.setMessage("Incorrect Request. [Value is not allowed for field 'status'.]");

		FINISH_ERROR_RS = new ErrorRS();
		FINISH_ERROR_RS.setErrorType(ErrorType.FINISH_TIME_EARLIER_THAN_START_TIME);
		FINISH_ERROR_RS.setMessage(
				"Finish time 'Thu Jan 01 00:00:00 UTC 1970' is earlier than start time 'Tue Aug 13 13:21:31 UTC 2019' for resource with ID '5d52b9899bd1160001b8f454'");
	}

	private static final ReportPortalException START_CLIENT_EXCEPTION = new ReportPortalException(
			400,
			"Bad Request",
			START_ERROR_RS
	);

	// taken from: https://github.com/reportportal/client-java/issues/99
	private static final ReportPortalException FINISH_CLIENT_EXCEPTION = new ReportPortalException(
			406,
			"Not Acceptable",
			FINISH_ERROR_RS
	);

	@Mock
	private ReportPortalClient rpClient;
	@Mock
	private StatisticsService statisticsService;

	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	@AfterEach
	public void tearDown() {
		shutdownExecutorService(executor);
	}

	@Test
	public void launch_should_finish_all_items_even_if_one_of_finishes_failed() {
		simulateStartLaunchResponse(rpClient);
		simulateStartTestItemResponse(rpClient);
		simulateStartChildTestItemResponse(rpClient);

		Launch launch = new LaunchImpl(
				rpClient,
				STANDARD_PARAMETERS,
				standardLaunchRequest(STANDARD_PARAMETERS),
				executor
		) {
			@Override
			StatisticsService getStatisticsService() {
				return statisticsService;
			}
		};

		Maybe<String> launchUuid = launch.start();
		Maybe<String> suiteRs = launch.startTestItem(standardStartSuiteRequest());
		Maybe<String> testRs = launch.startTestItem(suiteRs, standardStartTestRequest());
		Maybe<String> stepRs = launch.startTestItem(testRs, standardStartStepRequest());

		when(rpClient.finishTestItem(eq(stepRs.blockingGet()), any())).thenThrow(FINISH_CLIENT_EXCEPTION);
		when(rpClient.finishTestItem(
				eq(testRs.blockingGet()),
				any()
		)).thenReturn(Maybe.just(new OperationCompletionRS()));
		when(rpClient.finishTestItem(
				eq(suiteRs.blockingGet()),
				any()
		)).thenReturn(Maybe.just(new OperationCompletionRS()));
		when(rpClient.finishLaunch(
				eq(launchUuid.blockingGet()),
				any()
		)).thenReturn(Maybe.just(new OperationCompletionRS()));

		launch.finishTestItem(stepRs, positiveFinishRequest());
		launch.finishTestItem(testRs, positiveFinishRequest());
		launch.finishTestItem(suiteRs, positiveFinishRequest());
		launch.finish(new FinishExecutionRQ());

		verify(rpClient).finishTestItem(eq(stepRs.blockingGet()), any());
		verify(rpClient).finishTestItem(eq(testRs.blockingGet()), any());
		verify(rpClient).finishTestItem(eq(suiteRs.blockingGet()), any());
		verify(rpClient).finishLaunch(eq(launchUuid.blockingGet()), any());
	}

	@Test
	public void launch_should_finish_all_items_even_if_one_of_starts_failed() {
		simulateStartLaunchResponse(rpClient);
		simulateStartTestItemResponse(rpClient);
		simulateStartChildTestItemResponse(rpClient);

		Launch launch = new LaunchImpl(
				rpClient,
				STANDARD_PARAMETERS,
				standardLaunchRequest(STANDARD_PARAMETERS),
				executor
		) {
			@Override
			StatisticsService getStatisticsService() {
				return statisticsService;
			}
		};

		Maybe<String> launchUuid = launch.start();
		Maybe<String> suiteRs = launch.startTestItem(standardStartSuiteRequest());
		Maybe<String> testRs = launch.startTestItem(suiteRs, standardStartTestRequest());

		when(rpClient.startTestItem(eq(testRs.blockingGet()), any())).thenThrow(START_CLIENT_EXCEPTION);
		Maybe<String> stepRs = launch.startTestItem(testRs, standardStartStepRequest());

		when(rpClient.finishTestItem(
				eq(testRs.blockingGet()),
				any()
		)).thenReturn(Maybe.just(new OperationCompletionRS()));
		when(rpClient.finishTestItem(
				eq(suiteRs.blockingGet()),
				any()
		)).thenReturn(Maybe.just(new OperationCompletionRS()));
		when(rpClient.finishLaunch(
				eq(launchUuid.blockingGet()),
				any()
		)).thenReturn(Maybe.just(new OperationCompletionRS()));

		launch.finishTestItem(stepRs, positiveFinishRequest());
		launch.finishTestItem(testRs, positiveFinishRequest());
		launch.finishTestItem(suiteRs, positiveFinishRequest());
		launch.finish(new FinishExecutionRQ());

		verify(rpClient).finishTestItem(eq(testRs.blockingGet()), any());
		verify(rpClient).finishTestItem(eq(suiteRs.blockingGet()), any());
		verify(rpClient).finishLaunch(eq(launchUuid.blockingGet()), any());
	}

	@Test
	public void launch_should_stick_to_every_thread_which_uses_it() throws ExecutionException, InterruptedException {
		simulateStartLaunchResponse(rpClient);
		simulateStartTestItemResponse(rpClient);
		simulateStartChildTestItemResponse(rpClient);

		// Verify Launch set on creation
		ExecutorService launchCreateExecutor = Executors.newSingleThreadExecutor();
		Launch launchOnCreate = launchCreateExecutor.submit(() -> new LaunchImpl(rpClient,
				STANDARD_PARAMETERS,
				standardLaunchRequest(STANDARD_PARAMETERS),
				executor
		) {
			@Override
			StatisticsService getStatisticsService() {
				return statisticsService;
			}
		}).get();
		Launch launchGet = launchCreateExecutor.submit(Launch::currentLaunch).get();
		assertThat(launchGet, sameInstance(launchOnCreate));
		shutdownExecutorService(launchCreateExecutor);

		// Verify Launch set on start
		ExecutorService launchStartExecutor = Executors.newSingleThreadExecutor();
		launchStartExecutor.submit(launchOnCreate::start).get();
		launchGet = launchStartExecutor.submit(Launch::currentLaunch).get();
		assertThat(launchGet, sameInstance(launchOnCreate));
		shutdownExecutorService(launchStartExecutor);

		// Verify Launch set on start root test item
		ExecutorService launchSuiteStartExecutor = Executors.newSingleThreadExecutor();
		Maybe<String> parent = launchSuiteStartExecutor.submit(() -> launchOnCreate.startTestItem(
				standardStartSuiteRequest())).get();
		launchGet = launchSuiteStartExecutor.submit(Launch::currentLaunch).get();
		assertThat(launchGet, sameInstance(launchOnCreate));
		shutdownExecutorService(launchSuiteStartExecutor);

		// Verify Launch set on start child test item
		ExecutorService launchChildStartExecutor = Executors.newSingleThreadExecutor();
		launchChildStartExecutor.submit(() -> launchOnCreate.startTestItem(parent, standardStartTestRequest())).get();
		launchGet = launchChildStartExecutor.submit(Launch::currentLaunch).get();
		assertThat(launchGet, sameInstance(launchOnCreate));
		shutdownExecutorService(launchChildStartExecutor);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void launch_should_send_analytics_events_if_created_with_request() {
		simulateStartLaunchResponse(rpClient);
		simulateFinishLaunchResponse(rpClient);

		StartLaunchRQ startRq = standardLaunchRequest(STANDARD_PARAMETERS);
		Launch launch = new LaunchImpl(rpClient, STANDARD_PARAMETERS, startRq, executor) {
			@Override
			StatisticsService getStatisticsService() {
				return statisticsService;
			}
		};
		launch.start();
		launch.finish(standardLaunchFinishRequest());

		ArgumentCaptor<StartLaunchRQ> startCaptor = ArgumentCaptor.forClass(StartLaunchRQ.class);
		verify(statisticsService).sendEvent(any(Maybe.class), startCaptor.capture());
		verify(statisticsService).close();

		assertThat(ObjectUtils.toString(startCaptor.getValue()), equalTo(ObjectUtils.toString(startRq)));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void launch_should_send_analytics_events_if_created_with_launch_maybe() {
		simulateFinishLaunchResponse(rpClient);

		Maybe<String> launchUuid = Maybe.just("launchUuid");
		Launch launch = new LaunchImpl(rpClient, STANDARD_PARAMETERS, launchUuid, executor) {
			@Override
			StatisticsService getStatisticsService() {
				return statisticsService;
			}
		};
		launch.start();
		launch.finish(standardLaunchFinishRequest());

		ArgumentCaptor<StartLaunchRQ> startRqCaptor = ArgumentCaptor.forClass(StartLaunchRQ.class);
		verify(statisticsService).sendEvent(any(Maybe.class), startRqCaptor.capture());
		verify(statisticsService).close();

		StartLaunchRQ startRq = startRqCaptor.getAllValues().get(0);
		assertThat(startRq.getAttributes(), notNullValue());
		assertThat(startRq.getAttributes(), hasSize(1));
		ItemAttributesRQ attribute = startRq.getAttributes().iterator().next();
		assertThat(attribute.isSystem(), equalTo(Boolean.TRUE));
		assertThat(attribute.getKey(), equalTo(DefaultProperties.AGENT.getName()));
		assertThat(attribute.getValue(), equalTo(LaunchImpl.CUSTOM_AGENT));
	}

	private final StepAspect aspect = new StepAspect();
	private static final RuntimeException DUMMY_ERROR = new IllegalStateException("Just a failure");

	@Step
	public void aFailureStep() {
		throw DUMMY_ERROR;
	}

	@Test
	public void launch_should_correctly_track_parent_items_for_annotation_based_nested_steps()
			throws NoSuchMethodException {
		simulateStartLaunchResponse(rpClient);
		simulateStartTestItemResponse(rpClient);
		simulateFinishTestItemResponse(rpClient);
		simulateStartChildTestItemResponse(rpClient);
		simulateFinishLaunchResponse(rpClient);
		simulateBatchLogResponse(rpClient);

		Launch launch = new LaunchImpl(
				rpClient,
				STANDARD_PARAMETERS,
				standardLaunchRequest(STANDARD_PARAMETERS),
				executor
		) {
			@Override
			StatisticsService getStatisticsService() {
				return statisticsService;
			}
		};

		launch.start();
		Maybe<String> suiteRs = launch.startTestItem(standardStartSuiteRequest());
		Maybe<String> testRs = launch.startTestItem(suiteRs, standardStartTestRequest());
		Maybe<String> firstStepRs = launch.startTestItem(testRs, standardStartStepRequest());
		launch.finishTestItem(firstStepRs, positiveFinishRequest());
		Maybe<String> secondStepRs = launch.startTestItem(testRs, standardStartStepRequest());

		Method method = getClass().getMethod("aFailureStep");
		Step step = method.getAnnotation(Step.class);

		aspect.startNestedStep(StepAspectCommon.getJoinPointNoParams(mock(MethodSignature.class), method), step);
		aspect.failedNestedStep(step, DUMMY_ERROR);

		FinishTestItemRQ failedFinishRq = positiveFinishRequest();
		failedFinishRq.setStatus("FAILED");
		launch.finishTestItem(secondStepRs, failedFinishRq);

		FinishTestItemRQ finishRq = positiveFinishRequest();
		finishRq.setStatus(null);
		launch.finishTestItem(testRs, finishRq);
		launch.finishTestItem(suiteRs, finishRq);
		launch.finish(standardLaunchFinishRequest());

		ArgumentCaptor<FinishTestItemRQ> captor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(rpClient, times(1)).finishTestItem(same(firstStepRs.blockingGet()), captor.capture());
		captor.getAllValues().forEach(i -> assertThat(i.getStatus(), equalTo("PASSED")));
	}

	@Test
	public void launch_should_truncate_long_item_names() {
		simulateStartLaunchResponse(rpClient);
		simulateStartTestItemResponse(rpClient);
		simulateStartChildTestItemResponse(rpClient);

		Launch launch = new LaunchImpl(
				rpClient,
				STANDARD_PARAMETERS,
				standardLaunchRequest(STANDARD_PARAMETERS),
				executor
		) {
			@Override
			StatisticsService getStatisticsService() {
				return statisticsService;
			}
		};

		StartTestItemRQ suiteRq = standardStartSuiteRequest();
		suiteRq.setName(suiteRq.getName() + RandomStringUtils.random(1025 - suiteRq.getName().length()));
		StartTestItemRQ testRq = standardStartSuiteRequest();
		testRq.setName(testRq.getName() + RandomStringUtils.random(1025 - testRq.getName().length()));

		launch.start();
		Maybe<String> suiteRs = launch.startTestItem(suiteRq);
		launch.startTestItem(suiteRs, testRq);

		ArgumentCaptor<StartTestItemRQ> suiteCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(rpClient, timeout(1000)).startTestItem(suiteCaptor.capture());
		ArgumentCaptor<StartTestItemRQ> testCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(rpClient, timeout(1000)).startTestItem(same(suiteRs.blockingGet()), testCaptor.capture());

		String suiteName = suiteCaptor.getValue().getName();
		assertThat(suiteName, allOf(hasLength(1024), endsWith("...")));

		String testName = testCaptor.getValue().getName();
		assertThat(testName, allOf(hasLength(1024), endsWith("...")));
	}

	private static void verify_attribute_truncation(Set<ItemAttributesRQ> attributes) {
		assertThat(attributes, hasSize(1));
		ItemAttributesRQ suiteAttribute = attributes.iterator().next();
		assertThat(suiteAttribute.getKey(), allOf(hasLength(128), endsWith("...")));
		assertThat(suiteAttribute.getValue(), allOf(hasLength(128), endsWith("...")));
	}

	@Test
	public void launch_should_truncate_long_attributes() {
		simulateStartLaunchResponse(rpClient);
		simulateStartTestItemResponse(rpClient);
		simulateFinishTestItemResponse(rpClient);
		simulateFinishLaunchResponse(rpClient);

		ListenerParameters parameters = standardParameters();
		String longKey = RandomStringUtils.randomAlphanumeric(129);
		String longValue = RandomStringUtils.randomAlphanumeric(129);
		parameters.setAttributes(Collections.singleton(new ItemAttributesRQ(longKey, longValue)));

		Launch launch = new LaunchImpl(rpClient, parameters, standardLaunchRequest(parameters), executor) {
			@Override
			StatisticsService getStatisticsService() {
				return statisticsService;
			}
		};

		StartTestItemRQ suiteStartRq = standardStartSuiteRequest();
		suiteStartRq.setAttributes(Collections.singleton(new ItemAttributesRQ(longKey, longValue)));
		FinishTestItemRQ suiteFinishRq = positiveFinishRequest();
		suiteFinishRq.setAttributes(Collections.singleton(new ItemAttributesRQ(longKey, longValue)));
		FinishExecutionRQ finishRq = standardLaunchFinishRequest();
		finishRq.setAttributes(Collections.singleton(new ItemAttributesRQ(longKey, longValue)));

		launch.start();
		Maybe<String> suiteRs = launch.startTestItem(suiteStartRq);
		launch.finishTestItem(suiteRs, suiteFinishRq);
		launch.finish(finishRq);

		ArgumentCaptor<StartLaunchRQ> launchStartCaptor = ArgumentCaptor.forClass(StartLaunchRQ.class);
		verify(rpClient, timeout(1000)).startLaunch(launchStartCaptor.capture());

		verify_attribute_truncation(launchStartCaptor.getValue().getAttributes());

		ArgumentCaptor<StartTestItemRQ> suiteStartCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(rpClient, timeout(1000)).startTestItem(suiteStartCaptor.capture());

		verify_attribute_truncation(suiteStartCaptor.getValue().getAttributes());

		ArgumentCaptor<FinishTestItemRQ> suiteFinishCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(rpClient, timeout(1000)).finishTestItem(anyString(), suiteFinishCaptor.capture());

		verify_attribute_truncation(suiteFinishCaptor.getValue().getAttributes());

		ArgumentCaptor<FinishExecutionRQ> launchFinishCaptor = ArgumentCaptor.forClass(FinishExecutionRQ.class);
		verify(rpClient, timeout(1000)).finishLaunch(anyString(), launchFinishCaptor.capture());

		verify_attribute_truncation(launchFinishCaptor.getValue().getAttributes());
	}

	@Test
	@Timeout(10)
	public void launch_should_not_throw_exceptions_or_hang_if_finished_and_started_again() {
		simulateStartLaunchResponse(rpClient);
		simulateFinishLaunchResponse(rpClient);
		simulateStartTestItemResponse(rpClient);
		simulateFinishTestItemResponse(rpClient);

		StartLaunchRQ startRq = standardLaunchRequest(STANDARD_PARAMETERS);
		Launch launch = new LaunchImpl(rpClient, STANDARD_PARAMETERS, startRq, executor);
		launch.start();
		Maybe<String> id = launch.startTestItem(standardStartSuiteRequest());
		launch.finishTestItem(id, positiveFinishRequest());
		launch.finish(standardLaunchFinishRequest());
		launch.finish(standardLaunchFinishRequest());

		verify(rpClient).startTestItem(any());
		verify(rpClient).finishTestItem(same(id.blockingGet()), any());

		launch.start();
		id = launch.startTestItem(standardStartSuiteRequest());
		launch.finishTestItem(id, positiveFinishRequest());
		launch.finish(standardLaunchFinishRequest());

		verify(rpClient, times(2)).startTestItem(any());
		verify(rpClient, times(1)).finishTestItem(same(id.blockingGet()), any());
	}

	@Test
	public void test_noop_launch_not_null() {
		assertThat(Launch.NOOP_LAUNCH, notNullValue());
		assertThat(Launch.NOOP_LAUNCH.getLaunch(), allOf(notNullValue(), equalTo(Maybe.empty())));
	}

	@Test
	public void test_noop_launch_returns_valid_step_reporter() {
		StepReporter reporter = Launch.NOOP_LAUNCH.getStepReporter();

		assertThat(reporter, notNullValue());
		reporter.sendStep(ItemStatus.PASSED, "Dummy step name");
		reporter.sendStep(ItemStatus.FAILED, "Dummy failure step name", new IllegalStateException());
	}

	@Test
	public void test_noop_launch_returns_valid_rp_client() {
		ReportPortalClient client = Launch.NOOP_LAUNCH.getClient();

		assertThat(client, notNullValue());
		assertThat(client.toString(), notNullValue());
		assertThat(client.hashCode(), not(0));
		assertThat(client.equals(mock(ReportPortalClient.class)), equalTo(Boolean.FALSE));
		assertThat(client.startLaunch(new StartLaunchRQ()), notNullValue());
		assertThat(client.getItemByUuid("test"), notNullValue());
	}

	@Test
	public void verify_launch_get_response() {
		simulateStartLaunchResponse(rpClient);

		Launch launch = new LaunchImpl(
				rpClient,
				STANDARD_PARAMETERS,
				standardLaunchRequest(STANDARD_PARAMETERS),
				executor
		) {
			@Override
			StatisticsService getStatisticsService() {
				return statisticsService;
			}
		};

		Maybe<String> launchUuid = launch.start();
		assertThat(launchUuid, notNullValue());
		assertThat(launchUuid.blockingGet(), not(blankOrNullString()));

		Maybe<String> getLaunch = launch.getLaunch();
		assertThat(getLaunch, sameInstance(launchUuid));
	}
}
