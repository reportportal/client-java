/*
 * Copyright (C) 2018 EPAM Systems
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

import com.epam.reportportal.exception.ReportPortalException;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.listeners.Statuses;
import com.epam.reportportal.utils.LaunchFile;
import com.epam.reportportal.utils.RetryWithDelay;
import com.epam.ta.reportportal.ws.model.*;
import com.epam.ta.reportportal.ws.model.issue.Issue;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRS;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.reactivex.*;
import io.reactivex.functions.*;
import io.reactivex.schedulers.Schedulers;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static com.epam.reportportal.service.LoggingCallback.*;
import static com.epam.reportportal.utils.SubscriptionUtils.logCompletableResults;
import static com.epam.reportportal.utils.SubscriptionUtils.logMaybeResults;
import static com.google.common.collect.Lists.newArrayList;

/**
 * @author Andrei Varabyeu
 */
public class LaunchImpl extends Launch {

	private static final Function<EntryCreatedRS, Long> TO_ID = new Function<EntryCreatedRS, Long>() {
		@Override
		public Long apply(EntryCreatedRS rs) {
			return rs.getId();
		}
	};
	private static final Consumer<StartLaunchRS> LAUNCH_SUCCESS_CONSUMER = new Consumer<StartLaunchRS>() {
		@Override
		public void accept(StartLaunchRS rs) throws Exception {
			logCreated("launch").accept(rs);
			System.setProperty("rp.launch.id", String.valueOf(rs.getId()));
		}
	};
	private static final int ITEM_FINISH_MAX_RETRIES = 10;
	private static final int ITEM_FINISH_RETRY_TIMEOUT = 10;
	private static final String NOT_ISSUE = "NOT_ISSUE";

	/**
	 * REST Client
	 */
	private final ReportPortalClient rpClient;

	/**
	 * Messages queue to track items execution order
	 */
	private final LoadingCache<Maybe<Long>, LaunchImpl.TreeItem> QUEUE = CacheBuilder.newBuilder()
			.build(new CacheLoader<Maybe<Long>, LaunchImpl.TreeItem>() {
				@Override
				public LaunchImpl.TreeItem load(Maybe<Long> key) {
					return new LaunchImpl.TreeItem();
				}
			});

	private Maybe<Long> launch;
	private boolean rerun;

	LaunchImpl(final ReportPortalClient rpClient, ListenerParameters parameters, final StartLaunchRQ rq) {
		super(parameters);
		this.rpClient = Preconditions.checkNotNull(rpClient, "RestEndpoint shouldn't be NULL");
		Preconditions.checkNotNull(parameters, "Parameters shouldn't be NULL");

		if (!parameters.isRerun()) {

			LOGGER.info("Not rerun!");

			this.launch = Maybe.create(new MaybeOnSubscribe<Long>() {
				@Override
				public void subscribe(final MaybeEmitter<Long> emitter) {

					Maybe<StartLaunchRS> launchPromise = Maybe.defer(new Callable<MaybeSource<? extends StartLaunchRS>>() {
						@Override
						public MaybeSource<? extends StartLaunchRS> call() {
							return rpClient.startLaunch(rq).doOnSuccess(LAUNCH_SUCCESS_CONSUMER).doOnError(LOG_ERROR);
						}
					}).subscribeOn(Schedulers.computation()).cache();

					LaunchFile.create(rq.getName(), launchPromise);

					launchPromise.subscribe(new Consumer<StartLaunchRS>() {
						@Override
						public void accept(StartLaunchRS startLaunchRS) throws Exception {
							emitter.onSuccess(startLaunchRS.getId());
						}
					}, new Consumer<Throwable>() {
						@Override
						public void accept(Throwable throwable) throws Exception {

							LOG_ERROR.accept(throwable);
							emitter.onComplete();
						}
					});
				}
			}).cache();
		} else {
			LOGGER.info("rerun!");
			this.launch = LaunchFile.find(rq.getName());
			this.rerun = true;
		}

	}

	LaunchImpl(final ReportPortalClient rpClient, ListenerParameters parameters, Maybe<Long> launch) {
		super(parameters);
		this.rpClient = Preconditions.checkNotNull(rpClient, "RestEndpoint shouldn't be NULL");
		Preconditions.checkNotNull(parameters, "Parameters shouldn't be NULL");

		this.launch = launch.subscribeOn(Schedulers.computation()).cache();
	}

	/**
	 * Starts launch in ReportPortal. Does NOT starts the same launch twice
	 *
	 * @return Launch ID promise
	 */
	public synchronized Maybe<Long> start() {

		launch.subscribe(logMaybeResults("Launch start"));

		return this.launch;

	}

