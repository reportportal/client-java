/*
 *  Copyright 2025 EPAM Systems
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
import io.reactivex.Completable;
import io.reactivex.FlowableSubscriber;
import io.reactivex.subjects.CompletableSubject;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A logging subscriber that tracks the completion of log processing.
 * This subscriber allows waiting for all log batches to be processed before completing.
 * It can also delegate to another subscriber for compatibility.
 */
public final class TrackingLoggingSubscriber implements FlowableSubscriber<BatchSaveOperatingRS> {
	private static final Logger LOGGER = LoggerFactory.getLogger(TrackingLoggingSubscriber.class);

	private final AtomicInteger pendingBatches = new AtomicInteger(0);
	private final AtomicReference<CompletableSubject> completionSubject = new AtomicReference<>(CompletableSubject.create());
	private final FlowableSubscriber<BatchSaveOperatingRS> delegate;
	private volatile boolean completed = false;

	/**
	 * Creates a tracking subscriber that logs errors but doesn't delegate.
	 */
	public TrackingLoggingSubscriber() {
		this(null);
	}

	/**
	 * Creates a tracking subscriber that delegates to another subscriber.
	 *
	 * @param delegate The subscriber to delegate to, can be null
	 */
	public TrackingLoggingSubscriber(@Nullable FlowableSubscriber<BatchSaveOperatingRS> delegate) {
		this.delegate = delegate;
	}

	@Override
	public void onSubscribe(@Nonnull Subscription s) {
		// Request unlimited items to ensure all batches are processed
		s.request(Long.MAX_VALUE);
		if (delegate != null) {
			delegate.onSubscribe(s);
		}
	}

	@Override
	public void onNext(BatchSaveOperatingRS result) {
		// Decrement pending batches when we receive a batch response
		int remaining = pendingBatches.decrementAndGet();
		if (remaining < 0) {
			LOGGER.warn(
					"[{}] Pending batches counter went negative: {}. This indicates a bug in batch tracking.",
					Thread.currentThread().getId(),
					remaining
			);
			// Reset to 0 to prevent issues
			pendingBatches.set(0);
		}
		checkCompletion();
		if (delegate != null) {
			delegate.onNext(result);
		}
	}

	@Override
	public void onError(Throwable e) {
		LOGGER.error("[{}] ReportPortal logging error", Thread.currentThread().getId(), e);
		// Complete with error
		CompletableSubject subject = completionSubject.get();
		if (subject != null && !subject.hasComplete() && !subject.hasThrowable()) {
			subject.onError(e);
		}
		if (delegate != null) {
			delegate.onError(e);
		}
	}

	@Override
	public void onComplete() {
		completed = true;
		checkCompletion();
		if (delegate != null) {
			delegate.onComplete();
		}
	}

	/**
	 * Notifies that a log batch is being processed.
	 * This should be called when a batch is sent to the network layer.
	 */
	public void onBatchProcessing() {
		int pending = pendingBatches.incrementAndGet();
		LOGGER.debug("[{}] Log batch processing started. Pending batches: {}", Thread.currentThread().getId(), pending);
	}

	/**
	 * Returns a Completable that completes when all log processing is finished.
	 * This includes both the completion of the log stream and all pending batch requests.
	 *
	 * @return A Completable that signals when all logging is complete
	 */
	@Nonnull
	public Completable getCompletion() {
		return completionSubject.get();
	}

	/**
	 * Checks if all conditions for completion are met and completes the subject if so.
	 * This method handles the race condition between onComplete() and the last onNext() calls.
	 * <p>
	 * Race condition scenario:
	 * 1. Log stream emits completion (onComplete called, completed = true)
	 * 2. But there are still pending HTTP batch requests
	 * 3. When those HTTP responses arrive (onNext called), we need to check if we can now complete
	 * <p>
	 * Alternative scenario:
	 * 1. All HTTP responses arrive first (pendingBatches = 0)
	 * 2. Then log stream completes (onComplete called)
	 * 3. We need to complete immediately since all conditions are met
	 */
	private void checkCompletion() {
		// Both conditions must be true: stream completed AND no pending batches
		if (completed && pendingBatches.get() == 0) {
			CompletableSubject subject = completionSubject.get();
			if (subject != null && !subject.hasComplete() && !subject.hasThrowable()) {
				subject.onComplete();
			}
		}
	}
} 