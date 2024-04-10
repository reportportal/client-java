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
import com.epam.reportportal.service.LaunchIdLock;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.utils.Waiter;
import com.epam.ta.reportportal.ws.reporting.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.reporting.StartLaunchRQ;

import java.util.Calendar;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static com.epam.reportportal.utils.ObjectUtils.clonePojo;

/**
 * An implementation of a {@link Launch} object which managed to obtain main lock with {@link LaunchIdLock} object.
 * Therefore, it is responsible for an actual launch creation on ReportPortal and contains logic to wait secondary
 * launches.
 */
public class PrimaryLaunch extends AbstractJoinedLaunch {
	public PrimaryLaunch(ReportPortalClient rpClient, ListenerParameters parameters, StartLaunchRQ launch,
			ExecutorService executorService, LaunchIdLock launchIdLock, String instanceUuid) {
		super(rpClient, parameters, launch, executorService, launchIdLock, instanceUuid);
	}

	/**
	 * Wait for all secondary launches finish and then close the Primary Launch. If there was running secondary launch
	 * number change then the timeout will reset.
	 *
	 * @param request Finish Launch Request to use (end time will be updated after wait).
	 */
	@Override
	public void finish(final FinishExecutionRQ request) {
		stopRunning();
		Callable<Boolean> finishCondition = new SecondaryLaunchFinishCondition(lock, uuid);
		Boolean finished = Boolean.FALSE;
		// If there was launch number change (finished == false) we will wait more.
		// Only if
		while (finished != Boolean.TRUE && finished != null) {
			Waiter waiter = new Waiter("Wait for all launches end").duration(
					getParameters().getClientJoinTimeout(),
					TimeUnit.MILLISECONDS
			).pollingEvery(1, TimeUnit.SECONDS);
			finished = waiter.till(finishCondition);
		}
		lock.finishInstanceUuid(uuid);
		FinishExecutionRQ rq = clonePojo(request, FinishExecutionRQ.class);
		rq.setEndTime(Calendar.getInstance().getTime());
		super.finish(rq);
	}
}
