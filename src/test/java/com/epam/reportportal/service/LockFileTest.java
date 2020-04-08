/*
 *  Copyright 2019 EPAM Systems
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

import com.epam.reportportal.listeners.ListenerParameters;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;

/**
 * @author <a href="mailto:vadzim_hushchanskou@epam.com">Vadzim Hushchanskou</a>
 */
public class LockFileTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(LockFileTest.class);
	private static final String LOCK_FILE_NAME_PATTERN = "%s.reportportal.lock";
	private static final String SYNC_FILE_NAME_PATTERN = "%s.reportportal.sync";

	private String lockFileName;
	private String syncFileName;
	private LockFile lockFile;
	private Collection<LockFile> lockFileCollection;

	private ListenerParameters getParameters() {
		ListenerParameters params = new ListenerParameters();
		params.setLockFileName(lockFileName);
		params.setSyncFileName(syncFileName);
		return params;
	}

	@BeforeEach
	public void prepare() {
		String fileName = UUID.randomUUID().toString();
		lockFileName = String.format(LOCK_FILE_NAME_PATTERN, fileName);
		syncFileName = String.format(SYNC_FILE_NAME_PATTERN, fileName);
		lockFile = new LockFile(getParameters());
	}

	@AfterEach
	public void cleanUp() {
		lockFile.reset();
		if (lockFileCollection != null) {
			for (LockFile file : lockFileCollection) {
				file.reset();
			}
		}
		final File myLockFile = new File(lockFileName);
		if (myLockFile.exists()) {
			Awaitility.await().until(new Callable<Boolean>() {
				@Override
				public Boolean call() {
					return myLockFile.delete();
				}
			});
		}
		final File mySyncFile = new File(syncFileName);
		if (mySyncFile.exists()) {
			Awaitility.await().until(new Callable<Boolean>() {
				@Override
				public Boolean call() {
					return mySyncFile.delete();
				}
			});
		}
	}

	@Test
	public void test_launch_uuid_will_be_the_same_for_one_thread_obtainLaunchUuid() {
		String firstUuid = UUID.randomUUID().toString();
		String secondUuid = UUID.randomUUID().toString();
		assertThat(secondUuid, is(not(equalTo(firstUuid))));

		String firstLaunchUuid = lockFile.obtainLaunchUuid(firstUuid);
		String secondLaunchUuid = lockFile.obtainLaunchUuid(secondUuid);

		assertThat(secondLaunchUuid, equalTo(firstLaunchUuid));
	}

	private static Callable<String> getObtainLaunchUuidReadCallable(final String selfUuid, final LockFile lockFile) {
		return new Callable<String>() {
			@Override
			public String call() {
				return lockFile.obtainLaunchUuid(selfUuid);
			}
		};
	}

	private static final class GetFutureResults<T> implements Function<Future<T>, T> {
		@Override
		public T apply(Future<T> input) {
			try {
				return input.get();
			} catch (InterruptedException e) {
				LOGGER.error("Interrupted: ", e);
			} catch (ExecutionException e) {
				LOGGER.error("Failed: ", e);
			}
			return null;
		}
	}

	private Map<String, Callable<String>> getLaunchUuidReadCallables(int num, Supplier<LockFile> serviceProvider) {
		Map<String, Callable<String>> results = new HashMap<>();
		for (int i = 0; i < num; i++) {
			String uuid = UUID.randomUUID().toString();
			Callable<String> task = getObtainLaunchUuidReadCallable(uuid, serviceProvider.get());
			results.put(uuid, task);
		}
		return results;
	}

	private <T> Supplier<T> singletonSupplier(final T value) {
		return new Supplier<T>() {
			@Override
			public T get() {
				return value;
			}
		};
	}

	private ExecutorService testExecutor(final int threadNum) {
		return Executors.newFixedThreadPool(threadNum, r -> {
			Thread t = Executors.defaultThreadFactory().newThread(r);
			t.setDaemon(true);
			return t;
		});
	}

	@Test
	public void test_launch_uuid_will_be_the_same_for_ten_threads_obtainLaunchUuid() throws InterruptedException {
		int threadNum = 10;
		ExecutorService executor = testExecutor(threadNum);
		Map<String, Callable<String>> tasks = getLaunchUuidReadCallables(threadNum, singletonSupplier(lockFile));

		Collection<String> results = executor.invokeAll(tasks.values()).stream().map(new GetFutureResults<>()).collect(toList());
		assertThat(results, Matchers.everyItem(equalTo(results.iterator().next())));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void test_lock_file_contains_all_thread_uuids_obtainLaunchUuid() throws InterruptedException, IOException {
		int threadNum = 5;
		ExecutorService executor = testExecutor(threadNum);
		Map<String, Callable<String>> tasks = getLaunchUuidReadCallables(threadNum, singletonSupplier(lockFile));

		// Call Future#get to wait for execution.
		String launchUuid = executor.invokeAll(tasks.values()).stream().map(new GetFutureResults<>()).collect(toList()).iterator().next();

		List<String> syncFileContent = FileUtils.readLines(new File(syncFileName), LockFile.LOCK_FILE_CHARSET);
		assertThat(syncFileContent.get(0), equalTo(launchUuid));
		assertThat(syncFileContent, containsInAnyOrder(tasks.keySet().toArray(new String[0])));
	}

	private <T> Supplier<T> iterableSupplier(final Iterable<T> instanceIterable) {
		return new Supplier<T>() {
			private Iterator<T> instanceIterator = instanceIterable.iterator();

			@Override
			public T get() {
				return instanceIterator.next();
			}
		};
	}

	private Pair<Set<String>, Collection<String>> executeParallelLaunchUuidSync(int threadNum, Iterable<LockFile> lockFileCollection)
			throws InterruptedException {
		ExecutorService executor = testExecutor(threadNum);
		Map<String, Callable<String>> tasks = getLaunchUuidReadCallables(threadNum, iterableSupplier(lockFileCollection));
		Collection<String> result = executor.invokeAll(tasks.values()).stream().map(new GetFutureResults<>()).collect(toList());
		final File testFile = new File(lockFileName);

		Awaitility.await("Wait for .lock file creation").until(new Callable<Boolean>() {
			@Override
			public Boolean call() {
				return testFile.exists();
			}
		}, equalTo(Boolean.TRUE));
		return ImmutablePair.of(tasks.keySet(), result);
	}

	@Test
	public void test_temp_files_are_removed_after_last_uuid_removed_finishInstanceUuid() throws InterruptedException {
		int threadNum = 3;
		Pair<Set<String>, Collection<String>> uuidSet = executeParallelLaunchUuidSync(threadNum, Collections.nCopies(threadNum, lockFile));
		Iterator<String> uuidIterator = uuidSet.getLeft().iterator();
		lockFile.finishInstanceUuid(uuidIterator.next());
		lockFile.finishInstanceUuid(uuidIterator.next());
		lockFile.finishInstanceUuid(uuidIterator.next());

		final File lockFile = new File(lockFileName);
		Awaitility.await("Wait for .lock file removal").until(new Callable<Boolean>() {
			@Override
			public Boolean call() {
				return lockFile.exists();
			}
		}, equalTo(Boolean.FALSE));

		final File syncFile = new File(syncFileName);
		Awaitility.await("Wait for .sync file removal").until(new Callable<Boolean>() {
			@Override
			public Boolean call() {
				return syncFile.exists();
			}
		}, equalTo(Boolean.FALSE));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void test_uuid_remove_finishInstanceUuid() throws InterruptedException, IOException {
		int threadNum = 3;
		Pair<Set<String>, Collection<String>> uuidSet = executeParallelLaunchUuidSync(threadNum, Collections.nCopies(threadNum, lockFile));

		String uuidToRemove = uuidSet.getLeft().iterator().next();
		lockFile.finishInstanceUuid(uuidToRemove);

		List<String> syncFileContent = FileUtils.readLines(new File(syncFileName), LockFile.LOCK_FILE_CHARSET);
		assertThat(syncFileContent, Matchers.hasSize(threadNum - 1));
		assertThat(syncFileContent, not(contains(uuidToRemove)));
	}

	public static Iterable<Integer> threadNumProvider() {
		return Arrays.asList(5, 3, 1);
	}

	@SuppressWarnings("unchecked")
	@ParameterizedTest
	@MethodSource("threadNumProvider")
	public void test_new_uuid_remove_does_not_spoil_lock_file_finishInstanceUuid(final int threadNum)
			throws InterruptedException, IOException {
		Pair<Set<String>, Collection<String>> uuidSet = executeParallelLaunchUuidSync(threadNum, Collections.nCopies(threadNum, lockFile));

		String uuidToRemove = UUID.randomUUID().toString();
		lockFile.finishInstanceUuid(uuidToRemove);

		List<String> syncFileContent = FileUtils.readLines(new File(syncFileName), LockFile.LOCK_FILE_CHARSET);
		assertThat(syncFileContent, Matchers.hasSize(threadNum));
		assertThat(syncFileContent, not(hasItem(uuidToRemove)));
		assertThat(syncFileContent, containsInAnyOrder(uuidSet.getLeft().toArray(new String[0])));
	}

	@ParameterizedTest
	@MethodSource("threadNumProvider")
	public void test_different_lock_file_service_instances_synchronize_correctly(final int threadNum) throws InterruptedException {
		lockFileCollection = new ArrayList<>(threadNum);
		lockFileCollection.add(lockFile);
		for (int i = 1; i < threadNum; i++) {
			lockFileCollection.add(new LockFile(getParameters()));
		}

		Pair<Set<String>, Collection<String>> result = executeParallelLaunchUuidSync(threadNum, lockFileCollection);
		Set<String> instanceUuids = result.getLeft();
		Collection<String> launchUuids = result.getRight();
		String launchUuid = launchUuids.iterator().next();

		assertThat(instanceUuids, hasItem(launchUuid));
		assertThat(launchUuids, everyItem(equalTo(launchUuid)));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void test_lock_and_sync_files_will_be_overwritten_if_not_locked() throws IOException {
		String firstUuid = UUID.randomUUID().toString();
		String secondUuid = UUID.randomUUID().toString();
		assertThat(secondUuid, is(not(equalTo(firstUuid))));

		String firstLaunchUuid = lockFile.obtainLaunchUuid(firstUuid);
		lockFile.reset();
		lockFile = new LockFile(getParameters());
		String secondLaunchUuid = lockFile.obtainLaunchUuid(secondUuid);

		assertThat(secondLaunchUuid, not(equalTo(firstLaunchUuid)));
		lockFile.reset();

		List<String> lockFileContent = FileUtils.readLines(new File(lockFileName), LockFile.LOCK_FILE_CHARSET);
		assertThat(lockFileContent, Matchers.hasSize(1));
		assertThat(lockFileContent, contains(secondLaunchUuid));

		List<String> syncFileContent = FileUtils.readLines(new File(syncFileName), LockFile.LOCK_FILE_CHARSET);
		assertThat(syncFileContent, Matchers.hasSize(1));
		assertThat(syncFileContent, contains(secondLaunchUuid));
	}

	@Test
	public void test_launch_uuid_should_not_be_null_obtainLaunchUuid() {
		Assertions.assertThrows(NullPointerException.class, () -> lockFile.obtainLaunchUuid(null));
	}

	private static Triple<OutputStreamWriter, BufferedReader, BufferedReader> getProcessIos(Process process) {
		return ImmutableTriple.of(
				new OutputStreamWriter(process.getOutputStream()),
				new BufferedReader(new InputStreamReader(process.getInputStream())),
				new BufferedReader(new InputStreamReader(process.getErrorStream()))
		);
	}

	private static void closeIos(Triple<OutputStreamWriter, BufferedReader, BufferedReader> io) {
		try{
			io.getLeft().close();
			io.getMiddle().close();
			io.getRight().close();
		} catch (IOException ignore){

		}
	}

	private static final Predicate<String> WELCOME_MESSAGE_PREDICATE = new Predicate<String>() {
		@Override
		public boolean test(String input) {
			return LockFileRunner.WELCOME_MESSAGE.equals(input);
		}
	};

	@SuppressWarnings("unchecked")
	private static String waitForLine(final BufferedReader reader, final BufferedReader errorReader, final Predicate<String> linePredicate)
			throws IOException {
		try {
			return Awaitility.await("Waiting for a line")
					.timeout(8, TimeUnit.SECONDS)
					.pollInterval(100, TimeUnit.MILLISECONDS)
					.until(new Callable<String>() {
						@Override
						public String call() throws Exception {
							if (!reader.ready()) {
								return null;
							}
							String line;
							while ((line = reader.readLine()) != null) {
								if (linePredicate.test(line)) {
									return line;
								}
							}
							return null;
						}
					}, notNullValue());
		} catch (ConditionTimeoutException e) {
			List<String> errorLines = Collections.EMPTY_LIST;
			if (errorReader.ready()) {
				errorLines = IOUtils.readLines(errorReader);
			}
			String lineSeparator = System.getProperty("line.separator");
			throw new IllegalStateException("Unable to run test class: " + join(errorLines, lineSeparator));
		}
	}

	private static final Predicate<String> ANY_STRING_PREDICATE = new Predicate<String>() {
		@Override
		public boolean test(String input) {
			return !isEmpty(input);
		}
	};

	private static class ExecutableNotFoundException extends RuntimeException {
		public ExecutableNotFoundException(String message) {
			super(message);
		}
	}

	private static String getPathToClass(Class<?> mainClass) {
		return mainClass.getCanonicalName();
	}

	private static Process buildProcess(Class<?> mainClass, String... params) throws IOException {
		String fileSeparator = System.getProperty("file.separator");
		String javaHome = System.getProperty("java.home");
		String executablePath = joinWith(fileSeparator, javaHome, "bin", "java");
		File executableFile = new File(executablePath);
		if (!executableFile.exists()) {
			executablePath = executablePath + ".exe";
			executableFile = new File(executablePath);
			if (!executableFile.exists()) {
				throw new ExecutableNotFoundException("Unable to find java executable file.");
			}
		}
		List<String> paramList = new ArrayList<>();
		paramList.add(executablePath);
		paramList.add("-classpath");
		paramList.add(System.getProperty("java.class.path"));
		paramList.add(getPathToClass(mainClass));
		paramList.addAll(Arrays.asList(params));
		return new ProcessBuilder(paramList).start();
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
