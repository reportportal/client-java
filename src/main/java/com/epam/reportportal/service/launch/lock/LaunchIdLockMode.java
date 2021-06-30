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

import javax.annotation.Nonnull;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Enumeration of all possible Launch ID locking modes. The class provides a method for {@link LaunchIdLock} service instantiation.
 */
public enum LaunchIdLockMode {
	FILE(LaunchIdLockFile.class),
	SOCKET(LaunchIdLockSocket.class),
	NONE(LaunchIdLockNone.class);

	private final Class<? extends AbstractLaunchIdLock> clazz;

	LaunchIdLockMode(Class<? extends AbstractLaunchIdLock> launchIdLock) {
		clazz = launchIdLock;
	}

	@Nonnull
	public LaunchIdLock getInstance(@Nonnull final ListenerParameters parameters) {
		try {
			Constructor<? extends LaunchIdLock> constructor = clazz.getConstructor(ListenerParameters.class);
			try {
				return constructor.newInstance(parameters);
			} catch (InvocationTargetException | InstantiationException | IllegalAccessException instantiationException) {
				throw new IllegalStateException("Unable to instantiate class", instantiationException);
			}
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException("Unable to locate suitable constructor", e);
		}
	}
}
