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

import com.epam.reportportal.service.statistics.StatisticsService;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import io.reactivex.Maybe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.epam.reportportal.test.TestUtils.*;

public class LaunchNullCheckTest {

	@Mock
	private ReportPortalClient rpClient;

	@Mock
	private StatisticsService statisticsService;

	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	@BeforeEach
	public void prepare() {
		simulateStartLaunchResponse(rpClient);
	}

	@AfterEach
	public void tearDown() {
		shutdownExecutorService(executor);
	}

	// See https://github.com/reportportal/client-java/issues/99
	@Test
	public void launch_should_not_throw_any_exceptions_if_an_agent_bypasses_null_start_rq_on_start_item() {
		Launch launch = new LaunchImpl(rpClient, STANDARD_PARAMETERS, standardLaunchRequest(STANDARD_PARAMETERS), executor) {
			@Override
			StatisticsService getStatisticsService() {
				return statisticsService;
			}
		};

		launch.start();
		Maybe<String> result = launch.startTestItem(null);

		Assertions.assertThrows(NullPointerException.class, result::blockingGet);
	}

	@Test
	public void launch_should_not_throw_any_exceptions_if_an_agent_bypasses_null_parent_item_id_on_start_item() {
		Launch launch = new LaunchImpl(rpClient, STANDARD_PARAMETERS, standardLaunchRequest(STANDARD_PARAMETERS), executor) {
			@Override
			StatisticsService getStatisticsService() {
				return statisticsService;
			}
		};
		simulateStartTestItemResponse(rpClient);

		launch.start();
		launch.startTestItem(standardStartSuiteRequest());
		Maybe<String> result = launch.startTestItem(null, null);

		Assertions.assertThrows(NullPointerException.class, result::blockingGet);
	}

	@Test
	public void launch_should_not_throw_any_exceptions_if_an_agent_bypasses_null_item_id_on_finish_item() {
		Launch launch = new LaunchImpl(rpClient, STANDARD_PARAMETERS, standardLaunchRequest(STANDARD_PARAMETERS), executor) {
			@Override
			StatisticsService getStatisticsService() {
				return statisticsService;
			}
		};
		simulateStartTestItemResponse(rpClient);

		launch.start();
		launch.startTestItem(standardStartSuiteRequest());
		Maybe<OperationCompletionRS> result = launch.finishTestItem(null, positiveFinishRequest());

		Assertions.assertThrows(NullPointerException.class, result::blockingGet);
	}

	@Test
	public void launch_should_not_throw_any_exceptions_if_an_agent_bypasses_null_finish_rq_on_finish_item() {
		Launch launch = new LaunchImpl(rpClient, STANDARD_PARAMETERS, standardLaunchRequest(STANDARD_PARAMETERS), executor) {
			@Override
			StatisticsService getStatisticsService() {
				return statisticsService;
			}
		};
		simulateStartTestItemResponse(rpClient);

		launch.start();
		launch.startTestItem(standardStartSuiteRequest());
		Maybe<OperationCompletionRS> result = launch.finishTestItem(launch.startTestItem(standardStartSuiteRequest()), null);

		Assertions.assertThrows(NullPointerException.class, result::blockingGet);
	}
}
