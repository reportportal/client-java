/*
 * Copyright 2021 EPAM Systems
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

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.message.TypeAwareByteSource;
import com.epam.reportportal.service.logs.LogBatchingFlowable;
import com.epam.reportportal.service.logs.LoggingSubscriber;
import com.epam.reportportal.utils.http.HttpRequestUtils;
import com.epam.ta.reportportal.ws.reporting.BatchSaveOperatingRS;
import com.epam.ta.reportportal.ws.reporting.SaveLogRQ;
import com.epam.reportportal.utils.files.ByteSource;
import io.reactivex.*;
import io.reactivex.functions.Function;
import io.reactivex.internal.operators.flowable.FlowableFromObservable;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subjects.PublishSubject;
import org.apache.commons.lang3.tuple.Pair;
import org.reactivestreams.Publisher;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.epam.reportportal.utils.files.ImageConverter.convert;
import static com.epam.reportportal.utils.files.ImageConverter.isImage;
import static java.util.Optional.ofNullable;

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

	public static final int DEFAULT_LOG_BATCH_SIZE = 10;

	@Deprecated
	public static final int DEFAULT_BUFFER_SIZE = DEFAULT_LOG_BATCH_SIZE;

	private static final ThreadLocal<Pair<Long, Deque<LoggingContext>>> CONTEXT_THREAD_LOCAL = new InheritableThreadLocal<>();

	private static final Set<Long> THREAD_IDS = Collections.newSetFromMap(new ConcurrentHashMap<>());

	@Nonnull
	private static Deque<LoggingContext> createContext() {
		Long threadKey = Thread.currentThread().getId();
		if (!THREAD_IDS.contains(threadKey) || CONTEXT_THREAD_LOCAL.get() == null) {
			Deque<LoggingContext> context = new ArrayDeque<>();
			CONTEXT_THREAD_LOCAL.set(Pair.of(threadKey, context));
			THREAD_IDS.add(threadKey);
			return context;
		}
		return CONTEXT_THREAD_LOCAL.get().getValue();
	}

	@Nullable
	private static Deque<LoggingContext> getContext() {
		Long threadKey = Thread.currentThread().getId();
		return ofNullable(CONTEXT_THREAD_LOCAL.get()).filter(ctx -> threadKey.equals(ctx.getKey())).map(Pair::getValue).orElse(null);
	}

	/**
	 * Return current logging context attached to the current thread. Return parent thread context if self context not found. And return
	 * 'null' if nothing was found at all.
	 *
	 * @return current or parent logging context or 'null'
	 */
	@Nullable
	public static LoggingContext context() {
		return ofNullable(CONTEXT_THREAD_LOCAL.get()).map(Pair::getValue).map(Deque::peek).orElse(null);
	}

	/**
	 * Initializes new logging context and attaches it to current thread
	 *
	 * @param launchUuid        a UUID of a Launch
	 * @param itemUuid          a Test Item UUID
	 * @param client            Client of ReportPortal
	 * @param scheduler         a {@link Scheduler} to use with this LoggingContext
	 * @param parameters        ReportPortal client configuration parameters
	 * @param loggingSubscriber RxJava subscriber on logging results
	 * @return New Logging Context
	 */
	@Nonnull
	public static LoggingContext init(@Nonnull final Maybe<String> launchUuid, @Nullable final Maybe<String> itemUuid,
			@Nonnull final ReportPortalClient client, @Nonnull final Scheduler scheduler, @Nonnull final ListenerParameters parameters,
			@Nonnull final FlowableSubscriber<BatchSaveOperatingRS> loggingSubscriber) {
		LoggingContext context = new LoggingContext(launchUuid, itemUuid, client, scheduler, parameters, loggingSubscriber);
		createContext().push(context);
		return context;
	}

	/**
	 * Initializes new logging context and attaches it to current thread
	 *
	 * @param launchUuid a UUID of a Launch
	 * @param itemUuid   a Test Item UUID
	 * @param client     Client of ReportPortal
	 * @param scheduler  a {@link Scheduler} to use with this LoggingContext
	 * @param parameters ReportPortal client configuration parameters
	 * @return New Logging Context
	 */
	@Nonnull
	public static LoggingContext init(@Nonnull final Maybe<String> launchUuid, @Nullable final Maybe<String> itemUuid,
			@Nonnull final ReportPortalClient client, @Nonnull final Scheduler scheduler, @Nonnull final ListenerParameters parameters) {
		return init(launchUuid, itemUuid, client, scheduler, parameters, new LoggingSubscriber());
	}

	/**
	 * Initializes new logging context and attaches it to current thread
	 *
	 * @param launchUuid    a UUID of a Launch
	 * @param itemUuid      a Test Item UUID
	 * @param client        Client of ReportPortal
	 * @param scheduler     a {@link Scheduler} to use with this LoggingContext
	 * @param batchLogsSize Size of a log batch
	 * @param convertImages Whether Image should be converted to BlackAndWhite
	 * @return New Logging Context
	 */
	@Nonnull
	public static LoggingContext init(@Nonnull final Maybe<String> launchUuid, @Nullable final Maybe<String> itemUuid,
			@Nonnull final ReportPortalClient client, @Nonnull final Scheduler scheduler, final int batchLogsSize,
			final boolean convertImages) {
		ListenerParameters params = new ListenerParameters();
		params.setBatchLogsSize(batchLogsSize);
		params.setConvertImage(convertImages);
		return init(launchUuid, itemUuid, client, scheduler, params);
	}

	/**
	 * Initializes new logging context and attaches it to current thread
	 *
	 * @param launchUuid a UUID of a Launch
	 * @param itemUuid   a Test Item UUID
	 * @param client     Client of ReportPortal
	 * @param scheduler  a {@link Scheduler} to use with this LoggingContext
	 * @return New Logging Context
	 */
	@Nonnull
	public static LoggingContext init(@Nonnull final Maybe<String> launchUuid, @Nullable final Maybe<String> itemUuid,
			@Nonnull final ReportPortalClient client, @Nonnull final Scheduler scheduler) {
		return init(launchUuid, itemUuid, client, scheduler, DEFAULT_LOG_BATCH_SIZE, false);
	}

	/**
	 * Completes context attached to the current thread
	 *
	 * @return Waiting queue to be able to track request sending completion
	 */
	@Nonnull
	public static Completable complete() {
		final LoggingContext loggingContext = ofNullable(getContext()).map(Deque::poll).orElse(null);
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

	LoggingContext(@Nonnull final Maybe<String> launchUuid, @Nullable final Maybe<String> itemUuid,
			@Nonnull final ReportPortalClient client, @Nonnull final Scheduler scheduler, @Nonnull final ListenerParameters parameters,
			@Nonnull final FlowableSubscriber<BatchSaveOperatingRS> loggingSubscriber) {
		this.launchUuid = launchUuid;
		this.itemUuid = itemUuid;
		this.emitter = PublishSubject.create();
		this.convertImages = parameters.isConvertImage();

		RxJavaPlugins.onAssembly(new LogBatchingFlowable(
						new FlowableFromObservable<>(emitter).flatMap((Function<Maybe<SaveLogRQ>, Publisher<SaveLogRQ>>) Maybe::toFlowable),
						parameters
				))
				.flatMap((Function<List<SaveLogRQ>, Flowable<BatchSaveOperatingRS>>) rqs -> client.log(HttpRequestUtils.buildLogMultiPartRequest(
						rqs)).toFlowable())
				.observeOn(scheduler)
				.onBackpressureBuffer(parameters.getRxBufferSize(), false, true)
				.subscribe(loggingSubscriber);
	}

	private SaveLogRQ prepareRequest(@Nonnull final String launchId, @Nullable final String itemId,
			@Nonnull final java.util.function.Function<String, SaveLogRQ> logSupplier) throws IOException {
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
	public void emit(@Nonnull final java.util.function.Function<String, SaveLogRQ> logSupplier) {
		emitter.onNext(launchUuid.zipWith(itemUuid, (launchId, itemId) -> prepareRequest(launchId, itemId, logSupplier)));
	}

	/**
	 * Emits log. Basically, put it into processing pipeline
	 *
	 * @param logItemUuid Test Item ID promise
	 * @param logSupplier Log Message Factory. Key if the function is actual test item ID
	 */
	public void emit(@Nonnull final Maybe<String> logItemUuid, @Nonnull final java.util.function.Function<String, SaveLogRQ> logSupplier) {
		emitter.onNext(launchUuid.zipWith(logItemUuid, (launchId, itemId) -> prepareRequest(launchId, itemId, logSupplier)));
	}

	/**
	 * Marks flow as completed
	 *
	 * @return {@link Completable}
	 */
	@Nonnull
	public Completable completed() {
		emitter.onComplete();
		return emitter.ignoreElements();
	}

}
