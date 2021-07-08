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

package com.epam.reportportal.service.launch;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.LaunchIdLock;
import com.epam.reportportal.service.LaunchImpl;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.reactivex.Maybe;

import java.util.Random;
import java.util.concurrent.*;

/**
 * The class for common logic for joined launches.
 */
public class AbstractJoinedLaunch extends LaunchImpl {
	final LaunchIdLock lock;
	volatile String uuid;
	private final ScheduledExecutorService updater = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat(
			"rp-poll").setDaemon(true).build());
	private final ScheduledFuture<?> updateTask;

	private static ScheduledFuture<?> getUpdateTask(String instanceUuid, long updateInterval, LaunchIdLock launchIdLock,
			ScheduledExecutorService service) {
		Random r = new Random();
		int delay = updateInterval > Integer.MAX_VALUE ? r.nextInt(Integer.MAX_VALUE) : r.nextInt((int) updateInterval);
		return service.scheduleWithFixedDelay(
				() -> launchIdLock.updateInstanceUuid(instanceUuid),
				delay,
				updateInterval,
				TimeUnit.MILLISECONDS
		);
	}

	public AbstractJoinedLaunch(ReportPortalClient rpClient, ListenerParameters parameters, StartLaunchRQ launch,
			ExecutorService executorService, LaunchIdLock launchIdLock, String instanceUuid) {
		super(rpClient, parameters, launch, executorService);
		lock = launchIdLock;
		uuid = instanceUuid;
		updateTask = getUpdateTask(instanceUuid, parameters.getLockWaitTimeout(), launchIdLock, updater);
	}

	public AbstractJoinedLaunch(ReportPortalClient rpClient, ListenerParameters parameters, Maybe<String> launch,
			ExecutorService executorService, LaunchIdLock launchIdLock, String instanceUuid) {
		super(rpClient, parameters, launch, executorService);
		lock = launchIdLock;
		uuid = instanceUuid;
		updateTask = getUpdateTask(instanceUuid, parameters.getLockWaitTimeout(), launchIdLock, updater);
	}

	void stopRunning() {
		updateTask.cancel(false);
		updater.shutdown();
	}
}
