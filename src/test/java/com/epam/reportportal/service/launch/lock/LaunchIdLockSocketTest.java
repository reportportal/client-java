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
import com.epam.reportportal.service.LaunchIdLock;
import com.epam.reportportal.util.test.ProcessUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.epam.reportportal.service.launch.lock.LockTestUtil.*;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * @author <a href="mailto:vadzim_hushchanskou@epam.com">Vadzim Hushchanskou</a>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LaunchIdLockSocketTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(LaunchIdLockSocketTest.class);

	private final LaunchIdLockSocket launchIdLockSocket = new LaunchIdLockSocket(getParameters());

	private ListenerParameters getParameters() {
		ListenerParameters params = new ListenerParameters();
		params.setEnable(Boolean.TRUE);
		params.setLockWaitTimeout(LOCK_TIMEOUT);
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
	public void test_launch_uuid_will_be_the_same_for_ten_threads_obtainLaunchUuid() throws InterruptedException {
		int threadNum = 10;
		ExecutorService executor = testExecutor(threadNum);
		Map<String, Callable<String>> tasks = getLaunchUuidReadCallables(threadNum, singletonSupplier(launchIdLockSocket));

		Collection<String> results = executor.invokeAll(tasks.values()).stream().map(new GetFutureResults<>()).collect(toList());
		assertThat(results, hasSize(threadNum));
		assertThat(results, Matchers.everyItem(equalTo(results.iterator().next())));
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
			String result1 = waitForLine(primaryProcessIo.getMiddle(), primaryProcessIo.getRight(), ANY_STRING_PREDICATE);

			secondaryProcessIo.getLeft().write(lineSeparator);
			secondaryProcessIo.getLeft().flush();
			String result2 = waitForLine(secondaryProcessIo.getMiddle(), secondaryProcessIo.getRight(), ANY_STRING_PREDICATE);

			assertThat("Assert two UUIDs from different processes are equal", result1, equalTo(result2));

			secondaryProcessIo.getLeft().write(lineSeparator);
			secondaryProcessIo.getLeft().flush();
			processes.getValue().waitFor();

			primaryProcessIo.getLeft().write(lineSeparator);
			primaryProcessIo.getLeft().flush();
			processes.getKey().waitFor();
		} finally {
			LOGGER.info("Done. Closing them out.");
			closeIos(secondaryProcessIo);
			closeIos(primaryProcessIo);
			processes.getValue().destroyForcibly();
			processes.getKey().destroyForcibly();
		}
	}

	@Test
	@Timeout(30)
	public void test_launch_uuid_get_for_ten_processes_returns_equal_values_obtainLaunchUuid() {
		List<String> uuids = Stream.generate(() -> UUID.randomUUID().toString()).limit(10).collect(toList());
		LOGGER.info("Running 10 separate processes");
		List<Process> processes = uuids.stream().map(u -> {
			try {
				return ProcessUtils.buildProcess(LockSocketRunner.class, u);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}).collect(toList());

		List<Triple<OutputStreamWriter, BufferedReader, BufferedReader>> processIos = processes.stream()
				.map(LockTestUtil::getProcessIos)
				.collect(toList());
		String lineSeparator = System.getProperty("line.separator");
		List<String> results;
		try {

			processIos.forEach(p -> {
				try {
					waitForLine(p.getMiddle(), p.getRight(), WELCOME_MESSAGE_PREDICATE);
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
			});

			results = processIos.stream().map(p -> {
				try {
					p.getLeft().write(lineSeparator);
					p.getLeft().flush();
					return waitForLine(p.getMiddle(), p.getRight(), ANY_STRING_PREDICATE);
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
			}).collect(toList());

			Collections.reverse(processIos);
			Collections.reverse(processes);

			IntStream.range(0, processIos.size()).forEach(i -> {
				Triple<OutputStreamWriter, BufferedReader, BufferedReader> io = processIos.get(i);
				Process p = processes.get(i);
				try {
					io.getLeft().write(lineSeparator);
					io.getLeft().flush();
					p.waitFor();
				} catch (IOException | InterruptedException e) {
					throw new IllegalStateException(e);
				}
			});
		} finally {
			LOGGER.info("Done. Closing them out.");
			processIos.forEach(LockTestUtil::closeIos);
			processes.forEach(Process::destroyForcibly);
		}

		assertThat(results, hasSize(10));
		assertThat(results, everyItem(equalTo(results.iterator().next())));
	}

	private Pair<Set<String>, Collection<String>> executeParallelLaunchUuidSync(int threadNum, Iterable<LaunchIdLock> lockFileCollection)
			throws InterruptedException {
		ExecutorService executor = testExecutor(threadNum);
		Map<String, Callable<String>> tasks = getLaunchUuidReadCallables(threadNum, iterableSupplier(lockFileCollection));
		Collection<String> result = executor.invokeAll(tasks.values()).stream().map(new GetFutureResults<>()).collect(toList());
		return ImmutablePair.of(tasks.keySet(), result);
	}

	@Test
	public void test_uuid_remove_finishInstanceUuid() throws InterruptedException {
		int threadNum = 3;
		Pair<Set<String>, Collection<String>> uuidSet = executeParallelLaunchUuidSync(threadNum,
				Collections.nCopies(threadNum, launchIdLockSocket)
		);

		String uuidToRemove = uuidSet.getLeft().stream().skip(2).iterator().next();
		launchIdLockSocket.finishInstanceUuid(uuidToRemove);

		assertThat(launchIdLockSocket.getLiveInstanceUuids(), Matchers.hasSize(threadNum - 1));
		assertThat(launchIdLockSocket.getLiveInstanceUuids(), not(contains(uuidToRemove)));
	}

	@Test
	public void test_launch_uuid_should_not_be_null_obtainLaunchUuid() {
		//noinspection ConstantConditions
		Assertions.assertThrows(NullPointerException.class, () -> launchIdLockSocket.obtainLaunchUuid(null));
	}

	@Test
	public void test_instance_uuid_returns_in_live_list_before_timeout() {
		String launchUuid = UUID.randomUUID().toString();
		launchIdLockSocket.obtainLaunchUuid(launchUuid);

		Collection<String> liveUuids = launchIdLockSocket.getLiveInstanceUuids();
		assertThat(liveUuids, hasSize(1));
		assertThat(liveUuids, contains(launchUuid));
	}

	@Test
	public void test_instance_uuid_remove_from_live_after_timeout() throws InterruptedException {
		String launchUuid = UUID.randomUUID().toString();
		launchIdLockSocket.obtainLaunchUuid(launchUuid);

		Thread.sleep(LOCK_TIMEOUT + 10);

		Collection<String> liveUuids = launchIdLockSocket.getLiveInstanceUuids();
		assertThat(liveUuids, hasSize(0));
	}

	@Test
	public void test_instance_uuid_stay_live_after_timeout_if_updated() throws InterruptedException {
		String launchUuid = UUID.randomUUID().toString();
		launchIdLockSocket.obtainLaunchUuid(launchUuid);

		Thread.sleep(LOCK_TIMEOUT - 100);
		launchIdLockSocket.updateInstanceUuid(launchUuid);
		Thread.sleep(120);

		Collection<String> liveUuids = launchIdLockSocket.getLiveInstanceUuids();
		assertThat(liveUuids, hasSize(1));
		assertThat(liveUuids, contains(launchUuid));
	}

	@Test
	public void test_instance_uuid_removed_from_live_after_finish() {
		String launchUuid = UUID.randomUUID().toString();
		launchIdLockSocket.obtainLaunchUuid(launchUuid);
		launchIdLockSocket.finishInstanceUuid(launchUuid);
		Collection<String> liveUuids = launchIdLockSocket.getLiveInstanceUuids();
		assertThat(liveUuids, hasSize(0));
	}

	@Test
	public void test_live_instance_uuid_update_through_socket() {
		String launchUuid = UUID.randomUUID().toString();
		String clientUuid = UUID.randomUUID().toString();
		launchIdLockSocket.obtainLaunchUuid(launchUuid);
		launchIdLockSocket.sendCommand(LaunchIdLockSocket.Command.UPDATE, clientUuid);
		Collection<String> liveUuids = Awaitility.await()
				.pollDelay(Duration.ZERO)
				.pollInterval(Duration.ofMillis(100))
				.atMost(Duration.ofSeconds(10))
				.until(launchIdLockSocket::getLiveInstanceUuids, hasSize(2));
		assertThat(liveUuids, containsInAnyOrder(launchUuid, clientUuid));
	}

	@Test
	public void test_live_instance_uuid_remove_through_socket() {
		String launchUuid = UUID.randomUUID().toString();
		String clientUuid = UUID.randomUUID().toString();
		String removeUuid = UUID.randomUUID().toString();
		launchIdLockSocket.obtainLaunchUuid(launchUuid);
		launchIdLockSocket.sendCommand(LaunchIdLockSocket.Command.UPDATE, removeUuid);
		launchIdLockSocket.sendCommand(LaunchIdLockSocket.Command.UPDATE, clientUuid);
		Collection<String> liveUuids = launchIdLockSocket.getLiveInstanceUuids();
		assertThat(liveUuids, hasSize(3));
		assertThat(liveUuids, containsInAnyOrder(launchUuid, clientUuid, removeUuid));

		launchIdLockSocket.sendCommand(LaunchIdLockSocket.Command.FINISH, removeUuid);
		liveUuids = launchIdLockSocket.getLiveInstanceUuids();
		assertThat(liveUuids, hasSize(2));
		assertThat(liveUuids, containsInAnyOrder(launchUuid, clientUuid));
	}
}
