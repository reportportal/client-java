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
import com.epam.reportportal.util.test.CommonUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.channels.FileLock;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static com.epam.reportportal.service.launch.lock.LockTestUtil.*;
import static com.epam.reportportal.util.test.ProcessUtils.*;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * @author <a href="mailto:vadzim_hushchanskou@epam.com">Vadzim Hushchanskou</a>
 */
public class LaunchIdLockFileTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(LaunchIdLockFileTest.class);
	private static final String LOCK_FILE_NAME_PATTERN = "%s.reportportal.lock";
	private static final String SYNC_FILE_NAME_PATTERN = "%s.reportportal.sync";

	private final String fileName = UUID.randomUUID().toString();
	private final String lockFileName = String.format(LOCK_FILE_NAME_PATTERN, fileName);
	private final String syncFileName = String.format(SYNC_FILE_NAME_PATTERN, fileName);
	private LaunchIdLockFile launchIdLockFile = new LaunchIdLockFile(getParameters());
	private Collection<LaunchIdLockFile> launchIdLockCollection;

	private ListenerParameters getParameters() {
		ListenerParameters params = new ListenerParameters();
		params.setLockFileName(lockFileName);
		params.setSyncFileName(syncFileName);
		params.setEnable(Boolean.TRUE);
		params.setLockWaitTimeout(LOCK_TIMEOUT);
		return params;
	}

	@AfterEach
	public void cleanUp() {
		launchIdLockFile.reset();
		if (launchIdLockCollection != null) {
			for (LaunchIdLockFile file : launchIdLockCollection) {
				file.reset();
			}
		}
		final File myLockFile = new File(lockFileName);
		if (myLockFile.exists()) {
			Awaitility.await().atMost(Duration.ofSeconds(60)).until(myLockFile::delete);
		}
		final File mySyncFile = new File(syncFileName);
		if (mySyncFile.exists()) {
			Awaitility.await().atMost(Duration.ofSeconds(60)).until(mySyncFile::delete);
		}
	}

	@Test
	public void test_launch_uuid_will_be_the_same_for_one_thread_obtainLaunchUuid() {
		String firstUuid = UUID.randomUUID().toString();
		String secondUuid = UUID.randomUUID().toString();
		assertThat(secondUuid, is(not(equalTo(firstUuid))));

		String firstLaunchUuid = launchIdLockFile.obtainLaunchUuid(firstUuid);
		String secondLaunchUuid = launchIdLockFile.obtainLaunchUuid(secondUuid);

		assertThat(secondLaunchUuid, equalTo(firstLaunchUuid));
	}

	@Test
	public void test_launch_uuid_will_be_the_same_for_ten_threads_obtainLaunchUuid() throws InterruptedException {
		int threadNum = 10;
		try(CommonUtils.ExecutorService executor = CommonUtils.testExecutor(threadNum)) {
			Map<String, Callable<String>> tasks = getLaunchUuidReadCallables(threadNum, singletonSupplier(launchIdLockFile));

			Collection<String> results = executor.invokeAll(tasks.values()).stream().map(new GetFutureResults<>()).collect(toList());
			assertThat(results, hasSize(threadNum));
			assertThat(results, Matchers.everyItem(equalTo(results.iterator().next())));
		}
	}

	@Test
	public void test_sync_file_contains_all_thread_uuids_obtainLaunchUuid() throws InterruptedException, IOException {
		int threadNum = 5;
		try(CommonUtils.ExecutorService executor = CommonUtils.testExecutor(threadNum)) {
			Map<String, Callable<String>> tasks = getLaunchUuidReadCallables(threadNum, singletonSupplier(launchIdLockFile));

			// Call Future#get to wait for execution.
			String launchUuid = executor.invokeAll(tasks.values())
					.stream()
					.map(new GetFutureResults<>())
					.collect(toList())
					.iterator()
					.next();

			List<String> syncFileContent = FileUtils.readLines(new File(syncFileName), LaunchIdLockFile.LOCK_FILE_CHARSET.name());
			assertThat(syncFileContent.get(0), matchesPattern("\\d+:" + launchUuid));
			List<String> syncFileContentUuids = syncFileContent.stream()
					.map(r -> r.substring(r.indexOf(LaunchIdLockFile.TIME_SEPARATOR) + 1))
					.collect(Collectors.toList());
			assertThat(syncFileContentUuids, containsInAnyOrder(tasks.keySet().toArray(new String[0])));
		}
	}

	private Pair<Set<String>, Collection<String>> executeParallelLaunchUuidSync(int threadNum,
			Iterable<LaunchIdLockFile> lockFileCollection) throws InterruptedException {
		try(CommonUtils.ExecutorService executor = CommonUtils.testExecutor(threadNum)) {
			Map<String, Callable<String>> tasks = getLaunchUuidReadCallables(threadNum, iterableSupplier(lockFileCollection));
			Collection<String> result = executor.invokeAll(tasks.values()).stream().map(new GetFutureResults<>()).collect(toList());
			final File testFile = new File(lockFileName);

			Awaitility.await("Wait for .lock file creation").until(testFile::exists, equalTo(Boolean.TRUE));
			return ImmutablePair.of(tasks.keySet(), result);
		}
	}

	@Test
	public void test_temp_files_are_removed_after_last_uuid_removed_finishInstanceUuid() throws InterruptedException {
		int threadNum = 3;
		Pair<Set<String>, Collection<String>> uuidSet = executeParallelLaunchUuidSync(
				threadNum,
				Collections.nCopies(threadNum, launchIdLockFile)
		);
		Iterator<String> uuidIterator = uuidSet.getLeft().iterator();
		launchIdLockFile.finishInstanceUuid(uuidIterator.next());
		launchIdLockFile.finishInstanceUuid(uuidIterator.next());
		launchIdLockFile.finishInstanceUuid(uuidIterator.next());

		final File lockFile = new File(lockFileName);
		Awaitility.await("Wait for .lock file removal").until(lockFile::exists, equalTo(Boolean.FALSE));

		final File syncFile = new File(syncFileName);
		Awaitility.await("Wait for .sync file removal").until(syncFile::exists, equalTo(Boolean.FALSE));
	}

	@Test
	public void test_uuid_remove_finishInstanceUuid() throws InterruptedException, IOException {
		int threadNum = 3;
		Pair<Set<String>, Collection<String>> uuidSet = executeParallelLaunchUuidSync(
				threadNum,
				Collections.nCopies(threadNum, launchIdLockFile)
		);

		String uuidToRemove = uuidSet.getLeft().iterator().next();
		launchIdLockFile.finishInstanceUuid(uuidToRemove);

		List<String> syncFileContent = FileUtils.readLines(new File(syncFileName), LaunchIdLockFile.LOCK_FILE_CHARSET.name());
		assertThat(syncFileContent, Matchers.hasSize(threadNum - 1));
		assertThat(syncFileContent, not(contains(uuidToRemove)));
	}

	@SuppressWarnings("unused")
	public static Iterable<Integer> threadNumProvider() {
		return Arrays.asList(5, 3, 1);
	}

	@ParameterizedTest
	@MethodSource("threadNumProvider")
	public void test_new_uuid_remove_does_not_spoil_lock_file_finishInstanceUuid(final int threadNum)
			throws InterruptedException, IOException {
		Pair<Set<String>, Collection<String>> uuidSet = executeParallelLaunchUuidSync(
				threadNum,
				Collections.nCopies(threadNum, launchIdLockFile)
		);

		String uuidToRemove = UUID.randomUUID().toString();
		launchIdLockFile.finishInstanceUuid(uuidToRemove);

		List<String> syncFileContent = FileUtils.readLines(new File(syncFileName), LaunchIdLockFile.LOCK_FILE_CHARSET.name());
		List<String> syncFileContentUuids = syncFileContent.stream()
				.map(r -> r.substring(r.indexOf(LaunchIdLockFile.TIME_SEPARATOR) + 1))
				.collect(Collectors.toList());
		assertThat(syncFileContentUuids, Matchers.hasSize(threadNum));
		assertThat(syncFileContentUuids, not(hasItem(uuidToRemove)));
		assertThat(syncFileContentUuids, containsInAnyOrder(uuidSet.getLeft().toArray(new String[0])));
	}

	@ParameterizedTest
	@MethodSource("threadNumProvider")
	public void test_different_lock_file_service_instances_synchronize_correctly(final int threadNum) throws InterruptedException {
		launchIdLockCollection = new ArrayList<>(threadNum);
		launchIdLockCollection.add(launchIdLockFile);
		for (int i = 1; i < threadNum; i++) {
			launchIdLockCollection.add(new LaunchIdLockFile(getParameters()));
		}

		Pair<Set<String>, Collection<String>> result = executeParallelLaunchUuidSync(threadNum, launchIdLockCollection);
		Set<String> instanceUuids = result.getLeft();
		Collection<String> launchUuids = result.getRight();
		String launchUuid = launchUuids.iterator().next();

		assertThat(instanceUuids, hasItem(launchUuid));
		assertThat(launchUuids, everyItem(equalTo(launchUuid)));
	}

	@Test
	public void test_lock_and_sync_files_will_be_overwritten_if_not_locked() throws IOException {
		String firstUuid = UUID.randomUUID().toString();
		String secondUuid = UUID.randomUUID().toString();
		assertThat(secondUuid, is(not(equalTo(firstUuid))));

		String firstLaunchUuid = launchIdLockFile.obtainLaunchUuid(firstUuid);
		launchIdLockFile.reset();
		launchIdLockFile = new LaunchIdLockFile(getParameters());
		String secondLaunchUuid = launchIdLockFile.obtainLaunchUuid(secondUuid);

		assertThat(secondLaunchUuid, not(equalTo(firstLaunchUuid)));
		launchIdLockFile.reset();

		List<String> lockFileContent = FileUtils.readLines(new File(lockFileName), LaunchIdLockFile.LOCK_FILE_CHARSET.name());
		assertThat(lockFileContent, Matchers.hasSize(1));
		assertThat(lockFileContent, contains(matchesPattern("\\d+:" + secondLaunchUuid)));

		List<String> syncFileContent = FileUtils.readLines(new File(syncFileName), LaunchIdLockFile.LOCK_FILE_CHARSET.name());
		assertThat(syncFileContent, Matchers.hasSize(1));
		assertThat(syncFileContent, contains(matchesPattern("\\d+:" + secondLaunchUuid)));
	}

	@Test
	public void test_launch_uuid_should_not_be_null_obtainLaunchUuid() {
		//noinspection ConstantConditions
		Assertions.assertThrows(NullPointerException.class, () -> launchIdLockFile.obtainLaunchUuid(null));
	}

	@Test
	public void test_lock_file_should_not_throw_exception_if_it_is_not_possible_to_write_sync_file() throws IOException {
		File syncFile = new File(syncFileName);
		try (RandomAccessFile raf = new RandomAccessFile(syncFile, "rwd")) {
			try (FileLock ignored = raf.getChannel().lock()) {
				assertThat(launchIdLockFile.obtainLaunchUuid(UUID.randomUUID().toString()), nullValue());
			}
		}
	}

	@Test
	public void test_lock_file_should_not_throw_exception_if_it_is_not_possible_to_write_lock_file() throws IOException {
		String launchUuid = UUID.randomUUID().toString();
		File lockFile = new File(lockFileName);
		try (RandomAccessFile raf = new RandomAccessFile(lockFile, "rwd")) {
			try (FileLock ignored = raf.getChannel().lock()) {
				assertThat(launchIdLockFile.obtainLaunchUuid(launchUuid), equalTo(launchUuid));
			}
		}
	}

	@Test
	@Timeout(10)
	public void test_launch_uuid_get_for_two_processes_returns_equal_values_obtainLaunchUuid() throws IOException, InterruptedException {
		Pair<String, String> uuids = ImmutablePair.of(UUID.randomUUID().toString(), UUID.randomUUID().toString());

		LOGGER.info("Running two separate processes");
		// @formatter:off
		Pair<Process, Process> processes = ImmutablePair.of(
				buildProcess(LockFileRunner.class, lockFileName, syncFileName, uuids.getKey()),
				buildProcess(LockFileRunner.class, lockFileName, syncFileName, uuids.getValue())
		);
		// @formatter:on

		Triple<OutputStreamWriter, BufferedReader, BufferedReader> primaryProcessIo = getProcessIos(processes.getKey());
		Triple<OutputStreamWriter, BufferedReader, BufferedReader> secondaryProcessIo = getProcessIos(processes.getValue());

		try {

			waitForLine(primaryProcessIo.getMiddle(), primaryProcessIo.getRight(), WELCOME_MESSAGE_PREDICATE);
			waitForLine(secondaryProcessIo.getMiddle(), secondaryProcessIo.getRight(), WELCOME_MESSAGE_PREDICATE);

			String lineSeparator = System.lineSeparator();
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

	@Test
	public void test_instance_uuid_returns_in_live_list_before_timeout() {
		String launchUuid = UUID.randomUUID().toString();
		launchIdLockFile.obtainLaunchUuid(launchUuid);

		Collection<String> liveUuids = launchIdLockFile.getLiveInstanceUuids();
		assertThat(liveUuids, hasSize(1));
		assertThat(liveUuids, contains(launchUuid));
	}

	@Test
	public void test_instance_uuid_remove_from_live_after_timeout() throws InterruptedException {
		String launchUuid = UUID.randomUUID().toString();
		launchIdLockFile.obtainLaunchUuid(launchUuid);

		Thread.sleep(LOCK_TIMEOUT + 10);

		Collection<String> liveUuids = launchIdLockFile.getLiveInstanceUuids();
		assertThat(liveUuids, hasSize(0));
	}

	@Test
	public void test_instance_uuid_stay_live_after_timeout_if_updated() throws InterruptedException {
		String launchUuid = UUID.randomUUID().toString();
		launchIdLockFile.obtainLaunchUuid(launchUuid);

		Thread.sleep(LOCK_TIMEOUT - 100);
		launchIdLockFile.updateInstanceUuid(launchUuid);
		Thread.sleep(120);

		Collection<String> liveUuids = launchIdLockFile.getLiveInstanceUuids();
		assertThat(liveUuids, hasSize(1));
		assertThat(liveUuids, contains(launchUuid));
	}

	@Test
	public void test_instance_uuid_removed_from_live_after_finish() {
		String launchUuid = UUID.randomUUID().toString();
		launchIdLockFile.obtainLaunchUuid(launchUuid);
		launchIdLockFile.finishInstanceUuid(launchUuid);
		Collection<String> liveUuids = launchIdLockFile.getLiveInstanceUuids();
		assertThat(liveUuids, hasSize(0));
	}
}
