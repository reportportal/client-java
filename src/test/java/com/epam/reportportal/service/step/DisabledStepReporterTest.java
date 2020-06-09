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

package com.epam.reportportal.service.step;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.test.TestUtils;
import io.reactivex.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class DisabledStepReporterTest {

	@Mock
	private ReportPortalClient client;
	private ReportPortal rp;

	@BeforeEach
	public void setup() {
		ListenerParameters parameters = TestUtils.standardParameters();
		parameters.setEnable(false);
		rp = ReportPortal.create(client, parameters);
	}

	@Test
	public void test_step_reporter_do_not_fail_if_rp_disabled() {
		Launch launch = rp.newLaunch(TestUtils.standardLaunchRequest(TestUtils.standardParameters()));
		launch.start();
		Maybe<String> suite = launch.startTestItem(TestUtils.standardStartSuiteRequest());
		Maybe<String> test = launch.startTestItem(suite, TestUtils.standardStartTestRequest());
		launch.startTestItem(test, TestUtils.standardStartStepRequest());
		StepReporter sr = launch.getStepReporter();

		sr.sendStep("Test step");
		sr.finishPreviousStep();
	}
}