	/**
	 * Finishes launch in ReportPortal. Blocks until all items are reported correctly
	 *
	 * @param rq Finish RQ
	 */
	public synchronized void finish(final FinishExecutionRQ rq) {
		final Completable finish = Completable.concat(QUEUE.getUnchecked(this.launch).getChildren())
				.andThen(this.launch.flatMap(new Function<Long, Maybe<OperationCompletionRS>>() {
					@Override
					public Maybe<OperationCompletionRS> apply(Long id) {
						return rpClient.finishLaunch(id, rq).doOnSuccess(LOG_SUCCESS).doOnError(LOG_ERROR);
					}
				}))
				.doFinally(new Action() {
					@Override
					public void run() throws Exception {
						rpClient.close();
					}
				})
				.ignoreElement()
				.cache();
		try {
			finish.timeout(getParameters().getReportingTimeout(), TimeUnit.SECONDS).blockingGet();
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
	public Maybe<Long> startTestItem(final StartTestItemRQ rq) {

		final Maybe<Long> testItem = this.launch.flatMap(new Function<Long, Maybe<Long>>() {
			@Override
			public Maybe<Long> apply(Long launchId) {
				rq.setLaunchId(launchId);
				return rpClient.startTestItem(rq).doOnSuccess(logCreated("item")).map(TO_ID);

			}
		}).cache();
		testItem.subscribeOn(Schedulers.computation()).subscribe(logMaybeResults("Start test item"));
		QUEUE.getUnchecked(testItem).addToQueue(testItem.ignoreElement());
		LoggingContext.init(testItem, this.rpClient, getParameters().getBatchLogsSize(), getParameters().isConvertImage());
		return testItem;
	}

	public Maybe<Long> startTestItem(final Maybe<Long> parentId, final Maybe<Long> retryOf, final StartTestItemRQ rq) {
		return retryOf.flatMap(new Function<Long, Maybe<Long>>() {
			@Override
			public Maybe<Long> apply(Long s) {
				return startTestItem(parentId, rq);
			}
		}).cache();
	}

	/**
	 * Starts new test item in ReportPortal asynchronously (non-blocking)
	 *
	 * @param rq Start RQ
	 * @return Test Item ID promise
	 */
	public Maybe<Long> startTestItem(final Maybe<Long> parentId, final StartTestItemRQ rq) {
		if (null == parentId) {
			return startTestItem(rq);
		}
		final Maybe<Long> itemId = this.launch.flatMap(new Function<Long, Maybe<Long>>() {
			@Override
			public Maybe<Long> apply(final Long launchId) {
				return parentId.flatMap(new Function<Long, MaybeSource<Long>>() {
					@Override
					public MaybeSource<Long> apply(Long parentId) {
						rq.setLaunchId(launchId);
						LOGGER.debug("Starting test item..." + Thread.currentThread().getName());
						return rpClient.startTestItem(parentId, rq).doOnSuccess(logCreated("item")).map(TO_ID);
					}
				});
			}
		}).cache();
		itemId.subscribeOn(Schedulers.computation()).subscribe(logMaybeResults("Start test item"));
		QUEUE.getUnchecked(itemId).withParent(parentId).addToQueue(itemId.ignoreElement());
		LoggingContext.init(itemId, this.rpClient, getParameters().getBatchLogsSize(), getParameters().isConvertImage());
		return itemId;
	}

	/**
	 * Finishes Test Item in ReportPortal. Non-blocking. Schedules finish after success of all child items
	 *
	 * @param itemId Item ID promise
	 * @param rq     Finish request
	 */
	public void finishTestItem(final Maybe<Long> itemId, final FinishTestItemRQ rq) {

		Preconditions.checkArgument(null != itemId, "ItemID should not be null");

		if (Statuses.SKIPPED.equals(rq.getStatus()) && !getParameters().getSkippedAnIssue()) {
			Issue issue = new Issue();
			issue.setIssueType(NOT_ISSUE);
			rq.setIssue(issue);
		}

		QUEUE.getUnchecked(launch).addToQueue(LoggingContext.complete());

		LaunchImpl.TreeItem treeItem = QUEUE.getIfPresent(itemId);
		if (null == treeItem) {
			treeItem = new LaunchImpl.TreeItem();
			LOGGER.error("Item {} not found in the cache", itemId);
		}

		//wait for the children to complete
		final Completable finishCompletion = Completable.concat(treeItem.getChildren())
				.andThen(itemId.flatMap(new Function<Long, Maybe<OperationCompletionRS>>() {
					@Override
					public Maybe<OperationCompletionRS> apply(Long itemId) {
						return rpClient.finishTestItem(itemId, rq)
								.retry(new RetryWithDelay(new Predicate<Throwable>() {
									@Override
									public boolean test(Throwable throwable) {
										return throwable instanceof ReportPortalException
												&& ErrorType.FINISH_ITEM_NOT_ALLOWED.equals(((ReportPortalException) throwable).getError()
												.getErrorType());
									}
								}, ITEM_FINISH_MAX_RETRIES, TimeUnit.SECONDS.toMillis(ITEM_FINISH_RETRY_TIMEOUT)))
								.doOnSuccess(LOG_SUCCESS)
								.doOnError(LOG_ERROR);
					}
				}))
				.doAfterSuccess(new Consumer<OperationCompletionRS>() {
					@Override
					public void accept(OperationCompletionRS operationCompletionRS) {
						//cleanup children
						QUEUE.invalidate(itemId);
					}
				})
				.ignoreElement()
				.cache();
		finishCompletion.subscribeOn(Schedulers.computation()).subscribe(logCompletableResults("Finish test item"));
		//find parent and add to its queue
		final Maybe<Long> parent = treeItem.getParent();
		if (null != parent) {
			QUEUE.getUnchecked(parent).addToQueue(finishCompletion);
		} else {
			//seems like this is root item
			QUEUE.getUnchecked(this.launch).addToQueue(finishCompletion);
		}

	}

	public boolean isRerun() {
		return rerun;
	}

	/**
	 * Wrapper around TestItem entity to be able to track parent and children items
	 */
	static class TreeItem {
		private Maybe<Long> parent;
		private List<Completable> children = new CopyOnWriteArrayList<Completable>();

		synchronized LaunchImpl.TreeItem withParent(Maybe<Long> parent) {
			this.parent = parent;
			return this;
		}

		LaunchImpl.TreeItem addToQueue(Completable completable) {
			this.children.add(completable);
			return this;
		}

		List<Completable> getChildren() {
			return newArrayList(this.children);
		}

		synchronized Maybe<Long> getParent() {
			return parent;
		}
	}
}
