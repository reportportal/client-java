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

import com.epam.ta.reportportal.ws.model.EntryCreatedRS;
import com.epam.ta.reportportal.ws.model.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.MaybeSource;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Default ReportPortal Reporter implementation. Uses
 * {@link com.github.avarabyeu.restendpoint.http.RestEndpoint} as REST WS Client
 *
 * @author Andrei Varabyeu
 */
public class ReportPortal {

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
    private final int logBufferSize;

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

    private ReportPortal(ReportPortalClient rpClient, int logBufferSize) {
        this.rpClient = Preconditions.checkNotNull(rpClient, "RestEndpoint shouldn't be NULL");
        this.logBufferSize = logBufferSize;
    }

    public static ReportPortal startLaunch(ReportPortalClient rpClient, int logBufferSize, StartLaunchRQ rq) {
        ReportPortal service = new ReportPortal(rpClient, logBufferSize);
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
        this.launch = rpClient.startLaunch(rq).map(TO_ID)
                .doOnSuccess(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        System.out.println("Launch created");
                    }
                })
                .doOnError(new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                })
                .cache();
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
                        return rpClient.finishLaunch(id, rq);
                    }
                })).cache();
        finish.blockingGet();
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
                return rpClient.startTestItem(rq).map(TO_ID);

            }
        }).cache();
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
                        return rpClient.startTestItem(parentId, rq).map(TO_ID);
                    }
                });
            }
        }).cache();

        QUEUE.getUnchecked(itemId).withParent(parentId).addToQueue(itemId.ignoreElement());
        LoggingContext.init(itemId, this.rpClient, this.logBufferSize);
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
                .andThen(itemId.flatMap(new Function<String, MaybeSource<OperationCompletionRS>>() {
                    @Override
                    public Maybe<OperationCompletionRS> apply(String itemId) throws Exception {
                        return rpClient.finishTestItem(itemId, rq);
                    }
                }).doAfterSuccess(new Consumer<OperationCompletionRS>() {
                    @Override
                    public void accept(OperationCompletionRS operationCompletionRS) throws Exception {
                        //cleanup children
                        treeItem.children = null;
                    }
                }).ignoreElement()).cache();

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
        List<Completable> children = new CopyOnWriteArrayList<>();

        TreeItem withParent(Maybe<String> parent) {
            this.parent = parent;
            return this;
        }

        TreeItem addToQueue(Completable completable) {
            this.children.add(completable);
            return this;
        }
    }

}
