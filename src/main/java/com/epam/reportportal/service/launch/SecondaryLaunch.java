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
import com.epam.reportportal.service.LoggingContext;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.utils.Waiter;
import com.epam.ta.reportportal.ws.model.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.model.launch.LaunchResource;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.disposables.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * An implementation of a {@link Launch} object which didn't manage to obtain main lock with {@link LaunchIdLock}
 * object. Therefore, it does not create an actual launch ReportPortal, but using provided Launch UUID. It also does
 * not actually finish the launch on ReportPortal, but just waits for graceful items upload.
 */
public class SecondaryLaunch extends AbstractJoinedLaunch {
	private static final Logger LOGGER = LoggerFactory.getLogger(SecondaryLaunch.class);

	private final ReportPortalClient client;

	public SecondaryLaunch(ReportPortalClient rpClient, ListenerParameters parameters, Maybe<String> launch,
			ExecutorService executorService, LaunchIdLock launchIdLock, String instanceUuid) {
		super(rpClient, parameters, launch, executorService, launchIdLock, instanceUuid);
		client = rpClient;
	}

	private void waitForLaunchStart() {
		new Waiter("Wait for Launch start").pollingEvery(1, TimeUnit.SECONDS)
				.duration(getParameters().getClientJoinLaunchTimeout(), TimeUnit.MILLISECONDS)
				.timeoutFail()
				.till(new Callable<Boolean>() {
					private volatile Boolean result = null;
					private final Queue<Disposable> disposables = new ConcurrentLinkedQueue<>();

					@Override
					public Boolean call() {
						if (result == null) {
							disposables.add(launch.subscribe(uuid -> {
								Maybe<LaunchResource> maybeRs = client.getLaunchByUuid(uuid);
								if (maybeRs != null) {
									disposables.add(maybeRs.subscribe(
											launchResource -> result = Boolean.TRUE,
											throwable -> LOGGER.debug(
													"Unable to get a Launch: {}",
													throwable.getLocalizedMessage(),
													throwable
											)
									));
								} else {
									LOGGER.debug("RP Client returned 'null' response on get Launch by UUID call");
								}
							}));
						} else {
							Disposable disposable;
							while ((disposable = disposables.poll()) != null) {
								disposable.dispose();
							}
						}
						return result;
					}
				});
	}

	@Nonnull
	@Override
	public Maybe<String> start() {
		if (!getParameters().isAsyncReporting()) {
			waitForLaunchStart();
		}
		return super.start(false);
	}

	@Override
	public void finish(final FinishExecutionRQ request) {
		// Finish virtual items and logs
		waitForItemsCompletion();

		// Wait for all items to be finished
		waitForCompletable(Completable.concat(queue.getOrCompute(this.launch).getChildren()));

		// ignore super call, since only primary launch should finish it
		stopRunning();
		lock.finishInstanceUuid(uuid);

		// Dispose all collected virtual item disposables
		virtualItemDisposables.removeIf(d -> {
			d.dispose();
			return true;
		});
		// Dispose all logged items
		LoggingContext.dispose();
	}
}
