/*
 * Copyright 2017 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/client-java-core
 *
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.epam.reportportal.service;

import com.epam.reportportal.message.HashMarkSeparatedMessageParser;
import com.epam.reportportal.message.MessageParser;
import com.epam.reportportal.message.ReportPortalMessage;
import com.epam.reportportal.message.TypeAwareByteSource;
import com.epam.reportportal.utils.files.ImageConverter;
import com.epam.ta.reportportal.ws.model.BatchSaveOperatingRS;
import com.epam.ta.reportportal.ws.model.Constants;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import com.github.avarabyeu.restendpoint.http.MultiPartRequest;
import com.google.common.base.Strings;
import com.google.common.io.ByteSource;
import com.google.common.net.MediaType;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.subjects.PublishSubject;
import org.reactivestreams.Publisher;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

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
 * @see #init(Maybe, ReportPortalClient)
 */
public class LoggingContext {

    /* default back-pressure buffer size */
    public static final int DEFAULT_BUFFER_SIZE = 10;

    private static final ThreadLocal<LoggingContext> CONTEXT_THREAD_LOCAL = new ThreadLocal<>();
    private static final MessageParser MESSAGE_PARSER = new HashMarkSeparatedMessageParser();

    /**
     * Initializes new logging context and attaches it to current thread
     *
     * @param itemId Test Item ID
     * @param client Client of ReportPortal
     * @return New Logging Context
     */
    public static LoggingContext init(Maybe<String> itemId, final ReportPortalClient client) {
        return init(itemId, client, DEFAULT_BUFFER_SIZE, false);
    }

    /**
     * Initializes new logging context and attaches it to current thread
     *
     * @param itemId        Test Item ID
     * @param client        Client of ReportPortal
     * @param bufferSize    Size of back-pressure buffer
     * @param convertImages Whether Image should be converted to BlackAndWhite
     * @return New Logging Context
     */
    public static LoggingContext init(Maybe<String> itemId, final ReportPortalClient client, int bufferSize,
            boolean convertImages) {
        LoggingContext context = new LoggingContext(itemId, client, bufferSize, convertImages);
        CONTEXT_THREAD_LOCAL.set(context);
        return context;
    }

    /**
     * Emits log message if there is any active context attached to the current thread
     *
     * @param logSupplier Log supplier
     */
    public static void emitLog(com.google.common.base.Function<String, SaveLogRQ> logSupplier) {
        final LoggingContext loggingContext = CONTEXT_THREAD_LOCAL.get();
        if (null != loggingContext) {
            loggingContext.emit(logSupplier);
        }
    }

    /**
     * Emits log message if there is any active context attached to the current thread
     */
    public static void emitLog(final String message, final String level, final Date time) {
        final LoggingContext loggingContext = CONTEXT_THREAD_LOCAL.get();
        if (null != loggingContext) {
            loggingContext.emit(new com.google.common.base.Function<String, SaveLogRQ>() {
                @Nonnull
                @Override
                public SaveLogRQ apply(@Nullable String id) {
                    SaveLogRQ rq = new SaveLogRQ();
                    rq.setLevel(level);
                    rq.setLogTime(time);
                    rq.setTestItemId(id);
                    if (MESSAGE_PARSER.supports(message)) {
                        final ReportPortalMessage rpMessage = MESSAGE_PARSER.parse(message);
                        rq.setMessage(rpMessage.getMessage());
                        final TypeAwareByteSource data = rpMessage.getData();
                        SaveLogRQ.File file = new SaveLogRQ.File();
                        try {
                            file.setContent(
                                    (loggingContext.convertImages ? ImageConverter.convertIfImage(data) : data).read());
                            file.setContentType(data.getMediaType().toString());
                            file.setName(UUID.randomUUID().toString());
                            rq.setFile(file);
                        } catch (IOException e) {
                            // seems like there is some problem. Do not report an file
                        }
                    } else {
                        rq.setMessage(message);
                    }
                    return rq;
                }
            });
        }
    }

    /**
     * Completes context attached to the current thread
     *
     * @return Waiting queue to be able to track request sending completion
     */
    public static Completable complete() {
        final LoggingContext loggingContext = CONTEXT_THREAD_LOCAL.get();
        if (null != loggingContext) {
            return loggingContext.completed();
        } else {
            return Maybe.empty().ignoreElement();
        }
    }

    /* Log emitter */
    private final PublishSubject<Maybe<SaveLogRQ>> emitter;
    /* ID of TestItem in ReportPortal */
    private final Maybe<String> itemId;
    /* Whether Image should be converted to BlackAndWhite */
    private final boolean convertImages;

    LoggingContext(Maybe<String> itemId, final ReportPortalClient client, int bufferSize, boolean convertImages) {
        this.itemId = itemId;
        this.emitter = PublishSubject.create();
        this.convertImages = convertImages;
        emitter.toFlowable(BackpressureStrategy.BUFFER)
                .flatMap(new Function<Maybe<SaveLogRQ>, Publisher<SaveLogRQ>>() {
                    @Override
                    public Publisher<SaveLogRQ> apply(Maybe<SaveLogRQ> rq) throws Exception {
                        return rq.toFlowable();
                    }
                })
                .buffer(bufferSize)
                .flatMap(new Function<List<SaveLogRQ>, Flowable<BatchSaveOperatingRS>>() {
                    @Override
                    public Flowable<BatchSaveOperatingRS> apply(List<SaveLogRQ> rqs) throws Exception {
                        MultiPartRequest.Builder builder = new MultiPartRequest.Builder();

                        builder.addSerializedPart(Constants.LOG_REQUEST_JSON_PART, rqs);

                        for (SaveLogRQ rq : rqs) {
                            final SaveLogRQ.File file = rq.getFile();
                            if (null != file) {
                                builder.addBinaryPart(Constants.LOG_REQUEST_BINARY_PART, file.getName(),
                                        Strings.isNullOrEmpty(file.getContentType()) ?
                                                MediaType.OCTET_STREAM.toString() :
                                                file.getContentType(), ByteSource.wrap(file.getContent()));
                            }
                        }
                        return client.log(builder.build()).toFlowable();
                    }
                })
                .doOnError(new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                })
                .subscribe();

    }

    public void emit(final com.google.common.base.Function<String, SaveLogRQ> logSupplier) {
        emitter.onNext(itemId.map(new Function<String, SaveLogRQ>() {
            @Override
            public SaveLogRQ apply(String input) throws Exception {
                return logSupplier.apply(input);
            }
        }));

    }

    public Completable completed() {
        emitter.onComplete();
        return emitter.ignoreElements();
    }

}
