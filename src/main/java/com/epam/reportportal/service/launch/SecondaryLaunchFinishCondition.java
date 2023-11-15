/*
 *  Copyright 2023 EPAM Systems
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.epam.reportportal.service.launch;

import com.epam.reportportal.service.LaunchIdLock;

import java.util.Collection;
import java.util.concurrent.Callable;

import static java.util.Optional.ofNullable;

/**
 * Condition which designed to wait for all secondary launches finished (true), or at least one launch finished (false).
 */
public class SecondaryLaunchFinishCondition implements Callable<Boolean> {
	private volatile Collection<String> launches;
	private final LaunchIdLock lock;
	private final String uuid;

	public SecondaryLaunchFinishCondition(LaunchIdLock launchIdLock, String selfUuid) {
		lock = launchIdLock;
		uuid = selfUuid;
	}

	@Override
	public Boolean call() {
		// Get current live secondary launches, locks `.sync` file
		Collection<String> current = lock.getLiveInstanceUuids();

		// If there is no live launches, or the only live launch is the primary launch we are done
		if (current.isEmpty() || (current.size() == 1 && uuid.equals(current.iterator().next()))) {
			return true;
		}

		// Determine whether there were any updates in secondary launches: new launches started or old one finished
		Boolean changed = ofNullable(launches).map(l -> !l.equals(current)).orElse(Boolean.TRUE);
		launches = current;
		if (changed) {
			// If there were changes in secondary launches than the execution is live, and we are going wait more
			return false;
		}
		// No changes from last time
		return null;
	}
}
