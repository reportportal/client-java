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
import com.epam.reportportal.service.statistics.StatisticsService;
import com.epam.reportportal.utils.RetryWithDelay;
import com.epam.reportportal.utils.properties.DefaultProperties;
import com.epam.ta.reportportal.ws.model.*;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
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
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.epam.reportportal.service.logs.LaunchLoggingCallback.*;
import static com.epam.reportportal.utils.ObjectUtils.clonePojo;
import static com.epam.reportportal.utils.SubscriptionUtils.logCompletableResults;
import static com.epam.reportportal.utils.SubscriptionUtils.logMaybeResults;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

/**
 * A basic Launch object implementation which does straight requests to Report Portal.
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
	private static final Predicate<Throwable> TEST_ITEM_FINISH_RETRY_PREDICATE = throwable ->
			(throwable instanceof ReportPortalException
					&& ErrorType.FINISH_ITEM_NOT_ALLOWED.equals(((ReportPortalException) throwable).getError()
					.getErrorType())) || INTERNAL_CLIENT_EXCEPTION_PREDICATE.test(throwable);

	private static final RetryWithDelay DEFAULT_REQUEST_RETRY = new RetryWithDelay(INTERNAL_CLIENT_EXCEPTION_PREDICATE,
			DEFAULT_RETRY_COUNT,
			TimeUnit.SECONDS.toMillis(DEFAULT_RETRY_TIMEOUT)
	);
	private static final RetryWithDelay TEST_ITEM_FINISH_REQUEST_RETRY = new RetryWithDelay(
			TEST_ITEM_FINISH_RETRY_PREDICATE,
			ITEM_FINISH_MAX_RETRIES,
			TimeUnit.SECONDS.toMillis(ITEM_FINISH_RETRY_TIMEOUT)
	);

	/**
	 * @deprecated use {@link Launch#NOT_ISSUE}
	 */
	@Deprecated
	public static final String NOT_ISSUE = "NOT_ISSUE";

	public static final String CUSTOM_AGENT = "CUSTOM";

	/**
	 * Messages queue to track items execution order
	 */
	protected final LoadingCache<Maybe<String>, LaunchImpl.TreeItem> QUEUE = CacheBuilder.newBuilder()
			.build(new CacheLoader<Maybe<String>, LaunchImpl.TreeItem>() {
				@Override
				public LaunchImpl.TreeItem load(@Nonnull Maybe<String> key) {
					return new LaunchImpl.TreeItem();
				}
			});

	protected final Maybe<String> launch;
	private final ExecutorService executor;
	private final Scheduler scheduler;
	private StatisticsService statisticsService;
	private final StartLaunchRQ startRq;

	protected LaunchImpl(@Nonnull final ReportPortalClient reportPortalClient,
			@Nonnull final ListenerParameters parameters, @Nonnull final StartLaunchRQ rq,
			@Nonnull final ExecutorService executorService) {
		super(reportPortalClient, parameters);
		requireNonNull(parameters, "Parameters shouldn't be NULL");
		executor = requireNonNull(executorService);
		scheduler = createScheduler(executor);
		statisticsService = new StatisticsService(parameters);
		startRq = clonePojo(rq, StartLaunchRQ.class);

		LOGGER.info("Rerun: {}", parameters.isRerun());

		launch = Maybe.create((MaybeOnSubscribe<String>) emitter -> {
			Maybe<StartLaunchRS> launchPromise = Maybe.defer(() -> {
				truncateAttributes(startRq);
				return getClient().startLaunch(startRq)
						.retry(DEFAULT_REQUEST_RETRY)
						.doOnSuccess(LAUNCH_SUCCESS_CONSUMER)
						.doOnError(LOG_ERROR);
			}).subscribeOn(getScheduler()).cache();

			//noinspection ResultOfMethodCallIgnored
			launchPromise.subscribe(rs -> emitter.onSuccess(rs.getId()), t -> {
				LOG_ERROR.accept(t);
				emitter.onComplete();
			});
		}).cache();
	}

	protected LaunchImpl(@Nonnull final ReportPortalClient reportPortalClient,
			@Nonnull final ListenerParameters parameters, @Nonnull final Maybe<String> launchMaybe,
			@Nonnull final ExecutorService executorService) {
		super(reportPortalClient, parameters);
		requireNonNull(parameters, "Parameters shouldn't be NULL");
		executor = requireNonNull(executorService);
		scheduler = createScheduler(executor);
		statisticsService = new StatisticsService(parameters);
		startRq = emptyStartLaunchForStatistics();

		LOGGER.info("Rerun: {}", parameters.isRerun());
		launch = launchMaybe.cache();
	}

	private static StartLaunchRQ emptyStartLaunchForStatistics() {
		StartLaunchRQ result = new StartLaunchRQ();
		result.setAttributes(Collections.singleton(new ItemAttributesRQ(DefaultProperties.AGENT.getName(),
				CUSTOM_AGENT,
				true
		)));
		return result;
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

	@Override
	@Nonnull
	public Maybe<String> getLaunch() {
		return launch;
	}

	StatisticsService getStatisticsService() {
		return statisticsService;
	}

	private void truncateName(@Nonnull final StartTestItemRQ rq) {
		if (!getParameters().isTruncateFields() || rq.getName() == null || rq.getName().isEmpty()) {
			return;
		}
		String name = rq.getName();
		int limit = getParameters().getTruncateItemNamesLimit();
		String replacement = getParameters().getTruncateReplacement();
		if (name.length() > limit && name.length() > replacement.length()) {
			rq.setName(name.substring(0, limit - replacement.length()) + replacement);
		}
	}

	@Nullable
	private Set<ItemAttributesRQ> truncateAttributes(@Nullable final Set<ItemAttributesRQ> attributes) {
		if (!getParameters().isTruncateFields() || attributes == null || attributes.isEmpty()) {
			return attributes;
		}

		int limit = getParameters().getAttributeLengthLimit();
		String replacement = getParameters().getTruncateReplacement();
		return attributes.stream().map(attribute -> {
			ItemAttributesRQ updated = attribute;
			int keyLength = ofNullable(updated.getKey()).map(String::length).orElse(0);
			if (keyLength > limit && keyLength > replacement.length()) {
				updated = new ItemAttributesRQ(
						updated.getKey().substring(0, limit - replacement.length()) + replacement,
						updated.getValue(),
						updated.isSystem()
				);
			}
			int valueLength = ofNullable(updated.getValue()).map(String::length).orElse(0);
			if (valueLength > limit && valueLength > replacement.length()) {
				updated = new ItemAttributesRQ(updated.getKey(),
						updated.getValue().substring(0, limit - replacement.length()) + replacement,
						updated.isSystem()
				);
			}
			return updated;
		}).collect(Collectors.toSet());
	}

	private void truncateAttributes(@Nonnull final StartRQ rq) {
		rq.setAttributes(truncateAttributes(rq.getAttributes()));
	}

	private void truncateAttributes(@Nonnull final FinishExecutionRQ rq) {
		rq.setAttributes(truncateAttributes(rq.getAttributes()));
	}

	/**
	 * Starts launch in ReportPortal. Does NOT start the same launch twice
	 *
	 * @return Launch ID promise
	 */
	@Nonnull
	public Maybe<String> start() {
		launch.subscribe(logMaybeResults("Launch start"));
		LaunchLoggingContext.init(this.launch, getClient(), getScheduler(), getParameters());
		getStatisticsService().sendEvent(launch, startRq);
		return launch;
	}

	/**
	 * Finishes launch in ReportPortal. Blocks until all items are reported correctly
	 *
	 * @param request Finish RQ
	 */
	public void finish(final FinishExecutionRQ request) {
		QUEUE.getUnchecked(launch).addToQueue(LaunchLoggingContext.complete());
		Completable finish = Completable.concat(QUEUE.getUnchecked(launch).getChildren());
		if (StringUtils.isBlank(getParameters().getLaunchUuid())) {
			FinishExecutionRQ rq = clonePojo(request, FinishExecutionRQ.class);
			truncateAttributes(rq);
			finish = finish.andThen(launch.map(id -> getClient().finishLaunch(id, rq)
					.retry(DEFAULT_REQUEST_RETRY)
					.doOnSuccess(LOG_SUCCESS)
					.doOnError(LOG_ERROR)
					.blockingGet())).ignoreElement();
		}
		finish = finish.cache();

		Throwable error = finish.timeout(getParameters().getReportingTimeout(), TimeUnit.SECONDS).blockingGet();
		if (error != null) {
			LOGGER.error("Unable to finish launch in ReportPortal", error);
		}
		getStatisticsService().close();
		statisticsService = new StatisticsService(getParameters());
	}

	private static <T> Maybe<T> createErrorResponse(Throwable cause) {
		LOGGER.error(cause.getMessage(), cause);
		return Maybe.error(cause);
	}

	/**
	 * Starts new test item in ReportPortal asynchronously (non-blocking)
	 *
	 * @param request Start RQ
	 * @return Test Item ID promise
	 */
	@Nonnull
	public Maybe<String> startTestItem(final StartTestItemRQ request) {
		if (request == null) {
			/*
			 * This usually happens when we have a bug inside an agent or supported framework. But in any case we shouldn't rise an exception,
			 * since we are reporting tool and our problems	should not fail launches.
			 */
			return createErrorResponse(new NullPointerException("StartTestItemRQ should not be null"));
		}
		StartTestItemRQ rq = clonePojo(request, StartTestItemRQ.class);
		truncateName(rq);
		truncateAttributes(rq);

		Maybe<String> item = launch.flatMap((Function<String, Maybe<String>>) launchId -> {
			rq.setLaunchUuid(launchId);
			return getClient().startTestItem(rq)
					.retry(DEFAULT_REQUEST_RETRY)
					.doOnSuccess(logCreated("item"))
					.map(TO_ID);
		}).cache();

		item.subscribeOn(getScheduler()).subscribe(logMaybeResults("Start test item"));
		QUEUE.getUnchecked(item).addToQueue(item.ignoreElement().onErrorComplete());
		LoggingContext.init(launch, item, getClient(), getScheduler(), getParameters());

		getStepReporter().setParent(item);
		return item;
	}

	/**
	 * Starts new test item in ReportPortal in respect of provided retry item ID.
	 *
	 * @param parentId Parent item ID promise
	 * @param retryOf  previous item ID promise
	 * @param rq       Start RQ
	 * @return Test Item ID promise
	 */
	@Nonnull
	public Maybe<String> startTestItem(final Maybe<String> parentId, final Maybe<String> retryOf,
			final StartTestItemRQ rq) {
		return retryOf.flatMap((Function<String, Maybe<String>>) s -> startTestItem(parentId, rq)).cache();
	}

	/**
	 * Starts new test item in ReportPortal asynchronously (non-blocking)
	 *
	 * @param parentId Parent item ID promise
	 * @param request  Start RQ
	 * @return Test Item ID promise
	 */
	@Nonnull
	public Maybe<String> startTestItem(final Maybe<String> parentId, final StartTestItemRQ request) {
		if (parentId == null) {
			return startTestItem(request);
		}
		if (request == null) {
			/*
			 * This usually happens when we have a bug inside an agent or supported framework. But in any case we shouldn't rise an exception,
			 * since we are reporting tool and our problems	should not fail launches.
			 */
			return createErrorResponse(new NullPointerException("StartTestItemRQ should not be null"));
		}
		StartTestItemRQ rq = clonePojo(request, StartTestItemRQ.class);
		truncateName(rq);
		truncateAttributes(rq);

		final Maybe<String> item = launch.flatMap((Function<String, Maybe<String>>) lId -> parentId.flatMap((Function<String, MaybeSource<String>>) pId -> {
			rq.setLaunchUuid(lId);
			LOGGER.debug("Starting test item..." + Thread.currentThread().getName());
			Maybe<ItemCreatedRS> result = getClient().startTestItem(pId, rq);
			result = result.retry(DEFAULT_REQUEST_RETRY);
			result = result.doOnSuccess(logCreated("item"));
			return result.map(TO_ID);
		})).cache();
		item.subscribeOn(getScheduler()).subscribe(logMaybeResults("Start test item"));
		QUEUE.getUnchecked(item).withParent(parentId).addToQueue(item.ignoreElement().onErrorComplete());
		LoggingContext.init(launch, item, getClient(), getScheduler(), getParameters());

		getStepReporter().setParent(item);
		return item;
	}

	/**
	 * Finishes Test Item in ReportPortal. Non-blocking. Schedules finish after success of all child items
	 *
	 * @param item    Item UUID promise
	 * @param request Finish request
	 * @return a Finish Item response promise
	 */
	@Nonnull
	public Maybe<OperationCompletionRS> finishTestItem(final Maybe<String> item, final FinishTestItemRQ request) {
		if (item == null) {
			/*
			 * This usually happens when we have a bug inside an agent or supported framework. But in any case we shouldn't rise an exception,
			 * since we are reporting tool and our problems	should not fail launches.
			 */
			return createErrorResponse(new NullPointerException("ItemID should not be null"));
		}
		if (request == null) {
			return createErrorResponse(new NullPointerException("FinishTestItemRQ should not be null"));
		}
		FinishTestItemRQ rq = clonePojo(request, FinishTestItemRQ.class);
		truncateAttributes(rq);

		getStepReporter().finishPreviousStep(ofNullable(rq.getStatus()).map(ItemStatus::valueOf).orElse(null));

		if (ItemStatus.SKIPPED.name().equals(rq.getStatus()) && !getParameters().getSkippedAnIssue()) {
			rq.setIssue(Launch.NOT_ISSUE);
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
					return getClient().finishTestItem(itemId, rq)
							.retry(TEST_ITEM_FINISH_REQUEST_RETRY)
							.doOnSuccess(LOG_SUCCESS)
							.doOnError(LOG_ERROR);
				})).cache();

		Completable finishCompletion = Completable.concat(treeItem.getChildren())
				.andThen(finishResponse)
				.doAfterTerminate(() -> QUEUE.invalidate(item)) //cleanup children
				.ignoreElement()
				.cache();
		finishCompletion.subscribeOn(getScheduler()).subscribe(logCompletableResults("Finish test item"));
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
		private final List<Completable> children = new CopyOnWriteArrayList<>();

		public LaunchImpl.TreeItem withParent(Maybe<String> parent) {
			this.parent = parent;
			return this;
		}

		public void addToQueue(Completable completable) {
			this.children.add(completable);
		}

		public List<Completable> getChildren() {
			return newArrayList(this.children);
		}

		public Maybe<String> getParent() {
			return parent;
		}
	}
}
