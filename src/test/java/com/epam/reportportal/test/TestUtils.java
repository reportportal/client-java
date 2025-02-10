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

package com.epam.reportportal.test;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.utils.http.HttpRequestUtils;
import com.epam.ta.reportportal.ws.model.*;
import com.epam.ta.reportportal.ws.model.item.ItemCreatedRS;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRS;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import com.epam.ta.reportportal.ws.model.project.config.IssueSubTypeResource;
import com.epam.ta.reportportal.ws.model.project.config.ProjectSettingsResource;
import com.fasterxml.jackson.core.type.TypeReference;
import io.reactivex.Maybe;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okio.Buffer;
import org.apache.commons.lang3.tuple.Pair;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class TestUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestUtils.class);

	public static final ListenerParameters STANDARD_PARAMETERS = standardParameters();

	private TestUtils() {
	}

	public static ListenerParameters standardParameters() {
		ListenerParameters result = new ListenerParameters();
		result.setBaseUrl("http://localhost:8080");
		result.setClientJoin(false);
		result.setBatchLogsSize(1);
		result.setLaunchName("My-test-launch" + generateUniqueId());
		result.setProjectName("unit-test");
		result.setEnable(true);
		return result;
	}

	public static ProjectSettingsResource standardProjectSettings() {
		ProjectSettingsResource settings = new ProjectSettingsResource();
		settings.setProjectId(2L);
		HashMap<String, List<IssueSubTypeResource>> subTypes = new HashMap<>();
		settings.setSubTypes(subTypes);
		IssueSubTypeResource subType = new IssueSubTypeResource();
		subType.setId(1L);
		subType.setLocator("ti001");
		subType.setTypeRef("TO_INVESTIGATE");
		subType.setLongName("To Investigate");
		subType.setShortName("TI");
		subType.setColor("#00829b");
		subTypes.computeIfAbsent(subType.getTypeRef(), k -> new ArrayList<>()).add(subType);

		subType = new IssueSubTypeResource();
		subType.setId(2L);
		subType.setLocator("ab001");
		subType.setTypeRef("AUTOMATION_BUG");
		subType.setLongName("Automation Bug");
		subType.setShortName("AB");
		subType.setColor("#ffc208");
		subTypes.computeIfAbsent(subType.getTypeRef(), k -> new ArrayList<>()).add(subType);

		subType = new IssueSubTypeResource();
		subType.setId(3L);
		subType.setLocator("pb001");
		subType.setTypeRef("PRODUCT_BUG");
		subType.setLongName("Product Bug");
		subType.setShortName("PB");
		subType.setColor("#d32f2f");
		subTypes.computeIfAbsent(subType.getTypeRef(), k -> new ArrayList<>()).add(subType);

		subType = new IssueSubTypeResource();
		subType.setId(4L);
		subType.setLocator("nd001");
		subType.setTypeRef("NO_DEFECT");
		subType.setLongName("No Defect");
		subType.setShortName("ND");
		subType.setColor("#76839b");
		subTypes.computeIfAbsent(subType.getTypeRef(), k -> new ArrayList<>()).add(subType);

		subType = new IssueSubTypeResource();
		subType.setId(5L);
		subType.setLocator("si001");
		subType.setTypeRef("SYSTEM_ISSUE");
		subType.setLongName("System Issue");
		subType.setShortName("SI");
		subType.setColor("#3e7be6");
		subTypes.computeIfAbsent(subType.getTypeRef(), k -> new ArrayList<>()).add(subType);

		return settings;
	}

	public static Maybe<StartLaunchRS> startLaunchResponse(String id) {
		final StartLaunchRS rs = new StartLaunchRS();
		rs.setId(id);
		return Maybe.just(rs);
	}

	public static void simulateStartLaunchResponse(final ReportPortalClient client) {
		when(client.startLaunch(any(StartLaunchRQ.class))).then((Answer<Maybe<StartLaunchRS>>) invocation -> {
			StartLaunchRQ rq = invocation.getArgument(0);
			return startLaunchResponse(ofNullable(rq.getUuid()).orElseGet(() -> UUID.randomUUID().toString()));
		});
	}

	public static StartLaunchRQ standardLaunchRequest(final ListenerParameters params) {
		StartLaunchRQ rq = new StartLaunchRQ();
		rq.setName(params.getLaunchName());
		rq.setStartTime(Calendar.getInstance().getTime());
		rq.setAttributes(params.getAttributes());
		rq.setMode(params.getLaunchRunningMode());
		rq.setRerun(params.isRerun());
		rq.setStartTime(Calendar.getInstance().getTime());
		return rq;
	}

	public static void simulateFinishLaunchResponse(final ReportPortalClient client) {
		when(client.finishLaunch(anyString(), any(FinishExecutionRQ.class))).thenReturn(Maybe.just(new OperationCompletionRS()));
	}

	public static FinishExecutionRQ standardLaunchFinishRequest() {
		FinishExecutionRQ rq = new FinishExecutionRQ();
		rq.setEndTime(Calendar.getInstance().getTime());
		return rq;
	}

	public static Maybe<ItemCreatedRS> startTestItemResponse(String id) {
		final ItemCreatedRS rs = new ItemCreatedRS();
		rs.setId(id);
		return Maybe.just(rs);
	}

	public static Maybe<OperationCompletionRS> finishTestItemResponse() {
		final OperationCompletionRS rs = new OperationCompletionRS();
		return Maybe.just(rs);
	}

	public static void simulateStartTestItemResponse(final ReportPortalClient client) {
		when(client.startTestItem(any(StartTestItemRQ.class))).then((Answer<Maybe<ItemCreatedRS>>) invocation -> {
			StartTestItemRQ rq = invocation.getArgument(0);
			return startTestItemResponse(ofNullable(rq.getUuid()).orElseGet(() -> UUID.randomUUID().toString()));
		});
	}

	public static void simulateStartChildTestItemResponse(final ReportPortalClient client) {
		when(client.startTestItem(anyString(), any(StartTestItemRQ.class))).then((Answer<Maybe<ItemCreatedRS>>) invocation -> {
			StartTestItemRQ rq = invocation.getArgument(1);
			return startTestItemResponse(ofNullable(rq.getUuid()).orElseGet(() -> UUID.randomUUID().toString()));
		});
	}

	public static void simulateFinishTestItemResponse(final ReportPortalClient client) {
		when(client.finishTestItem(anyString(),
				any(FinishTestItemRQ.class)
		)).then((Answer<Maybe<OperationCompletionRS>>) invocation -> finishTestItemResponse());
	}

	public static StartTestItemRQ standardStartSuiteRequest() {
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setStartTime(Calendar.getInstance().getTime());
		String id = generateUniqueId();
		rq.setName("Suite_" + id);
		rq.setDescription("Suite description");
		rq.setUniqueId(id);
		rq.setType("SUITE");
		return rq;
	}

	public static StartTestItemRQ standardStartTestRequest() {
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setStartTime(Calendar.getInstance().getTime());
		String id = generateUniqueId();
		rq.setName("Test_" + id);
		rq.setDescription("Test description");
		rq.setUniqueId(id);
		rq.setType("TEST");
		return rq;
	}

	public static StartTestItemRQ standardStartStepRequest() {
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setStartTime(Calendar.getInstance().getTime());
		String id = generateUniqueId();
		rq.setName("Step_" + id);
		rq.setDescription("Test step description");
		rq.setUniqueId(id);
		rq.setType("STEP");
		return rq;
	}

	/**
	 * Generates a unique ID shorter than UUID based on current time in milliseconds and thread ID.
	 *
	 * @return a unique ID string
	 */
	public static String generateUniqueId() {
		return System.currentTimeMillis() + "-" + Thread.currentThread().getId() + "-" + ThreadLocalRandom.current().nextInt(9999);
	}

	public static Maybe<BatchSaveOperatingRS> batchLogResponse(List<String> ids) {
		final BatchSaveOperatingRS rs = new BatchSaveOperatingRS();
		ids.forEach(i -> rs.addResponse(new BatchElementCreatedRS(i)));
		return Maybe.just(rs);
	}

	public static List<SaveLogRQ> extractJsonParts(List<MultipartBody.Part> parts) {
		return parts.stream()
				.filter(p -> ofNullable(p.headers()).map(headers -> headers.get("Content-Disposition"))
						.map(h -> h.contains(Constants.LOG_REQUEST_JSON_PART))
						.orElse(false))
				.map(MultipartBody.Part::body)
				.map(b -> {
					Buffer buf = new Buffer();
					try {
						b.writeTo(buf);
					} catch (IOException ignore) {
					}
					return buf.readByteArray();
				})
				.map(b -> {
					try {
						return HttpRequestUtils.MAPPER.readValue(b, new TypeReference<List<SaveLogRQ>>() {
						});
					} catch (IOException e) {
						return Collections.<SaveLogRQ>emptyList();
					}
				})
				.flatMap(Collection::stream)
				.collect(Collectors.toList());
	}

	public static List<Pair<String, byte[]>> extractBinaryParts(List<MultipartBody.Part> parts) {
		return parts.stream()
				.filter(p -> ofNullable(p.headers()).map(headers -> headers.get("Content-Disposition"))
						.map(h -> h.contains(Constants.LOG_REQUEST_BINARY_PART))
						.orElse(false))
				.map(p-> Pair.of(ofNullable(p.body().contentType()).map(MediaType::toString).orElse(null), p.body()))
				.map(b -> {
					Buffer buf = new Buffer();
					try {
						b.getValue().writeTo(buf);
					} catch (IOException ignore) {
					}
					return Pair.of(b.getKey(), buf.readByteArray());
				})
				.collect(Collectors.toList());
	}

	public static void simulateBatchLogResponse(final ReportPortalClient client) {
		//noinspection unchecked
		when(client.log(any(List.class))).then((Answer<Maybe<BatchSaveOperatingRS>>) invocation -> {
			List<MultipartBody.Part> rq = invocation.getArgument(0);
			List<String> saveRqs = extractJsonParts(rq).stream()
					.peek(r -> LOGGER.info(r.getItemUuid() + " - " + r.getMessage()))
					.map(s -> ofNullable(s.getUuid()).orElseGet(() -> UUID.randomUUID().toString()))
					.collect(Collectors.toList());
			return batchLogResponse(saveRqs);
		});
	}

	public static FinishTestItemRQ positiveFinishRequest() {
		FinishTestItemRQ rq = new FinishTestItemRQ();
		rq.setEndTime(Calendar.getInstance().getTime());
		rq.setStatus("PASSED");
		return rq;
	}

	public static void mockLaunch(ReportPortalClient client, String launchUuid, String testClassUuid, String testMethodUuid) {
		mockLaunch(client, launchUuid, testClassUuid, Collections.singleton(testMethodUuid));
	}

	@SuppressWarnings("unchecked")
	public static void mockLaunch(ReportPortalClient client, String launchUuid, String testClassUuid,
			Collection<String> testMethodUuidList) {
		when(client.startLaunch(any())).thenReturn(Maybe.just(new StartLaunchRS(launchUuid, 1L)));

		Maybe<ItemCreatedRS> testClassMaybe = Maybe.just(new ItemCreatedRS(testClassUuid, testClassUuid));
		when(client.startTestItem(any())).thenReturn(testClassMaybe);

		List<Maybe<ItemCreatedRS>> responses = testMethodUuidList.stream()
				.map(uuid -> Maybe.just(new ItemCreatedRS(uuid, uuid)))
				.collect(Collectors.toList());
		Maybe<ItemCreatedRS> first = responses.get(0);
		Maybe<ItemCreatedRS>[] other = responses.subList(1, responses.size()).toArray(new Maybe[0]);
		when(client.startTestItem(eq(testClassUuid), any())).thenReturn(first, other);
		new HashSet<>(testMethodUuidList).forEach(testMethodUuid -> when(client.finishTestItem(
				eq(testMethodUuid),
				any()
		)).thenReturn(Maybe.just(new OperationCompletionRS())));

		Maybe<OperationCompletionRS> testClassFinishMaybe = Maybe.just(new OperationCompletionRS());
		when(client.finishTestItem(eq(testClassUuid), any())).thenReturn(testClassFinishMaybe);

		when(client.finishLaunch(eq(launchUuid), any())).thenReturn(Maybe.just(new OperationCompletionRS()));
	}

	public static void mockNestedSteps(ReportPortalClient client, Pair<String, String> parentNestedPair) {
		mockNestedSteps(client, Collections.singletonList(parentNestedPair));
	}

	@SuppressWarnings("unchecked")
	public static void mockNestedSteps(final ReportPortalClient client, final List<Pair<String, String>> parentNestedPairs) {
		Map<String, List<String>> responseOrders = parentNestedPairs.stream()
				.collect(Collectors.groupingBy(Pair::getKey, Collectors.mapping(Pair::getValue, Collectors.toList())));
		responseOrders.forEach((k, v) -> {
			List<Maybe<ItemCreatedRS>> responses = v.stream()
					.map(uuid -> Maybe.just(new ItemCreatedRS(uuid, uuid)))
					.collect(Collectors.toList());

			Maybe<ItemCreatedRS> first = responses.get(0);
			Maybe<ItemCreatedRS>[] other = responses.subList(1, responses.size()).toArray(new Maybe[0]);
			when(client.startTestItem(eq(k), any(StartTestItemRQ.class))).thenReturn(first, other);
		});
		parentNestedPairs.forEach(p -> when(client.finishTestItem(same(p.getValue()),
				any(FinishTestItemRQ.class)
		)).thenAnswer((Answer<Maybe<OperationCompletionRS>>) invocation -> Maybe.just(new OperationCompletionRS())));
	}

	@SuppressWarnings("unchecked")
	public static void mockBatchLogging(ReportPortalClient client) {
		when(client.log(any(List.class))).thenReturn(Maybe.just(new BatchSaveOperatingRS()));
	}
}
