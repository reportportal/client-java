/*
 * Copyright 2016 EPAM Systems
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

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.message.ReportPortalMessage;
import com.epam.reportportal.message.TypeAwareByteSource;
import com.epam.ta.reportportal.ws.model.EntryCreatedRS;
import com.epam.ta.reportportal.ws.model.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.MaybeSource;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static com.epam.reportportal.service.LoggingCallback.LOG_ERROR;
import static com.epam.reportportal.service.LoggingCallback.LOG_SUCCESS;
import static com.epam.reportportal.service.LoggingCallback.logCreated;
import static com.epam.reportportal.utils.MimeTypeDetector.detect;
import static com.google.common.io.Files.toByteArray;

/**
 * Default ReportPortal Reporter implementation. Uses
 * {@link com.github.avarabyeu.restendpoint.http.RestEndpoint} as REST WS Client
 *
 * @author Andrei Varabyeu
 */
public class ReportPortal {

    static final Logger LOGGER = LoggerFactory.getLogger(ReportPortal.class);

    private static final Function<EntryCreatedRS, String> TO_ID = new Function<EntryCreatedRS, String>() {
        @Override
        public String apply(EntryCreatedRS rs) throws Exception {
            return rs.getId();
        }
    };

    /**
     * REST Client
     */
    private final ReportPortalClient rpClient;
    private final ListenerParameters parameters;

    /**
     * Messages queue to track items execution order
     */
    private final LoadingCache<Maybe<String>, TreeItem> QUEUE = CacheBuilder.newBuilder().build(
            new CacheLoader<Maybe<String>, TreeItem>() {
                @Override
                public TreeItem load(Maybe<String> key) throws Exception {
                    return new TreeItem();
                }
            });

    private Maybe<String> launch;

    private ReportPortal(ReportPortalClient rpClient, ListenerParameters parameters) {
        this.rpClient = Preconditions.checkNotNull(rpClient, "RestEndpoint shouldn't be NULL");
        this.parameters = Preconditions.checkNotNull(parameters, "Parameters shouldn't be NULL");
    }

    public static ReportPortal startLaunch(ReportPortalClient rpClient, ListenerParameters parameters,
            StartLaunchRQ rq) {
        ReportPortal service = new ReportPortal(rpClient, parameters);
        service.startLaunch(rq);
        return service;
    }

    /**
     * Starts launch in ReportPortal
     *
     * @param rq Request Data
     * @return Launch ID promise
     */
    public Maybe<String> startLaunch(StartLaunchRQ rq) {
        this.launch = rpClient.startLaunch(rq)
                .doOnSuccess(logCreated("launch"))
                .doOnError(LOG_ERROR)
                .map(TO_ID).cache();
        this.launch.subscribeOn(Schedulers.io()).subscribe();
        return launch;
    }

    /**
     * Finishes launch in ReportPortal. Blocks until all items are reported correctly
     *
     * @param rq Finish RQ
     */
    public void finishLaunch(final FinishExecutionRQ rq) {
        final Maybe<OperationCompletionRS> finish = Completable
                .concat(QUEUE.getUnchecked(this.launch).children)
                .andThen(this.launch.flatMap(new Function<String, Maybe<OperationCompletionRS>>() {
                    @Override
                    public Maybe<OperationCompletionRS> apply(String id) throws Exception {
                        return rpClient.finishLaunch(id, rq).doOnSuccess(LOG_SUCCESS).doOnError(LOG_ERROR);
                    }
                })).cache();
        try {
            finish.timeout(parameters.getReportingTimeout(), TimeUnit.SECONDS).blockingGet();
        } catch (Exception e) {
            LOGGER.error("Unable to finish launch in ReportPortal", e);
        }
    }

    /**
     * Starts new test item in ReportPortal asynchronously (non-blocking)
     *
     * @param rq Start RQ
     * @return Test Item ID promise
     */
    public Maybe<String> startTestItem(final StartTestItemRQ rq) {
        final Maybe<String> testItem = this.launch.flatMap(new Function<String, Maybe<String>>() {
            @Override
            public Maybe<String> apply(String id) throws Exception {
                rq.setLaunchId(id);
                return rpClient.startTestItem(rq)
                        .doOnSuccess(logCreated("item"))
                        .doOnError(LOG_ERROR)
                        .map(TO_ID);

            }
        }).cache();
        testItem.subscribeOn(Schedulers.io()).subscribe();
        QUEUE.getUnchecked(launch).addToQueue(testItem.ignoreElement());
        return testItem;
    }

    /**
     * Starts new test item in ReportPortal asynchronously (non-blocking)
     *
     * @param rq Start RQ
     * @return Test Item ID promise
     */
    public Maybe<String> startTestItem(final Maybe<String> parentId, final StartTestItemRQ rq) {
        final Maybe<String> itemId = this.launch.flatMap(new Function<String, Maybe<String>>() {
            @Override
            public Maybe<String> apply(final String launchId) throws Exception {
                return parentId.flatMap(new Function<String, MaybeSource<String>>() {
                    @Override
                    public MaybeSource<String> apply(String parentId) throws Exception {
                        rq.setLaunchId(launchId);
                        return rpClient.startTestItem(parentId, rq)
                                .doOnSuccess(logCreated("item"))
                                .doOnError(LOG_ERROR)
                                .map(TO_ID);
                    }
                });
            }
        }).cache();
        itemId.subscribeOn(Schedulers.io()).subscribe();
        QUEUE.getUnchecked(itemId).withParent(parentId).addToQueue(itemId.ignoreElement());
        LoggingContext.init(itemId, this.rpClient, parameters.getBatchLogsSize(), parameters.isConvertImage());
        return itemId;
    }

