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

package com.epam.reportportal.service;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * A service to perform blocking operation to get single launch UUID for multiple clients.
 *
 * @author <a href="mailto:vadzim_hushchanskou@epam.com">Vadzim Hushchanskou</a>
 */
public interface LaunchIdLock {
	float MAX_WAIT_TIME_DISCREPANCY = 0.1f;

	/**
	 * Returns a Launch UUID for many clients.
	 *
	 * @param instanceUuid a Client instance UUID, which will be used to identify a Client and a Launch. If it the first one UUID passed to
	 *                     the method it will be returned to every other client instance.
	 * @return either instanceUuid, if the process managed obtain the lock and become the primary Launch, either the primary Launch UUID,
	 * or null in case of execution errors
	 */
	@Nullable
	String obtainLaunchUuid(@Nonnull final String instanceUuid);

	/**
	 * Update self UUID in a lock, means that the Client still executing its Launch.
	 *
	 * @param instanceUuid instanceUuid a Client instance UUID
	 */
	void updateInstanceUuid(@Nonnull final String instanceUuid);

	/**
	 * Remove self UUID from a lock, means that a Client finished its Launch.
	 *
	 * @param instanceUuid a Client instance UUID
	 */
	void finishInstanceUuid(@Nonnull final String instanceUuid);

	/**
	 * Return all instance UUIDs which are still running.
	 *
	 * @return a collection of live instance UUIDs
	 */
	@Nonnull
	default Collection<String> getLiveInstanceUuids() {
		return Collections.emptyList();
	}
}
