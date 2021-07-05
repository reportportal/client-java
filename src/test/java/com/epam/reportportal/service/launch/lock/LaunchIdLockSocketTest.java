/*
 *  Copyright 2021 EPAM Systems
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

package com.epam.reportportal.service.launch.lock;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.util.test.ProcessUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import static com.epam.reportportal.service.launch.lock.LockTestUtil.*;
import static org.apache.commons.lang3.StringUtils.join;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * @author <a href="mailto:vadzim_hushchanskou@epam.com">Vadzim Hushchanskou</a>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LaunchIdLockSocketTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(LaunchIdLockSocketTest.class);

	private LaunchIdLockSocket launchIdLockSocket = new LaunchIdLockSocket(getParameters());

	private ListenerParameters getParameters() {
		ListenerParameters params = new ListenerParameters();
		params.setEnable(Boolean.TRUE);
		params.setLockWaitTimeout(TimeUnit.SECONDS.toMillis(5));
		return params;
	}

	@AfterEach
	public void cleanUp() {
		launchIdLockSocket.reset();
	}

	@Test
	public void test_launch_uuid_will_be_the_same_for_one_thread_obtainLaunchUuid() {
		String firstUuid = UUID.randomUUID().toString();
		String secondUuid = UUID.randomUUID().toString();
		assertThat(secondUuid, is(not(equalTo(firstUuid))));

		String firstLaunchUuid = launchIdLockSocket.obtainLaunchUuid(firstUuid);
		String secondLaunchUuid = launchIdLockSocket.obtainLaunchUuid(secondUuid);

		assertThat(secondLaunchUuid, equalTo(firstLaunchUuid));
	}

	@Test
	@Timeout(10)
	public void test_launch_uuid_get_for_two_processes_returns_equal_values_obtainLaunchUuid() throws IOException, InterruptedException {
		Pair<String, String> uuids = ImmutablePair.of(UUID.randomUUID().toString(), UUID.randomUUID().toString());

		LOGGER.info("Running two separate processes");
		// @formatter:off
		Pair<Process, Process> processes = ImmutablePair.of(
				ProcessUtils.buildProcess(LockSocketRunner.class, uuids.getKey()),
				ProcessUtils.buildProcess(LockSocketRunner.class, uuids.getValue())
		);
		// @formatter:on

		Triple<OutputStreamWriter, BufferedReader, BufferedReader> primaryProcessIo = getProcessIos(processes.getKey());
		Triple<OutputStreamWriter, BufferedReader, BufferedReader> secondaryProcessIo = getProcessIos(processes.getValue());

		try {

			waitForLine(primaryProcessIo.getMiddle(), primaryProcessIo.getRight(), WELCOME_MESSAGE_PREDICATE);
			waitForLine(secondaryProcessIo.getMiddle(), secondaryProcessIo.getRight(), WELCOME_MESSAGE_PREDICATE);

			String lineSeparator = System.getProperty("line.separator");
			primaryProcessIo.getLeft().write(lineSeparator);
			primaryProcessIo.getLeft().flush();
			secondaryProcessIo.getLeft().write(lineSeparator);
			secondaryProcessIo.getLeft().flush();

			String result1 = waitForLine(primaryProcessIo.getMiddle(), primaryProcessIo.getRight(), ANY_STRING_PREDICATE);
			String result2 = waitForLine(secondaryProcessIo.getMiddle(), secondaryProcessIo.getRight(), ANY_STRING_PREDICATE);

			assertThat("Assert two UUIDs from different processes are equal", result1, equalTo(result2));

			primaryProcessIo.getLeft().write(lineSeparator);
			primaryProcessIo.getLeft().flush();
			secondaryProcessIo.getLeft().write(lineSeparator);
			secondaryProcessIo.getLeft().flush();

			processes.getKey().waitFor();
			processes.getValue().waitFor();
		} finally {
			LOGGER.info("Done. Closing them out.");
			closeIos(primaryProcessIo);
			closeIos(secondaryProcessIo);
			processes.getKey().destroyForcibly();
			processes.getValue().destroyForcibly();
		}
	}
}
