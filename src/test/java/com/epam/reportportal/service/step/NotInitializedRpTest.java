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

import com.epam.reportportal.util.test.ProcessUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.epam.reportportal.util.test.CommonUtils.shutdownExecutorService;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class NotInitializedRpTest {

	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	@AfterEach
	public void cleanup() {
		shutdownExecutorService(executor);
	}

	@Test
	public void test_step_reporter_do_not_fail_if_rp_is_not_initialized() throws InterruptedException, IOException {
		Process process = ProcessUtils.buildProcess(true, NotInitializedRpTestExecution.class);
		assertThat("Exit code should be '0'", process.waitFor(), equalTo(0));
	}
}
