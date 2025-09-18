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

import com.epam.reportportal.exception.InternalReportPortalClientException;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.utils.Waiter;
import com.epam.reportportal.utils.properties.ListenerProperty;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * A service to perform blocking I/O operations on '.lock' and '.sync' file to get single launch UUID for multiple clients on a machine.
 * This class uses a local storage disk, therefore applicable in scope of a single hardware machine. You can control lock and sync file
 * paths with {@link ListenerProperty#FILE_LOCK_NAME} and {@link ListenerProperty#FILE_SYNC_NAME} properties.
 *
 * @author <a href="mailto:vadzim_hushchanskou@epam.com">Vadzim Hushchanskou</a>
 */
public class LaunchIdLockFile extends AbstractLaunchIdLock {
	private static final Logger LOGGER = LoggerFactory.getLogger(LaunchIdLockFile.class);

	public static final Charset LOCK_FILE_CHARSET = StandardCharsets.ISO_8859_1;
	public static final String TIME_SEPARATOR = ":";
	private static final String LINE_SEPARATOR = System.lineSeparator();

	private final File lockFile;
	private final File syncFile;
	private final long fileWaitTimeout;
	private static volatile String lockUuid;
	private static volatile Pair<RandomAccessFile, FileLock> mainLock;

	private interface IoOperation<T> {
		T execute(@Nonnull final Pair<RandomAccessFile, FileLock> lock) throws IOException;
	}

	private static class UuidAppend implements IoOperation<String> {
		final String uuid;

		public UuidAppend(@Nonnull final String instanceUuid) {
			uuid = instanceUuid;
		}

		@Override
		public String execute(@Nonnull Pair<RandomAccessFile, FileLock> lock) throws IOException {
			RandomAccessFile access = lock.getKey();
			appendUuid(access, uuid);
			return uuid;
		}
	}

	private static class LaunchRead extends UuidAppend {

		public LaunchRead(@Nonnull final String instanceUuid) {
			super(instanceUuid);
		}

		@Override
		public String execute(@Nonnull Pair<RandomAccessFile, FileLock> lock) throws IOException {
			RandomAccessFile access = lock.getKey();
			String launchUuid = readLaunchUuid(access);
			super.execute(lock);
			return ofNullable(launchUuid).orElse(uuid);
		}
	}

	public LaunchIdLockFile(@Nonnull final ListenerParameters listenerParameters) {
		super(listenerParameters);
		lockFile = new File(parameters.getLockFileName());
		syncFile = new File(parameters.getSyncFileName());
		fileWaitTimeout = parameters.getLockWaitTimeout();
	}

	@Nullable
	private Pair<RandomAccessFile, FileLock> obtainLock(@Nonnull final File file) {
		final String filePath = file.getPath();
		RandomAccessFile lockAccess;
		try {
			lockAccess = new RandomAccessFile(file, "rwd");
		} catch (FileNotFoundException e) {
			LOGGER.debug("Unable to open '{}' file: {}", filePath, e.getLocalizedMessage(), e);
			return null;
		}

		try {
			FileLock lock = lockAccess.getChannel().tryLock();
			if (lock == null) {
				closeAccess(lockAccess);
				return null;
			}
			return Pair.of(lockAccess, lock);
		} catch (OverlappingFileLockException e) {
			LOGGER.debug("Lock already acquired on '{}' file: {}", filePath, e.getLocalizedMessage(), e);
		} catch (ClosedChannelException e) {
			LOGGER.warn("Channel was already closed on '{}' file: {}", filePath, e.getLocalizedMessage(), e);
		} catch (IOException e) {
			LOGGER.warn("Unexpected I/O exception while obtaining mainLock on '{}' file: {}", filePath, e.getLocalizedMessage(), e);
		}
		closeAccess(lockAccess);
		return null;
	}

	private static void releaseLock(@Nonnull final FileLock lock) {
		try {
			lock.release();
		} catch (ClosedChannelException e) {
			LOGGER.warn("Channel was already closed for file mainLock: {}", e.getLocalizedMessage(), e);
		} catch (IOException e) {
			LOGGER.warn("Unexpected I/O exception while releasing file mainLock: {}", e.getLocalizedMessage(), e);
		}
	}

	private static void closeAccess(@Nonnull final RandomAccessFile access) {
		try {
			access.close();
		} catch (IOException e) {
			LOGGER.warn("Unexpected I/O exception while closing file: {}", e.getLocalizedMessage(), e);
		}
	}

	@Nullable
	private static String readLaunchUuid(@Nonnull final RandomAccessFile access) throws IOException {
		String launchRecord = access.readLine();
		if (launchRecord == null) {
			return null;
		}
		return launchRecord.substring(launchRecord.indexOf(TIME_SEPARATOR) + 1);
	}

	private static void writeString(@Nonnull final RandomAccessFile access, @Nonnull final String text) throws IOException {
		access.write(text.getBytes(LOCK_FILE_CHARSET));
	}

	private static void writeLine(@Nonnull final RandomAccessFile access, @Nonnull final String text) throws IOException {
		writeString(access, text + LINE_SEPARATOR);
	}

	private static void closeIo(@Nonnull Pair<RandomAccessFile, FileLock> io) {
		releaseLock(io.getRight());
		closeAccess(io.getLeft());
	}

	@Nullable
	private <T> T executeOperation(@Nonnull final IoOperation<T> operation, @Nonnull final Pair<RandomAccessFile, FileLock> fileIo) {
		try {
			return operation.execute(fileIo);
		} catch (IOException e) {
			// operations failed with IOException will be retried according to timeout and retries number
			LOGGER.error("Unable to read/write a file after obtaining mainLock: {}", e.getMessage(), e);
		}
		return null;
	}

	@Nullable
	private <T> T executeBlockingOperation(@Nonnull final IoOperation<T> operation, @Nonnull final File file) {
		return new Waiter("Wait for a blocking operation on file '" + file.getPath() + "'").duration(fileWaitTimeout, TimeUnit.MILLISECONDS)
				.applyRandomDiscrepancy(MAX_WAIT_TIME_DISCREPANCY)
				.till(() -> {
					Pair<RandomAccessFile, FileLock> fileIo = obtainLock(file);
					if (fileIo != null) {
						T result = executeOperation(operation, fileIo);
						closeIo(fileIo);
						return result;
					}
					return null;
				});
	}

	private void rewriteWith(@Nonnull final RandomAccessFile access, @Nonnull final String content) throws IOException {
		access.setLength(content.length());
		writeLine(access, content);
	}

	void reset() {
		if (mainLock != null) {
			closeIo(mainLock);
			mainLock = null;
		}
		lockUuid = null;
	}

	@Nonnull
	private static String getRecord(@Nonnull final String instanceUuid) {
		return System.currentTimeMillis() + TIME_SEPARATOR + instanceUuid;
	}

	private void writeLaunchUuid(@Nonnull final Pair<RandomAccessFile, FileLock> syncIo) {
		String launchRecord = getRecord(lockUuid);
		try {
			rewriteWith(syncIo.getLeft(), launchRecord);
			rewriteWith(mainLock.getLeft(), launchRecord);
		} catch (IOException e) {
			// operations failed with IOException
			String error = "Unable to read/write a file after obtaining lock: " + e.getMessage();
			LOGGER.warn(error, e);
			reset();
			throw new InternalReportPortalClientException(error, e);
		}
	}

	private static void appendUuid(@Nonnull final RandomAccessFile access, @Nonnull final String uuid) throws IOException {
		access.seek(access.length());
		writeLine(access, getRecord(uuid));
	}

	private void writeInstanceUuid(@Nonnull final String instanceUuid) {
		executeBlockingOperation(new UuidAppend(instanceUuid), syncFile);
	}

	@Nullable
	private String obtainLaunch(@Nonnull final String instanceUuid) {
		return executeBlockingOperation(new LaunchRead(instanceUuid), syncFile);
	}

	/**
	 * Returns a Launch UUID for many Clients launched on one machine.
	 *
	 * @param instanceUuid a Client instance UUID, which will be written to lock and sync files and, if it is the first thread which managed
	 *                     to obtain lock on '.lock' file, returned to every client instance.
	 * @return either a Client instance UUID, either the first UUID which thread managed to place a lock on a '.lock' file.
	 */
	@Override
	@Nullable
	public String obtainLaunchUuid(@Nonnull final String instanceUuid) {
		Objects.requireNonNull(instanceUuid);
		if (mainLock != null) {
			if (!instanceUuid.equals(lockUuid)) {
				writeInstanceUuid(instanceUuid);
			}
			return lockUuid;
		}
		Pair<RandomAccessFile, FileLock> syncLock = obtainLock(syncFile);
		if (syncLock != null) {
			if (mainLock == null) {
				Pair<RandomAccessFile, FileLock> lock = obtainLock(lockFile);
				if (lock != null) {
					// we are the main thread / process
					lockUuid = instanceUuid;
					mainLock = lock;
					writeLaunchUuid(syncLock);
					closeIo(syncLock);
					return instanceUuid;
				} else {
					executeOperation(new LaunchRead(instanceUuid), syncLock);
					closeIo(syncLock);
				}
			} else {
				// another thread obtained main lock while we wait for .sync file
				executeOperation(new UuidAppend(instanceUuid), syncLock);
				closeIo(syncLock);
				return lockUuid;
			}
			// main lock file already locked, just close sync lock and proceed with secondary launch logic
		}
		return obtainLaunch(instanceUuid);
	}

	/**
	 * Update timestamp for instance record in sync file.
	 *
	 * @param instanceUuid instanceUuid a Client instance UUID
	 */
	@Override
	public void updateInstanceUuid(@Nonnull String instanceUuid) {
		IoOperation<Boolean> uuidUpdate = fileIo -> {
			String line;
			List<String> recordList = new ArrayList<>();
			RandomAccessFile fileAccess = fileIo.getKey();
			boolean needUpdate = false;
			while ((line = fileAccess.readLine()) != null) {
				String record = line.trim();
				String uuid = record.substring(record.indexOf(TIME_SEPARATOR) + 1);
				if (instanceUuid.equals(uuid)) {
					String newRecord = getRecord(instanceUuid);
					if (!newRecord.equals(record)) {
						needUpdate = true;
						recordList.add(newRecord);
					} else {
						recordList.add(record);
					}
				} else {
					recordList.add(record);
				}
			}

			if (needUpdate) {
				fileAccess.seek(0);
				for (String record : recordList) {
					writeLine(fileAccess, record);
				}
			}
			return needUpdate;
		};
		executeBlockingOperation(uuidUpdate, syncFile);
	}

	/**
	 * Remove self UUID from sync file, means that a client finished its Launch. If this is the last UUID in the sync file, lock and sync
	 * files will be removed.
	 *
	 * @param instanceUuid a Client instance UUID.
	 */
	@Override
	public void finishInstanceUuid(@Nonnull final String instanceUuid) {
		IoOperation<Boolean> uuidRemove = fileIo -> {
			String line;
			List<String> recordList = new ArrayList<>();
			RandomAccessFile fileAccess = fileIo.getKey();
			boolean needUpdate = false;
			while ((line = fileAccess.readLine()) != null) {
				String record = line.trim();
				String launchUuid = record.substring(record.indexOf(TIME_SEPARATOR) + 1);
				if (instanceUuid.equals(launchUuid)) {
					needUpdate = true;
					continue;
				}
				recordList.add(record);
			}

			if (!needUpdate) {
				return false;
			}

			String recordNl = System.currentTimeMillis() + TIME_SEPARATOR + instanceUuid + LINE_SEPARATOR;
			long newLength = fileAccess.length() - recordNl.length();
			if (newLength > 0) {
				fileAccess.setLength(newLength);
				fileAccess.seek(0);
				for (String record : recordList) {
					writeLine(fileAccess, record);
				}
				return false;
			} else {
				fileIo.getKey().setLength(0);
				return true;
			}
		};

		Boolean isLast = executeBlockingOperation(uuidRemove, syncFile);
		if (isLast != null && isLast) {
			if (!syncFile.delete()) {
				LOGGER.warn("Unable to delete synchronization file: {}", syncFile.getPath());
			}
		}

		if (mainLock != null && lockUuid.equals(instanceUuid)) {
			reset();
			if (!lockFile.delete()) {
				LOGGER.warn("Unable to delete locking file: {}", lockFile.getPath());
			}
		}
	}

	@Nonnull
	@Override
	public Collection<String> getLiveInstanceUuids() {
		IoOperation<List<String>> uuidListRead = fileIo -> {
			String line;
			List<String> recordList = new ArrayList<>();
			RandomAccessFile fileAccess = fileIo.getKey();
			while ((line = fileAccess.readLine()) != null) {
				String record = line.trim();
				recordList.add(record);
			}
			return recordList;
		};
		long timeoutTime = System.currentTimeMillis() - fileWaitTimeout;
		return ofNullable(executeBlockingOperation(uuidListRead, syncFile)).orElse(Collections.emptyList())
				.stream()
				.map(r -> Pair.of(Long.parseLong(r.substring(0, r.indexOf(TIME_SEPARATOR))), r.substring(r.indexOf(TIME_SEPARATOR) + 1)))
				.filter(r -> r.getKey() > timeoutTime)
				.map(Pair::getValue)
				.collect(Collectors.toSet());
	}
}