    /**
     * Finishes Test Item in ReportPortal. Non-blocking. Schedules finish after success of all child items
     *
     * @param itemId Item ID promise
     * @param rq     Finish request
     */
    public void finishTestItem(Maybe<String> itemId, final FinishTestItemRQ rq) {
        Preconditions.checkArgument(null != itemId, "ItemID should not be null");

        QUEUE.getUnchecked(launch).addToQueue(LoggingContext.complete());

        final TreeItem treeItem = QUEUE.getUnchecked(itemId);

        //wait for the children to complete
        final Completable finishCompletion = Completable.concat(treeItem.children)
                .andThen(itemId.flatMap(new Function<String, Maybe<OperationCompletionRS>>() {
                    @Override
                    public Maybe<OperationCompletionRS> apply(String itemId) throws Exception {
                        return rpClient.finishTestItem(itemId, rq)
                                .doOnSuccess(LOG_SUCCESS)
                                .doOnError(LOG_ERROR);
                    }
                }).doAfterSuccess(new Consumer<OperationCompletionRS>() {
                    @Override
                    public void accept(OperationCompletionRS operationCompletionRS) throws Exception {
                        //cleanup children
                        treeItem.freeChildren();
                    }
                })).ignoreElement().cache();
        finishCompletion.subscribeOn(Schedulers.io()).subscribe();
        //find parent and add to its queue
        final Maybe<String> parent = treeItem.parent;
        if (null != parent) {
            QUEUE.getUnchecked(parent).addToQueue(finishCompletion);
        } else {
            //seems like this is root item
            QUEUE.getUnchecked(this.launch).addToQueue(finishCompletion);
        }

    }

    /**
     * Wrapper around TestItem entity to be able to track parent and children items
     */
    static class TreeItem {
        Maybe<String> parent;
        List<Completable> children = new CopyOnWriteArrayList<Completable>();

        TreeItem withParent(Maybe<String> parent) {
            this.parent = parent;
            return this;
        }

        TreeItem addToQueue(Completable completable) {
            this.children.add(completable);
            return this;
        }

        void freeChildren() {
            this.children = null;
        }
    }

    /**
     * Emits log message if there is any active context attached to the current thread
     *
     * @param logSupplier Log supplier. Converts current Item ID to the {@link SaveLogRQ} object
     */
    public static boolean emitLog(com.google.common.base.Function<String, SaveLogRQ> logSupplier) {
        final LoggingContext loggingContext = LoggingContext.CONTEXT_THREAD_LOCAL.get();
        if (null != loggingContext) {
            loggingContext.emit(logSupplier);
            return true;
        }
        return false;
    }

    /**
     * Emits log message if there is any active context attached to the current thread
     */
    public static boolean emitLog(final String message, final String level, final Date time) {
        return emitLog(new com.google.common.base.Function<String, SaveLogRQ>() {
            @Nonnull
            @Override
            public SaveLogRQ apply(@Nullable String id) {
                SaveLogRQ rq = new SaveLogRQ();
                rq.setLevel(level);
                rq.setLogTime(time);
                rq.setTestItemId(id);
                rq.setMessage(message);
                return rq;
            }
        });

    }

    public static boolean emitLog(final String message, final String level, final Date time, final File file) {
        return emitLog(new com.google.common.base.Function<String, SaveLogRQ>() {
            @Nonnull
            @Override
            public SaveLogRQ apply(@Nullable String id) {
                SaveLogRQ rq = new SaveLogRQ();
                rq.setLevel(level);
                rq.setLogTime(time);
                rq.setTestItemId(id);
                rq.setMessage(message);

                try {
                    SaveLogRQ.File f = new SaveLogRQ.File();
                    f.setContentType(detect(file));
                    f.setContent(toByteArray(file));
                } catch (IOException e) {
                    // seems like there is some problem. Do not report an file
                    LOGGER.error("Cannot send file to ReportPortal", e);
                }

                return rq;
            }
        });
    }

    public static boolean emitLog(final ReportPortalMessage message, final String level, final Date time) {
        return emitLog(new com.google.common.base.Function<String, SaveLogRQ>() {
            @Nonnull
            @Override
            public SaveLogRQ apply(@Nullable String id) {
                SaveLogRQ rq = new SaveLogRQ();
                rq.setLevel(level);
                rq.setLogTime(time);
                rq.setTestItemId(id);
                rq.setMessage(message.getMessage());
                try {
                    final TypeAwareByteSource data = message.getData();
                    SaveLogRQ.File file = new SaveLogRQ.File();
                    file.setContent(data.read());

                    file.setContentType(data.getMediaType());
                    file.setName(UUID.randomUUID().toString());
                    rq.setFile(file);

                } catch (Exception e) {
                    // seems like there is some problem. Do not report an file
                    LOGGER.error("Cannot send file to ReportPortal", e);
                }

                return rq;
            }
        });
    }

}
