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
import com.epam.reportportal.utils.properties.ListenerProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;

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
	private static final float MAX_WAIT_TIME_DISCREPANCY = 0.1f;

	private final int portNumber;
	private volatile String lockUuid;

	public LaunchIdLockSocket(ListenerParameters listenerParameters) {
		super(listenerParameters);
		portNumber = listenerParameters.getLockPortNumber();
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
		return uuid;
	}

	/**
	 * Remove self UUID from sync file, means that a client finished its Launch. If this is the last UUID in the sync file, lock and sync
	 * files will be removed.
	 *
	 * @param uuid a Client instance UUID.
	 */
	@Override
	public void finishInstanceUuid(final String uuid) {

	}

	@Override
	public Collection<String> getLiveInstanceUuids() {
		return Collections.emptyList();
	}
}
