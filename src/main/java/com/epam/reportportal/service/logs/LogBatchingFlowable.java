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

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.utils.http.HttpRequestUtils;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Flowable;
import io.reactivex.FlowableSubscriber;
import io.reactivex.internal.fuseable.HasUpstreamPublisher;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subscribers.SerializedSubscriber;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * A flowable which compile {@link SaveLogRQ} messages into specific batches limited by the number of entities in the batch and estimated
 * payload size.
 */
public class LogBatchingFlowable extends Flowable<List<SaveLogRQ>> implements HasUpstreamPublisher<SaveLogRQ> {

	private final int maxSize;
	private final long payloadLimit;

	private final Flowable<SaveLogRQ> source;

	public LogBatchingFlowable(Flowable<SaveLogRQ> flowableSource, ListenerParameters parameters) {
		source = flowableSource;
		maxSize = parameters.getBatchLogsSize();
		payloadLimit = parameters.getBatchPayloadLimit();
	}

	@Override
	protected void subscribeActual(Subscriber<? super List<SaveLogRQ>> s) {
		source.subscribe(new LogBatchingFlowable.BufferSubscriber(new SerializedSubscriber<>(s), maxSize, payloadLimit));
	}

	@Override
	public Publisher<SaveLogRQ> source() {
		return source;
	}

	private static final class BufferSubscriber implements FlowableSubscriber<SaveLogRQ>, Subscription {
		private final Subscriber<List<SaveLogRQ>> downstream;
		private final int maxSize;
		private final long payloadLimit;

		private List<SaveLogRQ> buffer;
		private long payloadSize;
		private Subscription upstream;
		boolean done;

		public BufferSubscriber(Subscriber<List<SaveLogRQ>> actual, int batchMaxSize, long batchPayloadLimit) {
			downstream = actual;
			maxSize = batchMaxSize;
			payloadLimit = batchPayloadLimit;
		}

		@Override
		public void onSubscribe(@Nonnull Subscription s) {
			if (!SubscriptionHelper.validate(upstream, s)) {
				return;
			}
			upstream = s;
			buffer = new ArrayList<>();
			payloadSize = HttpRequestUtils.TYPICAL_MULTIPART_FOOTER_LENGTH;

			downstream.onSubscribe(this);
		}

		private void reset() {
			buffer = new ArrayList<>();
			payloadSize = HttpRequestUtils.TYPICAL_MULTIPART_FOOTER_LENGTH;
		}

		@Override
		public void onNext(SaveLogRQ t) {
			if (done) {
				return;
			}
			long size = HttpRequestUtils.calculateRequestSize(t);
			List<List<SaveLogRQ>> toSend = new ArrayList<>();
			synchronized (this) {
				if (buffer == null) {
					return;
				}
				if (payloadSize + size > payloadLimit) {
					if (buffer.size() > 0) {
						toSend.add(buffer);
						reset();
					}
				}
				buffer.add(t);
				payloadSize += size;
				if (buffer.size() >= maxSize) {
					toSend.add(buffer);
					reset();
				}
			}
			toSend.forEach(downstream::onNext);
		}

		@Override
		public void onError(Throwable t) {
			if (done) {
				RxJavaPlugins.onError(t);
				return;
			}
			done = true;
			downstream.onError(t);
		}

		@Override
		public void onComplete() {
			if (done) {
				return;
			}
			done = true;

			List<List<SaveLogRQ>> toSend = new ArrayList<>();
			synchronized (this) {
				if (buffer != null && !buffer.isEmpty()) {
					toSend.add(buffer);
					reset();
				}
			}
			toSend.forEach(downstream::onNext);
			downstream.onComplete();
		}

		@Override
		public void request(long n) {
			if (SubscriptionHelper.validate(n)) {
				upstream.request(BackpressureHelper.multiplyCap(n, maxSize));
			}
		}

		@Override
		public void cancel() {
			upstream.cancel();
		}
	}
}
