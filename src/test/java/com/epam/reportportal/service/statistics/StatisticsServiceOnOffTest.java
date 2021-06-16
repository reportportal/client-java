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

package com.epam.reportportal.service.statistics;

import com.epam.reportportal.util.test.ProcessUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class StatisticsServiceOnOffTest {
	@Test
	public void test_statistics_property_off() throws IOException, InterruptedException {
		Process process = ProcessUtils.buildProcess(
				true,
				StatisticsRunnable.class,
				Collections.singletonMap(StatisticsService.DISABLE_PROPERTY, "1"),
				DummyStatistics.class.getCanonicalName()
		);
		assertThat("Exit code should be '0'", process.waitFor(), equalTo(0));
	}

	@Test
	public void test_statistics_property_on() throws IOException, InterruptedException {
		Process process = ProcessUtils.buildProcess(true, StatisticsRunnable.class, StatisticsApiClient.class.getCanonicalName());
		assertThat("Exit code should be '0'", process.waitFor(), equalTo(0));
	}
}
