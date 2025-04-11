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
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Flowable;
import io.reactivex.internal.fuseable.HasUpstreamPublisher;
import io.reactivex.subscribers.SerializedSubscriber;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

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
		source.subscribe(new BufferSubscriber(new SerializedSubscriber<>(s), maxSize, payloadLimit));
	}

	@Override
	public Publisher<SaveLogRQ> source() {
		return source;
	}
}
