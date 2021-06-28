/*
 *  Copyright 2020 EPAM Systems
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

package com.epam.reportportal.service.launch;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.LaunchImpl;
import com.epam.reportportal.service.LaunchIdLockFile;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.ta.reportportal.ws.model.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The class represents a {@link Launch} which starts and reports into its own one.
 */
public class PrimaryLaunch extends LaunchImpl {
	private final LaunchIdLockFile launchIdLockFile;
	private final AtomicReference<String> instanceUuid;

	public PrimaryLaunch(ReportPortalClient rpClient, ListenerParameters parameters, StartLaunchRQ launch, ExecutorService executorService,
			LaunchIdLockFile launchIdLockFile, AtomicReference<String> instanceUuid) {
		super(rpClient, parameters, launch, executorService);
		this.launchIdLockFile = launchIdLockFile;
		this.instanceUuid = instanceUuid;
	}

	@Override
	public void finish(final FinishExecutionRQ rq) {
		try {
			super.finish(rq);
		} finally {
			launchIdLockFile.finishInstanceUuid(instanceUuid.get());
			instanceUuid.set(UUID.randomUUID().toString());
		}
	}
}
