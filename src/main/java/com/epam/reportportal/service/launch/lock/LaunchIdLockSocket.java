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
import com.epam.reportportal.utils.Waiter;
import com.epam.reportportal.utils.properties.ListenerProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * A service to perform blocking I/O operations on network sockets to get single launch UUID for multiple clients on a machine.
 * This class uses local networking, therefore applicable in scope of a single hardware machine. You can control port number
 * with {@link ListenerProperty#CLIENT_JOIN_LOCK_PORT} property.
 *
 * @author <a href="mailto:vadzim_hushchanskou@epam.com">Vadzim Hushchanskou</a>
 */
public class LaunchIdLockSocket extends AbstractLaunchIdLock implements LaunchIdLock {
	private static final Logger LOGGER = LoggerFactory.getLogger(LaunchIdLockSocket.class);

	public static final Charset TRANSFER_CHARSET = StandardCharsets.ISO_8859_1;
	private static final int SOCKET_BACKLOG = 50;
	private static final String COMMAND_DELIMITER = " - ";
	private static final String OK_SUFFIX = COMMAND_DELIMITER + "OK";
	private static final Map<String, Date> INSTANCES = new ConcurrentHashMap<>();

	private static final ReentrantLock classLevelLock = new ReentrantLock();

	private static volatile ServerSocket mainLock;
	private static volatile String lockUuid;
	private volatile ServerHandler handler;

	private final int portNumber;
	private final long instanceWaitTimeout;

	/**
	 * Internal supported communication commands. Should be exactly 6 characters long.
	 */
	enum Command {
		UPDATE,
		FINISH
	}

	private static class ServerHandler extends Thread {

		private final Random random = new Random();
		private final Queue<Socket> workSockets = new LinkedList<>();
		private volatile boolean running = true;

		public ServerHandler() {
			setDaemon(true);
			setName("rp-launch-join");
		}

		@Override
		public void run() {

			while (running) {
				try {
					Socket s = mainLock.accept();
					workSockets.add(s);
					OutputStream os = s.getOutputStream();
					byte[] launchUuid = lockUuid.getBytes(TRANSFER_CHARSET);
					os.write(launchUuid);
					os.flush();
					byte[] updateUuid = new byte[(Command.UPDATE.name() + COMMAND_DELIMITER + lockUuid).getBytes(
							TRANSFER_CHARSET).length];
					InputStream is = s.getInputStream();
					//noinspection ResultOfMethodCallIgnored
					is.read(updateUuid);
					String data = new String(updateUuid, TRANSFER_CHARSET);
					final Command command = Command.valueOf(data.substring(0, data.indexOf(COMMAND_DELIMITER)));
					final String instanceUuid = data.substring(
							data.indexOf(COMMAND_DELIMITER) + COMMAND_DELIMITER.length());
					switch (command) {
						case UPDATE:
							INSTANCES.put(instanceUuid, new Date());
							break;
						case FINISH:
							INSTANCES.remove(instanceUuid);
							break;
					}
					String answer = instanceUuid + OK_SUFFIX;
					byte[] answerBuffer = answer.getBytes(TRANSFER_CHARSET);
					os.write(answerBuffer);
					os.flush();
					if (random.nextInt(5) == 0) {
						Collection<Socket> checked = new LinkedList<>();
						Socket current;
						while ((current = workSockets.poll()) != null) {
							if (!current.isClosed()) {
								checked.add(current);
							}
						}
						workSockets.addAll(checked);
					}
				} catch (IOException e) {
					LOGGER.warn("Error serving server connections: ", e);
				}
			}

			Socket current;
			while ((current = workSockets.poll()) != null) {
				if (!current.isClosed()) {
					try {
						current.close();
					} catch (IOException e) {
						LOGGER.warn("Unable to close socket properly", e);
					}
				}
			}
		}
	}

	public LaunchIdLockSocket(ListenerParameters listenerParameters) {
		super(listenerParameters);
		portNumber = listenerParameters.getLockPortNumber();
		instanceWaitTimeout = listenerParameters.getLockWaitTimeout();
	}

	String sendCommand(@Nonnull final Command command, @Nonnull final String instanceUuid) {
		String result = new Waiter("Wait for a socket connection").duration(instanceWaitTimeout, TimeUnit.MILLISECONDS)
				.applyRandomDiscrepancy(MAX_WAIT_TIME_DISCREPANCY)
				.pollingEvery(1, TimeUnit.SECONDS)
				.till(() -> {
					try (Socket socket = new Socket(InetAddress.getLocalHost(), portNumber)) {
						byte[] launchAnswerBuffer = new byte[instanceUuid.getBytes(TRANSFER_CHARSET).length];
						InputStream is = socket.getInputStream();
						//noinspection ResultOfMethodCallIgnored
						is.read(launchAnswerBuffer);
						String launchUuid = new String(launchAnswerBuffer, TRANSFER_CHARSET);
						byte[] saveBuffer = (command.name() + COMMAND_DELIMITER + instanceUuid).getBytes(
								TRANSFER_CHARSET);
						OutputStream os = socket.getOutputStream();
						os.write(saveBuffer);
						os.flush();
						String expectedAnswer = instanceUuid + OK_SUFFIX;
						byte[] answerBuffer = new byte[expectedAnswer.getBytes(TRANSFER_CHARSET).length];
						//noinspection ResultOfMethodCallIgnored
						is.read(answerBuffer);
						String answer = new String(answerBuffer, TRANSFER_CHARSET);
						if (!expectedAnswer.equals(answer)) {
							LOGGER.warn("Invalid server instance UUID '{}' answer", command.name());
							return null;
						}
						return launchUuid;
					} catch (IOException e) {
						LOGGER.warn(
								"Unable to '{}' instance UUID on port '{}', connection error",
								command.name(),
								portNumber,
								e
						);
						return null;
					}
				});

		return result == null ? instanceUuid : result;
	}

	private String executeCommand(@Nonnull final Command command, @Nonnull final String instanceUuid) {
		if (mainLock != null) {
			switch (command) {
				case UPDATE:
					INSTANCES.put(instanceUuid, new Date());
					break;
				case FINISH:
					INSTANCES.remove(instanceUuid);
					break;
			}
			return lockUuid;
		}

		return sendCommand(command, instanceUuid);
	}

	private String writeInstanceUuid(@Nonnull final String instanceUuid) {
		return executeCommand(Command.UPDATE, instanceUuid);
	}

	/**
	 * Returns a Launch UUID for many Clients launched on one machine.
	 *
	 * @param uuid a Client instance UUID, which will be written to lock and sync files and, if it the first thread which managed to
	 *             obtain lock on '.lock' file, returned to every client instance.
	 * @return either a Client instance UUID, either the first UUID which thread managed to place a lock on a '.lock' file.
	 */
	@Override
	public String obtainLaunchUuid(@Nonnull final String uuid) {
		Objects.requireNonNull(uuid);
		if (mainLock == null) {
			classLevelLock.lock();
			try {
				if (mainLock == null) {
					mainLock = new ServerSocket(portNumber, SOCKET_BACKLOG, InetAddress.getLocalHost());
					lockUuid = uuid;
					INSTANCES.put(uuid, new Date());
				}
				classLevelLock.unlock();
			} catch (IOException e) {
				// already busy, try to connect
				classLevelLock.unlock();
				LOGGER.debug("Unable to obtain lock socket", e);
			}
			if (uuid.equals(lockUuid)) {
				// This is the main thread, serve clients
				handler = new ServerHandler();
				handler.start();
			} else {
				// Another thread acquired lock while synchronization wait
				return writeInstanceUuid(uuid);
			}
		} else {
			if (!uuid.equals(lockUuid)) {
				writeInstanceUuid(uuid);
			}
		}
		return lockUuid;
	}

	@Override
	public void updateInstanceUuid(@Nonnull final String instanceUuid) {
		writeInstanceUuid(instanceUuid);
	}

	void reset() {
		if (handler != null) {
			handler.running = false;
			handler = null;
		}
		lockUuid = null;
		INSTANCES.clear();
		if (mainLock != null) {
			ServerSocket socket = mainLock;
			mainLock = null; // faster than closing connection
			try {
				socket.close();
			} catch (IOException e) {
				LOGGER.warn("Unable to close server socket properly", e);
			}
		}
	}

	/**
	 * Remove self UUID from sync file, means that a client finished its Launch. If this is the last UUID in the sync file, lock and sync
	 * files will be removed.
	 *
	 * @param instanceUuid a Client instance UUID.
	 */
	@Override
	public void finishInstanceUuid(@Nonnull final String instanceUuid) {
		executeCommand(Command.FINISH, instanceUuid);
		if (mainLock != null) {
			if (instanceUuid.equals(lockUuid)) {
				classLevelLock.lock();
				if (mainLock != null) {
					reset();
				}
				classLevelLock.unlock();
			}
		}
	}

	@Nonnull
	@Override
	public Collection<String> getLiveInstanceUuids() {
		Calendar calendar = Calendar.getInstance();
		calendar.add(
				Calendar.MILLISECOND,
				-instanceWaitTimeout < Integer.MIN_VALUE ? Integer.MIN_VALUE : (int) -instanceWaitTimeout
		);
		Date timeoutTime = calendar.getTime();
		return INSTANCES.entrySet()
				.stream()
				.filter(e -> e.getValue().after(timeoutTime))
				.map(Map.Entry::getKey)
				.collect(Collectors.toList());
	}
}
