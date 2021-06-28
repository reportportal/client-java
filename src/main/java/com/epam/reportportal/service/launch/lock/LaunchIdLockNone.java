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
import com.epam.reportportal.utils.properties.ListenerProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;

/**
 * A dummy service which does nothing when {@link ListenerProperty#CLIENT_JOIN_MODE} set to `NONE` or `false`.
 *
 * @author <a href="mailto:vadzim_hushchanskou@epam.com">Vadzim Hushchanskou</a>
 */
public class LaunchIdLockNone extends AbstractLaunchIdLock implements LaunchIdLock {

	public LaunchIdLockNone(ListenerParameters listenerParameters) {
		super(listenerParameters);
	}

	@Override
	@Nullable
	public String obtainLaunchUuid(@Nonnull final String instanceUuid) {
		return instanceUuid;
	}

	@Override
	public void finishInstanceUuid(@Nonnull final String uuid) {
	}

	@Override
	@Nonnull
	public Collection<String> getLiveInstanceUuids() {
		return Collections.emptyList();
	}
}
