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

import jakarta.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Set of logging subscription for ReportPortal logging client
 */
public class LoggingSubscriber implements FlowableSubscriber<BatchSaveOperatingRS> {
	private static final Logger LOGGER = LoggerFactory.getLogger(LoggingSubscriber.class);

	private final AtomicInteger counter = new AtomicInteger(0);

	@Override
	public void onSubscribe(@Nonnull Subscription s) {
		s.request(Long.MAX_VALUE);
	}

	@Override
	public void onNext(BatchSaveOperatingRS result) {
		counter.incrementAndGet();
	}

	public int getProcessedCount() {
		return counter.get();
	}

	@Override
	public void onError(Throwable e) {
		LOGGER.error("[{}] ReportPortal logging error", Thread.currentThread().getId(), e);
		counter.incrementAndGet();
	}

	@Override
	public void onComplete() {
		// ignore
	}
}
