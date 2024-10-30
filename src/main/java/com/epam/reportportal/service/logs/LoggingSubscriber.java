/*
 *  Copyright 2022 EPAM Systems
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
package com.epam.reportportal.service.logs;

import com.epam.ta.reportportal.ws.model.BatchSaveOperatingRS;
import io.reactivex.FlowableSubscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * Set of logging subscription for ReportPortal logging client
 */
public final class LoggingSubscriber implements FlowableSubscriber<BatchSaveOperatingRS> {
	private static final Logger LOGGER = LoggerFactory.getLogger(LoggingSubscriber.class);

	@Override
	public void onSubscribe(@Nonnull Subscription s) {
		// ignore
	}

	@Override
	public void onNext(BatchSaveOperatingRS result) {
		// ignore
	}

	@Override
	public void onError(Throwable e) {
		LOGGER.error("[{}] ReportPortal logging error", Thread.currentThread().getId(), e);
	}

	@Override
	public void onComplete() {
		// ignore
	}
}
