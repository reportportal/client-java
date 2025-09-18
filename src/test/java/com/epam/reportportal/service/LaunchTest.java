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
import com.epam.reportportal.test.TestUtils;
import com.epam.reportportal.utils.ObjectUtils;
import com.epam.reportportal.utils.StaticStructuresUtils;
import com.epam.reportportal.utils.properties.DefaultProperties;
import com.epam.ta.reportportal.ws.model.*;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import com.epam.ta.reportportal.ws.model.issue.Issue;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import io.reactivex.Maybe;
import jakarta.annotation.Nonnull;
import org.apache.commons.lang3.RandomStringUtils;
import org.aspectj.lang.reflect.MethodSignature;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.epam.reportportal.test.TestUtils.*;
import static com.epam.reportportal.util.test.CommonUtils.shutdownExecutorService;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SuppressWarnings({ "ReactiveStreamsUnusedPublisher", "ResultOfMethodCallIgnored" })
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

	private static final ReportPortalException START_CLIENT_EXCEPTION = new ReportPortalException(400, "Bad Request", START_ERROR_RS);

	// taken from: https://github.com/reportportal/client-java/issues/99
	private static final ReportPortalException FINISH_CLIENT_EXCEPTION = new ReportPortalException(406, "Not Acceptable", FINISH_ERROR_RS);

	@Mock
	private ReportPortalClient rpClient;
	@Mock
	private StatisticsService statisticsService;

	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	@AfterEach
	public void tearDown() {
		shutdownExecutorService(executor);
	}

	@Nonnull
	private Launch createLaunch(@Nonnull StartLaunchRQ startRq, @Nonnull ListenerParameters parameters) {
		return new LaunchImpl(rpClient, parameters, startRq, executor) {
			@Override
			StatisticsService getStatisticsService() {
				return statisticsService;
			}
		};
	}

	@Nonnull
	private Launch createLaunch(@Nonnull StartLaunchRQ startRq) {
		return createLaunch(startRq, standardParameters());
	}

	@Nonnull
	private Launch createLaunch(@Nonnull ListenerParameters parameters) {
		return createLaunch(standardLaunchRequest(parameters), parameters);
	}

	@Nonnull
	private Launch createLaunch() {
		return createLaunch(standardLaunchRequest(STANDARD_PARAMETERS));
	}

	@Test
	public void launch_should_finish_all_items_even_if_one_of_finishes_failed() {
		simulateStartLaunchResponse(rpClient);
		simulateStartTestItemResponse(rpClient);
		simulateStartChildTestItemResponse(rpClient);
		simulateBatchLogResponse(rpClient);
		Launch launch = createLaunch();

		Maybe<String> launchUuid = launch.start();
		Maybe<String> suiteRs = launch.startTestItem(standardStartSuiteRequest());
		Maybe<String> testRs = launch.startTestItem(suiteRs, standardStartTestRequest());
		Maybe<String> stepRs = launch.startTestItem(testRs, standardStartStepRequest());

		when(rpClient.finishTestItem(eq(stepRs.blockingGet()), any())).thenThrow(FINISH_CLIENT_EXCEPTION);
		when(rpClient.finishTestItem(eq(testRs.blockingGet()), any())).thenReturn(Maybe.just(new OperationCompletionRS()));
		when(rpClient.finishTestItem(eq(suiteRs.blockingGet()), any())).thenReturn(Maybe.just(new OperationCompletionRS()));
		when(rpClient.finishLaunch(eq(launchUuid.blockingGet()), any())).thenReturn(Maybe.just(new OperationCompletionRS()));

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
		Launch launch = createLaunch();

		Maybe<String> launchUuid = launch.start();
		Maybe<String> suiteRs = launch.startTestItem(standardStartSuiteRequest());
		Maybe<String> testRs = launch.startTestItem(suiteRs, standardStartTestRequest());

		when(rpClient.startTestItem(eq(testRs.blockingGet()), any())).thenThrow(START_CLIENT_EXCEPTION);
		Maybe<String> stepRs = launch.startTestItem(testRs, standardStartStepRequest());

		when(rpClient.finishTestItem(eq(testRs.blockingGet()), any())).thenReturn(Maybe.just(new OperationCompletionRS()));
		when(rpClient.finishTestItem(eq(suiteRs.blockingGet()), any())).thenReturn(Maybe.just(new OperationCompletionRS()));
		when(rpClient.finishLaunch(eq(launchUuid.blockingGet()), any())).thenReturn(Maybe.just(new OperationCompletionRS()));

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
		simulateBatchLogResponse(rpClient);
		simulateFinishLaunchResponse(rpClient);

		// Verify Launch set on creation
		ExecutorService launchCreateExecutor = Executors.newSingleThreadExecutor();
		Launch launchOnCreate = launchCreateExecutor.submit(() -> this.createLaunch()).get();
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
		Maybe<String> parent = launchSuiteStartExecutor.submit(() -> launchOnCreate.startTestItem(standardStartSuiteRequest())).get();
		launchGet = launchSuiteStartExecutor.submit(Launch::currentLaunch).get();

		assertThat(launchGet, sameInstance(launchOnCreate));
		shutdownExecutorService(launchSuiteStartExecutor);

		// Verify Launch set on start child test item
		ExecutorService launchChildStartExecutor = Executors.newSingleThreadExecutor();
		launchChildStartExecutor.submit(() -> launchOnCreate.startTestItem(parent, standardStartTestRequest())).get();
		launchGet = launchChildStartExecutor.submit(Launch::currentLaunch).get();

		assertThat(launchGet, sameInstance(launchOnCreate));

		launchOnCreate.finish(TestUtils.standardLaunchFinishRequest());
		shutdownExecutorService(launchChildStartExecutor);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void launch_should_send_analytics_events_if_created_with_request() {
		simulateStartLaunchResponse(rpClient);
		simulateBatchLogResponse(rpClient);
		simulateFinishLaunchResponse(rpClient);

		StartLaunchRQ startRq = standardLaunchRequest(STANDARD_PARAMETERS);
		Launch launch = createLaunch(startRq);
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
		simulateBatchLogResponse(rpClient);
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
	public void launch_should_correctly_track_parent_items_for_annotation_based_nested_steps() throws NoSuchMethodException {
		simulateStartLaunchResponse(rpClient);
		simulateStartTestItemResponse(rpClient);
		simulateFinishTestItemResponse(rpClient);
		simulateStartChildTestItemResponse(rpClient);
		simulateBatchLogResponse(rpClient);
		simulateFinishLaunchResponse(rpClient);
		Launch launch = createLaunch();

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
		Launch launch = createLaunch();

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
		simulateBatchLogResponse(rpClient);
		simulateFinishLaunchResponse(rpClient);

		ListenerParameters parameters = standardParameters();
		String longKey = RandomStringUtils.randomAlphanumeric(129);
		String longValue = RandomStringUtils.randomAlphanumeric(129);
		parameters.setAttributes(Collections.singleton(new ItemAttributesRQ(longKey, longValue)));

		Launch launch = createLaunch(parameters);

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
		simulateBatchLogResponse(rpClient);
		simulateFinishLaunchResponse(rpClient);
		simulateStartTestItemResponse(rpClient);
		simulateFinishTestItemResponse(rpClient);

		Launch launch = createLaunch();
		launch.start();
		Maybe<String> id = launch.startTestItem(standardStartSuiteRequest());
		launch.finishTestItem(id, positiveFinishRequest());
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
		Launch launch = createLaunch();

		Maybe<String> launchUuid = launch.start();
		assertThat(launchUuid, notNullValue());
		assertThat(launchUuid.blockingGet(), not(blankOrNullString()));

		Maybe<String> getLaunch = launch.getLaunch();
		assertThat(getLaunch, sameInstance(launchUuid));
	}

	@Test
	public void verify_launch_print() {
		simulateStartLaunchResponse(rpClient);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream testStream = new PrintStream(baos, false, StandardCharsets.UTF_8);
		ListenerParameters parameters = standardParameters();
		parameters.setPrintLaunchUuid(true);
		parameters.setPrintLaunchUuidOutput(testStream);
		Launch launch = createLaunch(parameters);

		String launchUuid = launch.start().blockingGet();
		Awaitility.await("Wait for Launch UUID output").atMost(Duration.ofSeconds(10)).until(() -> {
			testStream.flush();
			return baos.size() > 0;
		});
		String result = baos.toString(StandardCharsets.UTF_8);
		assertThat(result, endsWith(launchUuid + System.lineSeparator()));
	}

	@ParameterizedTest
	@ValueSource(strings = { "FAILED", "SKIPPED" })
	public void verify_external_issue_filling_logic(String itemStatus) {
		simulateStartLaunchResponse(rpClient);
		simulateStartTestItemResponse(rpClient);
		simulateFinishTestItemResponse(rpClient);
		ListenerParameters parameters = standardParameters();
		parameters.setBtsUrl("https://example.com");
		parameters.setBtsProjectId("example_project");
		parameters.setBtsIssueUrl("https://example.com/{bts_project}/issue/{issue_id}");
		Launch launch = createLaunch(parameters);

		String launchUuid = launch.start().blockingGet();
		StartTestItemRQ itemRq = standardStartStepRequest();
		itemRq.setLaunchUuid(launchUuid);
		Maybe<String> itemId = launch.startTestItem(itemRq);
		FinishTestItemRQ finishRq = positiveFinishRequest();
		finishRq.setStatus(itemStatus);
		Issue issue = new Issue();
		issue.setIssueType("pb001");
		issue.setComment("issue_comment");
		Issue.ExternalSystemIssue externalIssue = new Issue.ExternalSystemIssue();
		externalIssue.setTicketId("RP-001");
		issue.setExternalSystemIssues(Collections.singleton(externalIssue));
		finishRq.setIssue(issue);
		launch.finishTestItem(itemId, finishRq).blockingGet();

		ArgumentCaptor<FinishTestItemRQ> captor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(rpClient).finishTestItem(eq(itemId.blockingGet()), captor.capture());

		FinishTestItemRQ resultFinishRq = captor.getValue();
		assertThat(resultFinishRq.getIssue().getExternalSystemIssues(), hasSize(1));
		Issue.ExternalSystemIssue resultExternalIssue = resultFinishRq.getIssue().getExternalSystemIssues().iterator().next();
		assertThat(resultExternalIssue.getTicketId(), equalTo("RP-001"));
		assertThat(resultExternalIssue.getUrl(), equalTo("https://example.com/example_project/issue/RP-001"));
		assertThat(resultExternalIssue.getBtsUrl(), equalTo("https://example.com"));
		assertThat(resultExternalIssue.getBtsProject(), equalTo("example_project"));
	}

	@Test
	public void verify_failing_item_with_issue_on_passed_test() {
		simulateStartLaunchResponse(rpClient);
		simulateStartTestItemResponse(rpClient);
		simulateFinishTestItemResponse(rpClient);
		Launch launch = createLaunch();

		String launchUuid = launch.start().blockingGet();
		StartTestItemRQ itemRq = standardStartStepRequest();
		itemRq.setLaunchUuid(launchUuid);
		Maybe<String> itemId = launch.startTestItem(itemRq);
		FinishTestItemRQ finishRq = positiveFinishRequest();
		finishRq.setIssue(new Issue());
		launch.finishTestItem(itemId, finishRq).blockingGet();

		ArgumentCaptor<FinishTestItemRQ> captor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(rpClient).finishTestItem(eq(itemId.blockingGet()), captor.capture());
		FinishTestItemRQ resultFinishRq = captor.getValue();

		assertThat(resultFinishRq.getIssue(), notNullValue());
		assertThat(resultFinishRq.getStatus(), equalTo(ItemStatus.FAILED.name()));
		Issue issue = resultFinishRq.getIssue();
		assertThat(issue.getIssueType(), equalTo(StaticStructuresUtils.REDUNDANT_ISSUE.getIssueType()));
		assertThat(issue.getComment(), equalTo(StaticStructuresUtils.REDUNDANT_ISSUE.getComment()));
		assertThat(issue.getExternalSystemIssues(), nullValue());
	}

	@Test
	public void verify_not_failing_item_with_issue_on_passed_test_if_it_turned_off() {
		simulateStartLaunchResponse(rpClient);
		simulateStartTestItemResponse(rpClient);
		simulateFinishTestItemResponse(rpClient);
		ListenerParameters parameters = standardParameters();
		parameters.setBtsIssueFail(false);
		Launch launch = createLaunch(parameters);

		String launchUuid = launch.start().blockingGet();
		StartTestItemRQ itemRq = standardStartStepRequest();
		itemRq.setLaunchUuid(launchUuid);
		Maybe<String> itemId = launch.startTestItem(itemRq);
		FinishTestItemRQ finishRq = positiveFinishRequest();
		finishRq.setIssue(new Issue());
		launch.finishTestItem(itemId, finishRq).blockingGet();

		ArgumentCaptor<FinishTestItemRQ> captor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(rpClient).finishTestItem(eq(itemId.blockingGet()), captor.capture());
		FinishTestItemRQ resultFinishRq = captor.getValue();

		assertThat(resultFinishRq.getIssue(), nullValue());
	}

	@Test
	public void verify_external_issue_no_override_if_set() {
		simulateStartLaunchResponse(rpClient);
		simulateStartTestItemResponse(rpClient);
		simulateFinishTestItemResponse(rpClient);
		ListenerParameters parameters = standardParameters();
		parameters.setBtsUrl("https://example.com");
		parameters.setBtsProjectId("example_project");
		parameters.setBtsIssueUrl("https://example.com/{bts_project}/issue/{issue_id}");
		Launch launch = createLaunch(parameters);

		String launchUuid = launch.start().blockingGet();
		StartTestItemRQ itemRq = standardStartStepRequest();
		itemRq.setLaunchUuid(launchUuid);
		Maybe<String> itemId = launch.startTestItem(itemRq);
		FinishTestItemRQ finishRq = positiveFinishRequest();
		finishRq.setStatus(ItemStatus.FAILED.name());
		Issue issue = new Issue();
		issue.setIssueType("pb001");
		issue.setComment("issue_comment");
		Issue.ExternalSystemIssue externalIssue = new Issue.ExternalSystemIssue();
		externalIssue.setTicketId("RP-001");
		externalIssue.setBtsUrl("https://test.com");
		externalIssue.setUrl("https://test.com/test_project/issue/RP-002");
		externalIssue.setBtsProject("test_project");
		issue.setExternalSystemIssues(Collections.singleton(externalIssue));
		finishRq.setIssue(issue);
		launch.finishTestItem(itemId, finishRq).blockingGet();

		ArgumentCaptor<FinishTestItemRQ> captor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(rpClient).finishTestItem(eq(itemId.blockingGet()), captor.capture());

		FinishTestItemRQ resultFinishRq = captor.getValue();
		assertThat(resultFinishRq.getIssue().getExternalSystemIssues(), hasSize(1));
		Issue.ExternalSystemIssue resultExternalIssue = resultFinishRq.getIssue().getExternalSystemIssues().iterator().next();
		assertThat(resultExternalIssue.getTicketId(), equalTo("RP-001"));
		assertThat(resultExternalIssue.getUrl(), equalTo("https://test.com/test_project/issue/RP-002"));
		assertThat(resultExternalIssue.getBtsUrl(), equalTo("https://test.com"));
		assertThat(resultExternalIssue.getBtsProject(), equalTo("test_project"));
	}

	@Test
	public void verify_external_issue_url_is_used_for_project_and_ticket_ids() {
		simulateStartLaunchResponse(rpClient);
		simulateStartTestItemResponse(rpClient);
		simulateFinishTestItemResponse(rpClient);
		ListenerParameters parameters = standardParameters();
		parameters.setBtsUrl("https://example.com");
		parameters.setBtsProjectId("example_project");
		parameters.setBtsIssueUrl("https://example.com/{bts_project}/issue/{issue_id}");
		Launch launch = createLaunch(parameters);

		String launchUuid = launch.start().blockingGet();
		StartTestItemRQ itemRq = standardStartStepRequest();
		itemRq.setLaunchUuid(launchUuid);
		Maybe<String> itemId = launch.startTestItem(itemRq);
		FinishTestItemRQ finishRq = positiveFinishRequest();
		finishRq.setStatus(ItemStatus.FAILED.name());
		Issue issue = new Issue();
		issue.setIssueType("pb001");
		issue.setComment("issue_comment");
		Issue.ExternalSystemIssue externalIssue = new Issue.ExternalSystemIssue();
		externalIssue.setTicketId("RP-001");
		externalIssue.setUrl("https://test.com/{bts_project}/issue/{issue_id}");
		issue.setExternalSystemIssues(Collections.singleton(externalIssue));
		finishRq.setIssue(issue);
		launch.finishTestItem(itemId, finishRq).blockingGet();

		ArgumentCaptor<FinishTestItemRQ> captor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(rpClient).finishTestItem(eq(itemId.blockingGet()), captor.capture());

		FinishTestItemRQ resultFinishRq = captor.getValue();
		assertThat(resultFinishRq.getIssue().getExternalSystemIssues(), hasSize(1));
		Issue.ExternalSystemIssue resultExternalIssue = resultFinishRq.getIssue().getExternalSystemIssues().iterator().next();
		assertThat(resultExternalIssue.getUrl(), equalTo("https://test.com/example_project/issue/RP-001"));
	}

	@Test
	public void verify_issue_type_lookup_by_locator() {
		simulateStartLaunchResponse(rpClient);
		simulateStartTestItemResponse(rpClient);
		simulateFinishTestItemResponse(rpClient);
		when(rpClient.getProjectSettings()).thenReturn(Maybe.just(standardProjectSettings()));

		Launch launch = createLaunch(standardParameters());
		String launchUuid = launch.start().blockingGet();
		StartTestItemRQ itemRq = standardStartStepRequest();
		itemRq.setLaunchUuid(launchUuid);
		Maybe<String> itemId = launch.startTestItem(itemRq);
		FinishTestItemRQ finishRq = positiveFinishRequest();
		finishRq.setStatus(ItemStatus.FAILED.name());
		Issue issue = new Issue();
		issue.setIssueType("pb001");
		finishRq.setIssue(issue);
		launch.finishTestItem(itemId, finishRq).blockingGet();

		ArgumentCaptor<FinishTestItemRQ> captor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(rpClient).finishTestItem(eq(itemId.blockingGet()), captor.capture());

		FinishTestItemRQ resultFinishRq = captor.getValue();
		assertThat(resultFinishRq.getIssue(), notNullValue());
		assertThat(resultFinishRq.getIssue().getIssueType(), equalTo("pb001"));
	}

	@Test
	public void verify_issue_type_lookup_by_short_name() {
		simulateStartLaunchResponse(rpClient);
		simulateStartTestItemResponse(rpClient);
		simulateFinishTestItemResponse(rpClient);
		when(rpClient.getProjectSettings()).thenReturn(Maybe.just(standardProjectSettings()));

		Launch launch = createLaunch(standardParameters());
		String launchUuid = launch.start().blockingGet();
		StartTestItemRQ itemRq = standardStartStepRequest();
		itemRq.setLaunchUuid(launchUuid);
		Maybe<String> itemId = launch.startTestItem(itemRq);
		FinishTestItemRQ finishRq = positiveFinishRequest();
		finishRq.setStatus(ItemStatus.FAILED.name());
		Issue issue = new Issue();
		issue.setIssueType("ab");
		finishRq.setIssue(issue);
		launch.finishTestItem(itemId, finishRq).blockingGet();

		ArgumentCaptor<FinishTestItemRQ> captor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(rpClient).finishTestItem(eq(itemId.blockingGet()), captor.capture());

		FinishTestItemRQ resultFinishRq = captor.getValue();
		assertThat(resultFinishRq.getIssue(), notNullValue());
		assertThat(resultFinishRq.getIssue().getIssueType(), equalTo("ab001"));
	}

	@Test
	public void verify_issue_type_lookup_by_long_name() {
		simulateStartLaunchResponse(rpClient);
		simulateStartTestItemResponse(rpClient);
		simulateFinishTestItemResponse(rpClient);
		when(rpClient.getProjectSettings()).thenReturn(Maybe.just(standardProjectSettings()));

		Launch launch = createLaunch(standardParameters());
		String launchUuid = launch.start().blockingGet();
		StartTestItemRQ itemRq = standardStartStepRequest();
		itemRq.setLaunchUuid(launchUuid);
		Maybe<String> itemId = launch.startTestItem(itemRq);
		FinishTestItemRQ finishRq = positiveFinishRequest();
		finishRq.setStatus(ItemStatus.FAILED.name());
		Issue issue = new Issue();
		issue.setIssueType("system issue");
		finishRq.setIssue(issue);
		launch.finishTestItem(itemId, finishRq).blockingGet();

		ArgumentCaptor<FinishTestItemRQ> captor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(rpClient).finishTestItem(eq(itemId.blockingGet()), captor.capture());

		FinishTestItemRQ resultFinishRq = captor.getValue();
		assertThat(resultFinishRq.getIssue(), notNullValue());
		assertThat(resultFinishRq.getIssue().getIssueType(), equalTo("si001"));
	}

	@Test
	public void verify_issue_type_lookup_by_type_reference() {
		simulateStartLaunchResponse(rpClient);
		simulateStartTestItemResponse(rpClient);
		simulateFinishTestItemResponse(rpClient);
		when(rpClient.getProjectSettings()).thenReturn(Maybe.just(standardProjectSettings()));

		Launch launch = createLaunch(standardParameters());
		String launchUuid = launch.start().blockingGet();
		StartTestItemRQ itemRq = standardStartStepRequest();
		itemRq.setLaunchUuid(launchUuid);
		Maybe<String> itemId = launch.startTestItem(itemRq);
		FinishTestItemRQ finishRq = positiveFinishRequest();
		finishRq.setStatus(ItemStatus.FAILED.name());
		Issue issue = new Issue();
		issue.setIssueType("NO_DEFECT");
		finishRq.setIssue(issue);
		launch.finishTestItem(itemId, finishRq).blockingGet();

		ArgumentCaptor<FinishTestItemRQ> captor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(rpClient).finishTestItem(eq(itemId.blockingGet()), captor.capture());

		FinishTestItemRQ resultFinishRq = captor.getValue();
		assertThat(resultFinishRq.getIssue(), notNullValue());
		assertThat(resultFinishRq.getIssue().getIssueType(), equalTo("nd001"));
	}
}
