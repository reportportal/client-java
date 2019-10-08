package com.epam.reportportal.service;

import com.epam.reportportal.exception.InternalReportPortalClientException;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.utils.Waiter;
import com.epam.reportportal.utils.properties.ListenerProperty;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * A service to perform blocking I/O operations on '.lock' and '.sync' file to get single launch UUID for multiple clients on a machine.
 * This class uses a local storage disk, therefore applicable in scope of a single hardware machine. You can control lock and sync file
 * paths with {@link ListenerProperty#LOCK_FILE_NAME} and {@link ListenerProperty#SYNC_FILE_NAME} properties.
 */
public class LockFile implements LaunchIdLock {
	private static final Logger LOGGER = LoggerFactory.getLogger(LockFile.class);

	public static final String LOCK_FILE_CHARSET = "US-ASCII";
	private static final String LINE_SEPARATOR = System.getProperty("line.separator");
	private static final float MAX_WAIT_TIME_DISCREPANCY = 0.1f;

	private final File lockFile;
	private final File syncFile;
	private final long fileWaitTimeout;
	private volatile String lockUuid;
	private volatile Pair<RandomAccessFile, FileLock> mainLock;

	public LockFile(ListenerParameters parameters) {
		lockFile = new File(parameters.getLockFileName());
		syncFile = new File(parameters.getSyncFileName());
		fileWaitTimeout = parameters.getFileWaitTimeout();
	}

	private Pair<RandomAccessFile, FileLock> obtainLock(final File file) {
		final String filePath = file.getPath();
		RandomAccessFile lockAccess;
		try {
			lockAccess = new RandomAccessFile(file, "rwd");
		} catch (FileNotFoundException e) {
			LOGGER.info("Unable to open '{}' file: {}", filePath, e.getLocalizedMessage(), e);
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

	private Pair<RandomAccessFile, FileLock> waitForLock(final File file) {
		return new Waiter("Wait for file '" + file.getPath() + "' lock").duration(fileWaitTimeout, TimeUnit.MILLISECONDS)
				.applyRandomDiscrepancy(MAX_WAIT_TIME_DISCREPANCY)
				.till(new Callable<Pair<RandomAccessFile, FileLock>>() {
					@Override
					public Pair<RandomAccessFile, FileLock> call() {
						return obtainLock(file);
					}
				});
	}

	private static void releaseLock(FileLock lock) {
		try {
			lock.release();
		} catch (ClosedChannelException e) {
			LOGGER.warn("Channel was already closed for file mainLock: {}", e.getLocalizedMessage(), e);
		} catch (IOException e) {
			LOGGER.warn("Unexpected I/O exception while releasing file mainLock: {}", e.getLocalizedMessage(), e);
		}
	}

	private static void closeAccess(RandomAccessFile access) {
		try {
			access.close();
		} catch (IOException e) {
			LOGGER.warn("Unexpected I/O exception while closing file: {}", e.getLocalizedMessage(), e);
		}
	}

	private static String readLaunchUuid(RandomAccessFile access) throws IOException {
		return access.readLine();
	}

	private static void writeString(RandomAccessFile access, String text) throws IOException {
		access.write((text).getBytes(LOCK_FILE_CHARSET));
	}

	private static void writeLine(RandomAccessFile access, String text) throws IOException {
		writeString(access, text + LINE_SEPARATOR);
	}

	private interface IoOperation<T> {
		T execute(Pair<RandomAccessFile, FileLock> lock) throws IOException;
	}

	private static void closeIo(Pair<RandomAccessFile, FileLock> io) {
		releaseLock(io.getRight());
		closeAccess(io.getLeft());
	}

	private <T> T executeBockingOperation(final IoOperation<T> operation, final File file) {
		return new Waiter("Wait for a blocking operation on file '" + file.getPath() + "'")
				.duration(fileWaitTimeout, TimeUnit.MILLISECONDS)
				.applyRandomDiscrepancy(MAX_WAIT_TIME_DISCREPANCY)
				.till(new Callable<T>() {
					@Override
					public T call() {
						Pair<RandomAccessFile, FileLock> fileIo = obtainLock(file);
						if (fileIo != null) {
							try {
								return operation.execute(fileIo);
							} catch (IOException e) {
								// operations failed with IOException will be retried according to timeout and retries number
								LOGGER.error("Unable to read/write a file after obtaining mainLock: " + e.getMessage(), e);
							} finally {
								closeIo(fileIo);
							}
						}
						return null;
					}
				});
	}

	private void rewriteWith(RandomAccessFile access, String content) throws IOException {
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

	private void writeLaunchUuid(Pair<RandomAccessFile, FileLock> syncIo) {
		try {
			rewriteWith(syncIo.getLeft(), lockUuid);
			rewriteWith(mainLock.getLeft(), lockUuid);
		} catch (IOException e) {
			// operations failed with IOException
			String error = "Unable to read/write a file after obtaining lock: " + e.getMessage();
			LOGGER.warn(error, e);
			reset();
			throw new InternalReportPortalClientException(error, e);
		}
	}

	private static void appendUuid(RandomAccessFile access, String uuid) throws IOException {
		access.seek(access.length());
		writeLine(access, uuid);
	}

	private void writeInstanceUuid(final String instanceUuid) {
		IoOperation<Boolean> uuidRead = new IoOperation<Boolean>() {
			public Boolean execute(Pair<RandomAccessFile, FileLock> fileIo) throws IOException {
				appendUuid(fileIo.getKey(), instanceUuid);
				return Boolean.TRUE;
			}
		};
		Boolean result = executeBockingOperation(uuidRead, syncFile);
		if (result == null || !result) {
			throw new InternalReportPortalClientException("Unable to write instance UUID to a sync file: " + syncFile.getPath());
		}
	}

	private String obtainLaunch(final String instanceUuid) {
		IoOperation<String> uuidRead = new IoOperation<String>() {
			public String execute(Pair<RandomAccessFile, FileLock> fileIo) throws IOException {
				RandomAccessFile access = fileIo.getKey();
				String uuid = readLaunchUuid(access);
				appendUuid(access, instanceUuid);
				return uuid != null ? uuid : instanceUuid;
			}
		};
		String result = executeBockingOperation(uuidRead, syncFile);
		if (result == null) {
			throw new InternalReportPortalClientException("Unable to read launch UUID from sync file: " + syncFile.getPath());
		}
		return result;
	}

	/**
	 * Returns a Launch UUID for many Clients launched on one machine.
	 *
	 * @param uuid a Client instance UUID, which will be written to lock and sync files and, if it the first thread which managed to
	 *             obtain lock on '.lock' file, returned to every client instance.
	 * @return either a Client instance UUID, either the first UUID which thread managed to place a lock on a '.lock' file.
	 */
	@Override
	public String obtainLaunchUuid(final String uuid) {
		assert uuid != null;
		if (mainLock != null) {
			if (!uuid.equals(lockUuid)) {
				writeInstanceUuid(uuid);
			}
			return lockUuid;
		}
		Pair<RandomAccessFile, FileLock> syncLock = waitForLock(syncFile);
		if (syncLock != null) {
			try {
				if (mainLock == null) {
					Pair<RandomAccessFile, FileLock> lock = obtainLock(lockFile);
					if (lock != null) {
						// we are the main thread
						mainLock = lock;
						lockUuid = uuid;
						writeLaunchUuid(syncLock);
						return uuid;
					}
				}
			} finally {
				closeIo(syncLock);
			}
			// main lock file already locked, just close sync lock (through finally) and proceed with secondary launch logic
		}

		return obtainLaunch(uuid);
	}

	/**
	 * Remove self UUID from sync file, means that a client finished its Launch. If this is the last UUID in the sync file, lock and sync
	 * files will be removed.
	 *
	 * @param uuid a Client instance UUID.
	 */
	@Override
	public void finishInstanceUuid(final String uuid) {
		if (uuid == null) {
			return;
		}
		IoOperation<Boolean> uuidRemove = new IoOperation<Boolean>() {
			public Boolean execute(Pair<RandomAccessFile, FileLock> fileIo) throws IOException {
				String line;
				List<String> uuidList = new ArrayList<String>();
				RandomAccessFile fileAccess = fileIo.getKey();
				boolean needUpdate = false;
				while ((line = fileAccess.readLine()) != null) {
					String trimmedLine = line.trim();
					if (uuid.equals(trimmedLine)) {
						needUpdate = true;
						continue;
					}
					uuidList.add(trimmedLine);
				}

				if (!needUpdate) {
					return false;
				}

				String uuidNl = uuid + LINE_SEPARATOR;
				long newLength = fileAccess.length() - uuidNl.length();
				if (newLength > 0) {
					fileAccess.setLength(newLength);
					fileAccess.seek(0);
					for (String uuid : uuidList) {
						writeLine(fileAccess, uuid);
					}
					return false;
				} else {
					fileIo.getKey().setLength(0);
					return true;
				}
			}
		};

		Boolean isLast = executeBockingOperation(uuidRemove, syncFile);
		if (isLast != null && isLast) {
			syncFile.delete();
		}

		if (mainLock != null && lockUuid.equals(uuid)) {
			reset();
			lockFile.delete();
		}
	}
}

