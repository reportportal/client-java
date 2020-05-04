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

import com.epam.reportportal.exception.InternalReportPortalClientException;
import com.epam.reportportal.exception.ReportPortalException;
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.utils.RetryWithDelay;
import com.epam.ta.reportportal.ws.model.*;
import com.epam.ta.reportportal.ws.model.issue.Issue;
import com.epam.ta.reportportal.ws.model.item.ItemCreatedRS;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRS;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.reactivex.*;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

import static com.epam.reportportal.service.LoggingCallback.*;
import static com.epam.reportportal.utils.SubscriptionUtils.logCompletableResults;
import static com.epam.reportportal.utils.SubscriptionUtils.logMaybeResults;
import static com.google.common.collect.Lists.newArrayList;

/**
 * @author Andrei Varabyeu
 */
public class LaunchImpl extends Launch {

	private static final Map<ExecutorService, Scheduler> SCHEDULERS = new ConcurrentHashMap<>();

	private static final Function<ItemCreatedRS, String> TO_ID = EntryCreatedAsyncRS::getId;
	private static final Consumer<StartLaunchRS> LAUNCH_SUCCESS_CONSUMER = rs -> {
		logCreated("launch").accept(rs);
		System.setProperty("rp.launch.id", String.valueOf(rs.getId()));
	};

	private static final int DEFAULT_RETRY_COUNT = 5;
	private static final int DEFAULT_RETRY_TIMEOUT = 2;

	private static final int ITEM_FINISH_MAX_RETRIES = 10;
	private static final int ITEM_FINISH_RETRY_TIMEOUT = 10;

	private static final Predicate<Throwable> INTERNAL_CLIENT_EXCEPTION_PREDICATE = throwable -> throwable instanceof InternalReportPortalClientException;
	private static final Predicate<Throwable> TEST_ITEM_FINISH_RETRY_PREDICATE = throwable -> (throwable instanceof ReportPortalException
			&& ErrorType.FINISH_ITEM_NOT_ALLOWED.equals(((ReportPortalException) throwable).getError().getErrorType()))
			|| INTERNAL_CLIENT_EXCEPTION_PREDICATE.test(throwable);

	private static final RetryWithDelay DEFAULT_REQUEST_RETRY = new RetryWithDelay(INTERNAL_CLIENT_EXCEPTION_PREDICATE,
			DEFAULT_RETRY_COUNT,
			TimeUnit.SECONDS.toMillis(DEFAULT_RETRY_TIMEOUT)
	);
	private static final RetryWithDelay TEST_ITEM_FINISH_REQUEST_RETRY = new RetryWithDelay(TEST_ITEM_FINISH_RETRY_PREDICATE,
			ITEM_FINISH_MAX_RETRIES,
			TimeUnit.SECONDS.toMillis(ITEM_FINISH_RETRY_TIMEOUT)
	);

	public static final String NOT_ISSUE = "NOT_ISSUE";

	/**
	 * REST Client
	 */
	private final ReportPortalClient rpClient;

	/**
	 * Messages queue to track items execution order
	 */
	protected final LoadingCache<Maybe<String>, LaunchImpl.TreeItem> QUEUE = CacheBuilder.newBuilder()
			.build(new CacheLoader<Maybe<String>, LaunchImpl.TreeItem>() {
				@Override
				public LaunchImpl.TreeItem load(Maybe<String> key) {
					return new LaunchImpl.TreeItem();
				}
			});

	protected final Maybe<String> launch;
	private final ExecutorService executor;
	private final Scheduler scheduler;

