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

import com.epam.reportportal.exception.ReportPortalException;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.ta.reportportal.ws.model.ErrorRS;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.epam.ta.reportportal.ws.model.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import io.reactivex.Maybe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.epam.reportportal.test.TestUtils.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class LaunchTest {

	private static final ErrorRS ERROR_RS;

	static {
		ERROR_RS = new ErrorRS();
		ERROR_RS.setErrorType(ErrorType.INCORRECT_REQUEST);
		ERROR_RS.setMessage("Incorrect Request. [Value is not allowed for field 'status'.]");
	}

	private static final ReportPortalException CLIENT_EXCEPTION = new ReportPortalException(400, "Bad Request", ERROR_RS);

	@Mock
	private ReportPortalClient rpClient;

	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	@BeforeEach
	public void prepare() {
		simulateStartLaunchResponse(rpClient);
		simulateStartTestItemResponse(rpClient);
	}

	@AfterEach
	public void tearDown() throws InterruptedException {
		executor.shutdown();
		if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
			executor.shutdownNow();
		}
	}

	@Test
	public void launch_should_finish_all_items_even_if_one_of_finishes_failed() {
		simulateStartChildTestItemResponse(rpClient);

		Launch launch = new LaunchImpl(rpClient, STANDARD_PARAMETERS, standardLaunchRequest(STANDARD_PARAMETERS), executor);

		Maybe<String> launchUuid = launch.start();
		Maybe<String> suiteRs = launch.startTestItem(standardStartSuiteRequest());
		Maybe<String> testRs = launch.startTestItem(suiteRs, standardStartTestRequest());
		Maybe<String> stepRs = launch.startTestItem(testRs, standardStartStepRequest());

		when(rpClient.finishTestItem(eq(stepRs.blockingGet()), any())).thenThrow(CLIENT_EXCEPTION);
		when(rpClient.finishTestItem(eq(testRs.blockingGet()), any())).thenReturn(getConstantMaybe(new OperationCompletionRS()));
		when(rpClient.finishTestItem(eq(suiteRs.blockingGet()), any())).thenReturn(getConstantMaybe(new OperationCompletionRS()));
		when(rpClient.finishLaunch(eq(launchUuid.blockingGet()), any())).thenReturn(getConstantMaybe(new OperationCompletionRS()));

		launch.finishTestItem(stepRs, positiveFinishRequest());
		launch.finishTestItem(testRs, positiveFinishRequest());
		launch.finishTestItem(suiteRs, positiveFinishRequest());
		launch.finish(new FinishExecutionRQ());

		verify(rpClient, times(1)).finishTestItem(eq(stepRs.blockingGet()), any());
		verify(rpClient, times(1)).finishTestItem(eq(testRs.blockingGet()), any());
		verify(rpClient, times(1)).finishTestItem(eq(suiteRs.blockingGet()), any());
		verify(rpClient, times(1)).finishLaunch(eq(launchUuid.blockingGet()), any());
	}

	@Test
	public void launch_should_finish_all_items_even_if_one_of_starts_failed() {
		Launch launch = new LaunchImpl(rpClient, STANDARD_PARAMETERS, standardLaunchRequest(STANDARD_PARAMETERS), executor);

		Maybe<String> launchUuid = launch.start();
		Maybe<String> suiteRs = launch.startTestItem(standardStartSuiteRequest());

		when(rpClient.startTestItem(eq(suiteRs.blockingGet()), any())).thenReturn(startTestItemResponse(UUID.randomUUID().toString()));
		Maybe<String> testRs = launch.startTestItem(suiteRs, standardStartTestRequest());

		when(rpClient.startTestItem(eq(testRs.blockingGet()), any())).thenThrow(CLIENT_EXCEPTION);
		Maybe<String> stepRs = launch.startTestItem(testRs, standardStartStepRequest());

		when(rpClient.finishTestItem(eq(testRs.blockingGet()), any())).thenReturn(getConstantMaybe(new OperationCompletionRS()));
		when(rpClient.finishTestItem(eq(suiteRs.blockingGet()), any())).thenReturn(getConstantMaybe(new OperationCompletionRS()));
		when(rpClient.finishLaunch(eq(launchUuid.blockingGet()), any())).thenReturn(getConstantMaybe(new OperationCompletionRS()));

		launch.finishTestItem(stepRs, positiveFinishRequest());
		launch.finishTestItem(testRs, positiveFinishRequest());
		launch.finishTestItem(suiteRs, positiveFinishRequest());
		launch.finish(new FinishExecutionRQ());

		verify(rpClient, times(1)).finishTestItem(eq(testRs.blockingGet()), any());
		verify(rpClient, times(1)).finishTestItem(eq(suiteRs.blockingGet()), any());
		verify(rpClient, times(1)).finishLaunch(eq(launchUuid.blockingGet()), any());
	}
}
