/*
 * Copyright 2019 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.reportportal.service;

import com.epam.reportportal.message.TypeAwareByteSource;
import com.epam.reportportal.utils.http.HttpRequestUtils;
import com.epam.ta.reportportal.ws.model.BatchSaveOperatingRS;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import com.google.common.io.ByteSource;
import io.reactivex.*;
import io.reactivex.functions.Function;
import io.reactivex.subjects.PublishSubject;
import org.reactivestreams.Publisher;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import static com.epam.reportportal.service.LoggingCallback.LOG_ERROR;
import static com.epam.reportportal.utils.SubscriptionUtils.logFlowableResults;
import static com.epam.reportportal.utils.files.ImageConverter.convert;
import static com.epam.reportportal.utils.files.ImageConverter.isImage;

/**
 * Logging context holds thread-local context for logging and converts
 * {@link SaveLogRQ} to multipart HTTP request to ReportPortal
 * Basic flow:
 * After start some test item (suite/test/step) context should be initialized with observable of
 * item ID and ReportPortal client.
 * Before actual finish of test item, context should be closed/completed.
 * Context consists of {@link Flowable} with buffering back-pressure strategy to be able
 * to batch incoming log messages into one request
 *
 * @author Andrei Varabyeu
 * @see LoggingContext#init(Maybe, Maybe, ReportPortalClient, Scheduler)
 */
public class LoggingContext {

	/* default back-pressure buffer size */
	public static final int DEFAULT_BUFFER_SIZE = 10;

	static final ThreadLocal<Deque<LoggingContext>> CONTEXT_THREAD_LOCAL = ThreadLocal.withInitial(ArrayDeque::new);

	/**
	 * Initializes new logging context and attaches it to current thread
	 *
	 * @param launchUuid a UUID of a Launch
	 * @param itemUuid   a Test Item UUID
	 * @param client     Client of ReportPortal
	 * @param scheduler  a {@link Scheduler} to use with this LoggingContext
	 * @return New Logging Context
	 */
	public static LoggingContext init(Maybe<String> launchUuid, Maybe<String> itemUuid, final ReportPortalClient client,
			Scheduler scheduler) {
		return init(launchUuid, itemUuid, client, scheduler, DEFAULT_BUFFER_SIZE, false);
	}

	/**
	 * Initializes new logging context and attaches it to current thread
	 *
	 * @param launchUuid    a UUID of a Launch
	 * @param itemUuid      a Test Item UUID
	 * @param client        Client of ReportPortal
	 * @param scheduler     a {@link Scheduler} to use with this LoggingContext
	 * @param bufferSize    Size of back-pressure buffer
	 * @param convertImages Whether Image should be converted to BlackAndWhite
	 * @return New Logging Context
	 */
	public static LoggingContext init(Maybe<String> launchUuid, Maybe<String> itemUuid, final ReportPortalClient client,
			Scheduler scheduler, int bufferSize, boolean convertImages) {
		LoggingContext context = new LoggingContext(launchUuid, itemUuid, client, scheduler, bufferSize, convertImages);
		CONTEXT_THREAD_LOCAL.get().push(context);
		return context;
	}

	/**
	 * Completes context attached to the current thread
	 *
	 * @return Waiting queue to be able to track request sending completion
	 */
	public static Completable complete() {
		final LoggingContext loggingContext = CONTEXT_THREAD_LOCAL.get().poll();
		if (null != loggingContext) {
			return loggingContext.completed();
		} else {
			return Maybe.empty().ignoreElement();
		}
	}

	/* Log emitter */
	private final PublishSubject<Maybe<SaveLogRQ>> emitter;
	/* a UUID of Launch in ReportPortal */
	private final Maybe<String> launchUuid;
	/* a UUID of TestItem in ReportPortal to report into */
	private final Maybe<String> itemUuid;
	/* Whether Image should be converted to BlackAndWhite */
	private final boolean convertImages;

	LoggingContext(Maybe<String> launchUuid, Maybe<String> itemUuid, final ReportPortalClient client, Scheduler scheduler, int bufferSize,
			boolean convertImages) {
		this.launchUuid = launchUuid;
		this.itemUuid = itemUuid;
		this.emitter = PublishSubject.create();
		this.convertImages = convertImages;
		// TODO: fix
//		emitter.toFlowable(BackpressureStrategy.BUFFER)
//				.flatMap((Function<Maybe<SaveLogRQ>, Publisher<SaveLogRQ>>) Maybe::toFlowable)
//				.buffer(bufferSize)
//				.flatMap((Function<List<SaveLogRQ>, Flowable<BatchSaveOperatingRS>>) rqs -> client.log(HttpRequestUtils.buildLogMultiPartRequest(
//						rqs)).toFlowable())
//				.doOnError(LOG_ERROR)
//				.observeOn(scheduler)
//				.subscribe(logFlowableResults("Logging context"));

	}

	private SaveLogRQ prepareRequest(String launchId, String itemId, final java.util.function.Function<String, SaveLogRQ> logSupplier)
			throws IOException {
		final SaveLogRQ rq = logSupplier.apply(itemId);
		rq.setLaunchUuid(launchId);
		SaveLogRQ.File file = rq.getFile();
		if (convertImages && null != file && isImage(file.getContentType())) {
			final TypeAwareByteSource source = convert(ByteSource.wrap(file.getContent()));
			file.setContent(source.read());
			file.setContentType(source.getMediaType());
		}
		return rq;
	}

	/**
	 * Emits log. Basically, put it into processing pipeline
	 *
	 * @param logSupplier Log Message Factory. Key if the function is actual test item ID
	 */
	public void emit(final java.util.function.Function<String, SaveLogRQ> logSupplier) {
		emitter.onNext(launchUuid.zipWith(itemUuid, (launchId, itemId) -> prepareRequest(launchId, itemId, logSupplier)));
	}

	/**
	 * Emits log. Basically, put it into processing pipeline
	 *
	 * @param logItemUuid Test Item ID promise
	 * @param logSupplier Log Message Factory. Key if the function is actual test item ID
	 */
	public void emit(final Maybe<String> logItemUuid, final java.util.function.Function<String, SaveLogRQ> logSupplier) {
		emitter.onNext(launchUuid.zipWith(logItemUuid, (launchId, itemId) -> prepareRequest(launchId, itemId, logSupplier)));
	}

	/**
	 * Marks flow as completed
	 *
	 * @return {@link Completable}
	 */
	public Completable completed() {
		emitter.onComplete();
		return emitter.ignoreElements();
	}

}
