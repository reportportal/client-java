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
import com.epam.reportportal.service.*;
import com.epam.reportportal.utils.Waiter;
import com.epam.ta.reportportal.ws.model.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.model.launch.LaunchResource;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.disposables.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The class represents a {@link Launch} which reports into existing one and never starts its own.
 */
public class SecondaryLaunch extends LaunchImpl {
	private static final Logger LOGGER = LoggerFactory.getLogger(SecondaryLaunch.class);

	private final ReportPortalClient rpClient;
	private final LockFile lockFile;
	private final AtomicReference<String> instanceUuid;

	public SecondaryLaunch(ReportPortalClient rpClient, ListenerParameters parameters, Maybe<String> launch,
			ExecutorService executorService, LockFile lockFile, AtomicReference<String> instanceUuid) {
		super(rpClient, parameters, launch, executorService);
		this.rpClient = rpClient;
		this.lockFile = lockFile;
		this.instanceUuid = instanceUuid;
	}

	private void waitForLaunchStart() {
		new Waiter("Wait for Launch start").pollingEvery(1, TimeUnit.SECONDS).timeoutFail().till(new Callable<Boolean>() {
			private volatile Boolean result = null;
			private final Queue<Disposable> disposables = new ConcurrentLinkedQueue<>();

			@Override
			public Boolean call() {
				if (result == null) {
					disposables.add(launch.subscribe(uuid -> {
						Maybe<LaunchResource> maybeRs = rpClient.getLaunchByUuid(uuid);
						if (maybeRs != null) {
							disposables.add(maybeRs.subscribe(
									launchResource -> result = Boolean.TRUE,
									throwable -> LOGGER.debug("Unable to get a Launch: " + throwable.getLocalizedMessage(), throwable)
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

	@Override
	public Maybe<String> start() {
		if (!getParameters().isAsyncReporting()) {
			waitForLaunchStart();
		}
		return super.start();
	}

	@Override
	public void finish(final FinishExecutionRQ rq) {
		QUEUE.getUnchecked(launch).addToQueue(LaunchLoggingContext.complete());
		try {
			Throwable throwable = Completable.concat(QUEUE.getUnchecked(this.launch).getChildren()).
					timeout(getParameters().getReportingTimeout(), TimeUnit.SECONDS).blockingGet();
			if (throwable != null) {
				LOGGER.error("Unable to finish secondary launch in ReportPortal", throwable);
			}
		} finally {
			rpClient.close();
			// ignore that call, since only primary launch should finish it
			lockFile.finishInstanceUuid(instanceUuid.get());
		}
	}
}