	protected LaunchImpl(@NotNull final ReportPortalClient reportPortalClient, @NotNull final ListenerParameters parameters,
			@NotNull final StartLaunchRQ rq, @NotNull final ExecutorService executorService) {
		super(parameters);
		rpClient = Objects.requireNonNull(reportPortalClient, "RestEndpoint shouldn't be NULL");
		Objects.requireNonNull(parameters, "Parameters shouldn't be NULL");
		executor = Objects.requireNonNull(executorService);
		scheduler = createScheduler(executor);

		LOGGER.info("Rerun: {}", parameters.isRerun());

		launch = Maybe.create(new MaybeOnSubscribe<String>() {
			@Override
			public void subscribe(final MaybeEmitter<String> emitter) {

				Maybe<StartLaunchRS> launchPromise = Maybe.defer(new Callable<MaybeSource<? extends StartLaunchRS>>() {
					@Override
					public MaybeSource<? extends StartLaunchRS> call() {
						return rpClient.startLaunch(rq)
								.retry(DEFAULT_REQUEST_RETRY)
								.doOnSuccess(LAUNCH_SUCCESS_CONSUMER)
								.doOnError(LOG_ERROR);
					}
				}).subscribeOn(scheduler).cache();

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
	}

	protected LaunchImpl(@NotNull final ReportPortalClient reportPortalClient, @NotNull final ListenerParameters parameters,
			@NotNull final Maybe<String> launchMaybe, @NotNull final ExecutorService executorService) {
		super(parameters);
		rpClient = Objects.requireNonNull(reportPortalClient, "RestEndpoint shouldn't be NULL");
		Objects.requireNonNull(parameters, "Parameters shouldn't be NULL");
		executor = Objects.requireNonNull(executorService);
		scheduler = createScheduler(executor);

		LOGGER.info("Rerun: {}", parameters.isRerun());

		launch = launchMaybe.subscribeOn(scheduler).cache();
	}

	protected Scheduler createScheduler(ExecutorService executorService) {
		return SCHEDULERS.computeIfAbsent(executorService, Schedulers::from);
	}

	/**
	 * Returns a current executor which is used to process launch events such as requests and responses.
	 *
	 * @return an {@link ExecutorService}
	 */
	public ExecutorService getExecutor() {
		return executor;
	}

	/**
	 * Returns a current {@link Scheduler} which is used to process launch events such as requests and responses.
	 *
	 * @return an {@link Scheduler}
	 */
	public Scheduler getScheduler() {
		return scheduler;
	}

	/**
	 * Starts launch in ReportPortal. Does NOT starts the same launch twice
	 *
	 * @return Launch ID promise
	 */
	public Maybe<String> start() {
		launch.subscribe(logMaybeResults("Launch start"));
		LaunchLoggingContext.init(this.launch,
				this.rpClient,
				this.scheduler,
				getParameters().getBatchLogsSize(),
				getParameters().isConvertImage()
		);

		return this.launch;
	}

	/**
	 * Finishes launch in ReportPortal. Blocks until all items are reported correctly
	 *
	 * @param rq Finish RQ
	 */
	public void finish(final FinishExecutionRQ rq, final Completable... dependencies) {
		QUEUE.getUnchecked(launch).addToQueue(LaunchLoggingContext.complete());
		List<Completable> children = QUEUE.getUnchecked(this.launch).getChildren();
		Collections.addAll(children, dependencies);
		final Completable finish = Completable.concat(children)
				.andThen(this.launch.map(id -> rpClient.finishLaunch(id, rq)
						.retry(DEFAULT_REQUEST_RETRY)
						.doOnSuccess(LOG_SUCCESS)
						.doOnError(LOG_ERROR)
						.blockingGet()))
				.ignoreElement()
				.cache();
		try {
			Throwable error = finish.timeout(getParameters().getReportingTimeout(), TimeUnit.SECONDS).blockingGet();
			if (error != null) {
				LOGGER.error("Unable to finish launch in ReportPortal", error);
			}
		} finally {
			rpClient.close();
		}
	}

	/**
	 * Starts new test item in ReportPortal asynchronously (non-blocking)
	 *
	 * @param rq Start RQ
	 * @return Test Item ID promise
	 */
	public Maybe<String> startTestItem(final StartTestItemRQ rq) {

		final Maybe<String> testItem = launch.flatMap(new Function<String, Maybe<String>>() {
			@Override
			public Maybe<String> apply(String launchId) {
				rq.setLaunchUuid(launchId);
				return rpClient.startTestItem(rq).retry(DEFAULT_REQUEST_RETRY).doOnSuccess(logCreated("item")).map(TO_ID);

			}
		}).cache();
		testItem.subscribeOn(scheduler).subscribe(logMaybeResults("Start test item"));
		QUEUE.getUnchecked(testItem).addToQueue(testItem.ignoreElement().onErrorComplete());
		LoggingContext.init(launch, testItem, rpClient, scheduler, getParameters().getBatchLogsSize(), getParameters().isConvertImage());
		getStepReporter().setParent(testItem);
		return testItem;
	}

	public Maybe<String> startTestItem(final Maybe<String> parentId, final Maybe<String> retryOf, final StartTestItemRQ rq) {
		return retryOf.flatMap(new Function<String, Maybe<String>>() {
			@Override
			public Maybe<String> apply(String s) {
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
	public Maybe<String> startTestItem(final Maybe<String> parentId, final StartTestItemRQ rq) {
		if (null == parentId) {
			return startTestItem(rq);
		}
		final Maybe<String> itemId = launch.flatMap(new Function<String, Maybe<String>>() {
			@Override
			public Maybe<String> apply(final String launchId) {
				return parentId.flatMap(new Function<String, MaybeSource<String>>() {
					@Override
					public MaybeSource<String> apply(String parentId) {
						rq.setLaunchUuid(launchId);
						LOGGER.debug("Starting test item..." + Thread.currentThread().getName());
						Maybe<ItemCreatedRS> result = rpClient.startTestItem(parentId, rq);
						result = result.retry(DEFAULT_REQUEST_RETRY);
						result = result.doOnSuccess(logCreated("item"));
						return result.map(TO_ID);
					}
				});
			}
		}).cache();
		itemId.subscribeOn(scheduler).subscribe(logMaybeResults("Start test item"));
		QUEUE.getUnchecked(itemId).withParent(parentId).addToQueue(itemId.ignoreElement().onErrorComplete());
		LoggingContext.init(launch, itemId, rpClient, scheduler, getParameters().getBatchLogsSize(), getParameters().isConvertImage());
		getStepReporter().setParent(itemId);
		return itemId;
	}

	/**
	 * Finishes Test Item in ReportPortal. Non-blocking. Schedules finish after success of all child items
	 *
	 * @param item Item UUID promise
	 * @param rq   Finish request
	 * @return a Finish Item response promise
	 */
	public Maybe<OperationCompletionRS> finishTestItem(@NotNull final Maybe<String> item, @NotNull final FinishTestItemRQ rq) {
		Objects.requireNonNull(item, "ItemID should not be null");
		Objects.requireNonNull(rq, "FinishTestItemRQ should not be null");

		if (ItemStatus.SKIPPED.name().equals(rq.getStatus()) && !getParameters().getSkippedAnIssue()) {
			Issue issue = new Issue();
			issue.setIssueType(NOT_ISSUE);
			rq.setIssue(issue);
		}

		QUEUE.getUnchecked(launch).addToQueue(LoggingContext.complete());

		LaunchImpl.TreeItem treeItem = QUEUE.getIfPresent(item);
		if (null == treeItem) {
			treeItem = new LaunchImpl.TreeItem();
			LOGGER.error("Item {} not found in the cache", item);
		}

		if (getStepReporter().isFailed(item)) {
			rq.setStatus(ItemStatus.FAILED.name());
		}

		//wait for the children to complete
		Maybe<OperationCompletionRS> finishResponse = this.launch.flatMap((Function<String, Maybe<OperationCompletionRS>>) launchId -> item.flatMap(
				(Function<String, Maybe<OperationCompletionRS>>) itemId -> {
					rq.setLaunchUuid(launchId);
					return rpClient.finishTestItem(itemId, rq)
							.retry(TEST_ITEM_FINISH_REQUEST_RETRY)
							.doOnSuccess(LOG_SUCCESS)
							.doOnError(LOG_ERROR);
				})).cache();

		Completable finishCompletion = Completable.concat(treeItem.getChildren())
				.andThen(finishResponse)
				.doAfterTerminate(() -> QUEUE.invalidate(item)) //cleanup children
				.ignoreElement()
				.cache();
		finishCompletion.subscribeOn(scheduler).subscribe(logCompletableResults("Finish test item"));
		//find parent and add to its queue
		final Maybe<String> parent = treeItem.getParent();
		if (null != parent) {
			QUEUE.getUnchecked(parent).addToQueue(finishCompletion.onErrorComplete());
		} else {
			//seems like this is root item
			QUEUE.getUnchecked(this.launch).addToQueue(finishCompletion.onErrorComplete());
		}

		getStepReporter().removeParent(item);

		return finishResponse;
	}

	/**
	 * Wrapper around TestItem entity to be able to track parent and children items
	 */
	protected static class TreeItem {
		private volatile Maybe<String> parent;
		private final List<Completable> children = new CopyOnWriteArrayList<Completable>();

		public LaunchImpl.TreeItem withParent(Maybe<String> parent) {
			this.parent = parent;
			return this;
		}

		public LaunchImpl.TreeItem addToQueue(Completable completable) {
			this.children.add(completable);
			return this;
		}

		public List<Completable> getChildren() {
			return newArrayList(this.children);
		}

		public Maybe<String> getParent() {
			return parent;
		}
	}
}
