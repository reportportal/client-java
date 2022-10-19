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
import com.epam.ta.reportportal.ws.model.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;

import java.util.Calendar;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static com.epam.reportportal.utils.ObjectUtils.clonePojo;
import static java.util.Optional.ofNullable;

/**
 * An implementation of a {@link Launch} object which managed to obtain main lock with {@link LaunchIdLock} object.
 * Therefore, it is responsible for an actual launch creation on Report Portal and contains logic to wait secondary
 * launches.
 */
public class PrimaryLaunch extends AbstractJoinedLaunch {
	public PrimaryLaunch(ReportPortalClient rpClient, ListenerParameters parameters, StartLaunchRQ launch,
			ExecutorService executorService, LaunchIdLock launchIdLock, String instanceUuid) {
		super(rpClient, parameters, launch, executorService, launchIdLock, instanceUuid);
	}

	@Override
	public void finish(final FinishExecutionRQ request) {
		stopRunning();

		// Secondary launch wait finish condition
		Callable<Boolean> finishCondition = new Callable<Boolean>() {
			private volatile Collection<String> launches;

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
		};

		Boolean finished = Boolean.FALSE;
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
