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
import com.epam.reportportal.restendpoint.http.MultiPartRequest;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.reportportal.utils.SubscriptionUtils;
import com.epam.ta.reportportal.ws.model.*;
import com.epam.ta.reportportal.ws.model.item.ItemCreatedRS;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRS;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Maybe;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class TestUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestUtils.class);

	public static final ListenerParameters STANDARD_PARAMETERS = standardParameters();

	private TestUtils() {
	}

	public static ListenerParameters standardParameters() {
		ListenerParameters result = new ListenerParameters();
		result.setClientJoin(false);
		result.setBatchLogsSize(1);
		result.setLaunchName("My-test-launch" + generateUniqueId());
		result.setProjectName("unit-test");
		result.setEnable(true);
		return result;
	}

	public static void shutdownExecutorService(ExecutorService executor) {
		executor.shutdown();
		try {
			if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
				executor.shutdownNow();
			}
		} catch (InterruptedException ignore) {
		}
	}

	public static Maybe<StartLaunchRS> startLaunchResponse(String id) {
		final StartLaunchRS rs = new StartLaunchRS();
		rs.setId(id);
		return SubscriptionUtils.createConstantMaybe(rs);
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
		when(client.finishLaunch(
				anyString(),
				any(FinishExecutionRQ.class)
		)).thenReturn(CommonUtils.createMaybe(new OperationCompletionRS()));
	}

	public static FinishExecutionRQ standardLaunchFinishRequest() {
		FinishExecutionRQ rq = new FinishExecutionRQ();
		rq.setEndTime(Calendar.getInstance().getTime());
		return rq;
	}

	public static Maybe<ItemCreatedRS> startTestItemResponse(String id) {
		final ItemCreatedRS rs = new ItemCreatedRS();
		rs.setId(id);
		return SubscriptionUtils.createConstantMaybe(rs);
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
		return SubscriptionUtils.createConstantMaybe(rs);
	}

	@SuppressWarnings("unchecked")
	public static void simulateBatchLogResponse(final ReportPortalClient client) {
		when(client.log(any(MultiPartRequest.class))).then((Answer<Maybe<BatchSaveOperatingRS>>) invocation -> {
			MultiPartRequest rq = invocation.getArgument(0);
			List<String> saveRqs = rq.getSerializedRQs()
					.stream()
					.map(r -> (List<SaveLogRQ>) r.getRequest())
					.flatMap(Collection::stream)
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
}
