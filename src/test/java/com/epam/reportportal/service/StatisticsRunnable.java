/*
 *  Copyright 2024 EPAM Systems
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

import com.epam.reportportal.service.statistics.StatisticsClient;
import com.epam.reportportal.service.statistics.StatisticsService;
import com.epam.reportportal.service.statistics.item.StatisticsItem;
import com.epam.reportportal.test.TestUtils;
import com.epam.reportportal.util.test.CommonUtils;
import io.reactivex.Maybe;

import java.util.concurrent.ExecutorService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class StatisticsRunnable {
	public static final long DELAY = 1000;
	public static final StatisticsClient STATISTICS_CLIENT = mock(StatisticsClient.class);
	public static final StatisticsService STATISTICS_SERVICE = new StatisticsService(TestUtils.standardParameters(), STATISTICS_CLIENT);
	public static final Maybe<String> LAUNCH_ID = Maybe.just("launch_id");
	public static final ReportPortalClient CLIENT = mock(ReportPortalClient.class);

	public static class MyLaunch extends LaunchImpl {
		public MyLaunch(ExecutorService executorService) {
			super(CLIENT, TestUtils.standardParameters(), LAUNCH_ID, executorService);
		}

		@Override
		StatisticsService getStatisticsService() {
			return STATISTICS_SERVICE;
		}
	}

	public static void main(String... args) {
		try (CommonUtils.ExecutorService executorService = CommonUtils.testExecutor()) {
			MyLaunch launch = new MyLaunch(executorService);
			//noinspection ReactiveStreamsUnusedPublisher
			launch.start();
			verify(STATISTICS_CLIENT, after(DELAY).times(Integer.parseInt(args[0]))).send(any(StatisticsItem.class));
		}
	}
}
