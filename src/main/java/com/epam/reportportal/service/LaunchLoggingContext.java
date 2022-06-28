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

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.message.TypeAwareByteSource;
import com.epam.reportportal.service.logs.LogBatchingFlowable;
import com.epam.reportportal.utils.http.HttpRequestUtils;
import com.epam.ta.reportportal.ws.model.BatchSaveOperatingRS;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Scheduler;
import io.reactivex.functions.Function;
import io.reactivex.internal.operators.flowable.FlowableFromObservable;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subjects.PublishSubject;
import org.reactivestreams.Publisher;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.epam.reportportal.utils.SubscriptionUtils.logFlowableResults;
import static com.epam.reportportal.utils.files.ImageConverter.convert;
import static com.epam.reportportal.utils.files.ImageConverter.isImage;
import static com.google.common.io.ByteSource.wrap;

/**
 * Logging context holds {@link ConcurrentHashMap} context for launch logging and converts
 * {@link SaveLogRQ} to multipart HTTP request to ReportPortal
 * Basic flow:
 * After start some launch context should be initialized with observable of
 * launch ID and ReportPortal client.
 * Before actual finish of launch, context should be closed/completed.
 * Context consists of {@link Flowable} with buffering back-pressure strategy to be able
 * to batch incoming log messages into one request
 *
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 * @see #init(Maybe, ReportPortalClient, Scheduler)
 */
public class LaunchLoggingContext {
	static final String DEFAULT_LAUNCH_KEY = "default";

	private static final ConcurrentHashMap<String, LaunchLoggingContext> loggingContextMap = new ConcurrentHashMap<>();
	/* Log emitter */
	private final PublishSubject<Maybe<SaveLogRQ>> emitter;
	/* a UUID of Launch in ReportPortal */
	private final Maybe<String> launchUuid;
	/* Whether Image should be converted to BlackAndWhite */
	private final boolean convertImages;

	private LaunchLoggingContext(Maybe<String> launchUuid, final ReportPortalClient client, Scheduler scheduler,
			ListenerParameters parameters) {
		this.launchUuid = launchUuid;
		this.emitter = PublishSubject.create();
		this.convertImages = parameters.isConvertImage();
		RxJavaPlugins.onAssembly(new LogBatchingFlowable(
						new FlowableFromObservable<>(emitter).flatMap((Function<Maybe<SaveLogRQ>, Publisher<SaveLogRQ>>) Maybe::toFlowable),
						parameters
				))
				.flatMap((Function<List<SaveLogRQ>, Flowable<BatchSaveOperatingRS>>) rqs -> client.log(HttpRequestUtils.buildLogMultiPartRequest(
						rqs)).toFlowable())
				.doOnError(Throwable::printStackTrace)
				.observeOn(scheduler)
				.onBackpressureBuffer(parameters.getRxBufferSize(), false, true)
				.subscribe(logFlowableResults("Launch logging context"));
	}

	@Nullable
	public static LaunchLoggingContext context(String key) {
		return loggingContextMap.get(key);
	}

	/**
	 * Initializes new logging context and attaches it to current thread
	 *
	 * @param launchUuid a UUID of a Launch
	 * @param client     Client of ReportPortal
	 * @param scheduler  a {@link Scheduler} to use with this LoggingContext
	 * @param parameters Report Portal client configuration parameters
	 * @return New Logging Context
	 */
	public static LaunchLoggingContext init(Maybe<String> launchUuid, final ReportPortalClient client, Scheduler scheduler,
			ListenerParameters parameters) {
		LaunchLoggingContext context = new LaunchLoggingContext(launchUuid, client, scheduler, parameters);
		loggingContextMap.put(DEFAULT_LAUNCH_KEY, context);
		return context;
	}

	/**
	 * Initializes new logging context and attaches it to current thread
	 *
	 * @param launchUuid Launch UUID
	 * @param client     Client of ReportPortal
	 * @param scheduler  a {@link Scheduler} to use with this LoggingContext
	 * @return New Logging Context
	 */
	static LaunchLoggingContext init(Maybe<String> launchUuid, final ReportPortalClient client, Scheduler scheduler) {
		return init(launchUuid, client, scheduler, LoggingContext.DEFAULT_LOG_BATCH_SIZE, false);
	}

	/**
	 * Initializes new logging context and attaches it to current thread
	 *
	 * @param launchUuid    Launch UUID
	 * @param client        Client of ReportPortal
	 * @param scheduler     a {@link Scheduler} to use with this LoggingContext
	 * @param batchLogsSize Size of a log batch
	 * @param convertImages Whether Image should be converted to BlackAndWhite
	 * @return New Logging Context
	 */
	static LaunchLoggingContext init(Maybe<String> launchUuid, final ReportPortalClient client, Scheduler scheduler, int batchLogsSize,
			boolean convertImages) {
		ListenerParameters params = new ListenerParameters();
		params.setBatchLogsSize(batchLogsSize);
		params.setConvertImage(convertImages);
		LaunchLoggingContext context = new LaunchLoggingContext(launchUuid, client, scheduler, params);
		loggingContextMap.put(DEFAULT_LAUNCH_KEY, context);
		return context;
	}

	/**
	 * Completes context attached to the current thread
	 *
	 * @return Waiting queue to be able to track request sending completion
	 */
	public static Completable complete() {
		final LaunchLoggingContext loggingContext = loggingContextMap.get(DEFAULT_LAUNCH_KEY);
		if (null != loggingContext) {
			return loggingContext.completed();
		} else {
			return Maybe.empty().ignoreElement();
		}
	}

	/**
	 * Emits log. Basically, put it into processing pipeline
	 *
	 * @param logSupplier Log Message Factory. Key if the function is actual test item ID
	 */
	void emit(final java.util.function.Function<String, SaveLogRQ> logSupplier) {
		emitter.onNext(launchUuid.map(input -> {
			final SaveLogRQ rq = logSupplier.apply(input);
			SaveLogRQ.File file = rq.getFile();
			if (convertImages && null != file && isImage(file.getContentType())) {
				final TypeAwareByteSource source = convert(wrap(file.getContent()));
				file.setContent(source.read());
				file.setContentType(source.getMediaType());
			}
			return rq;
		}));
	}

	/**
	 * Marks flow as completed
	 *
	 * @return {@link Completable}
	 */
	private Completable completed() {
		emitter.onComplete();
		return emitter.ignoreElements();
	}
}
