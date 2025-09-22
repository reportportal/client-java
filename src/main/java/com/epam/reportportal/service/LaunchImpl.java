/*
 * Copyright 2025 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
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
import com.epam.reportportal.message.TypeAwareByteSource;
import com.epam.reportportal.service.logs.LogBatchingFlowable;
import com.epam.reportportal.service.logs.LoggingSubscriber;
import com.epam.reportportal.service.statistics.StatisticsService;
import com.epam.reportportal.utils.*;
import com.epam.reportportal.utils.files.ByteSource;
import com.epam.reportportal.utils.http.HttpRequestUtils;
import com.epam.reportportal.utils.properties.DefaultProperties;
import com.epam.ta.reportportal.ws.model.*;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import com.epam.ta.reportportal.ws.model.issue.Issue;
import com.epam.ta.reportportal.ws.model.item.ItemCreatedRS;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRS;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import com.epam.ta.reportportal.ws.model.project.config.ProjectSettingsResource;
import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.internal.operators.flowable.FlowableFromObservable;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.epam.reportportal.service.logs.LaunchLoggingCallback.LOG_ERROR;
import static com.epam.reportportal.service.logs.LaunchLoggingCallback.LOG_SUCCESS;
import static com.epam.reportportal.utils.BasicUtils.compareSemanticVersions;
import static com.epam.reportportal.utils.ObjectUtils.clonePojo;
import static com.epam.reportportal.utils.SubscriptionUtils.*;
import static com.epam.reportportal.utils.files.ImageConverter.convert;
import static com.epam.reportportal.utils.files.ImageConverter.isImage;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

/**
 * Asynchronous Launch object implementation, which uses reactive framework to handle launch events. It accepts and returns promises of
 * items' IDs and launch ID, and handle actual requests to ReportPortal under the hood.
 */
public class LaunchImpl extends Launch {
	/**
	 * Environment variable name to disable analytics
	 */
	public static final String DISABLE_PROPERTY = "AGENT_NO_ANALYTICS";

	public static final String MICROSECONDS_MIN_VERSION = "5.13.2";

	private static final Map<ExecutorService, Scheduler> SCHEDULERS = new ConcurrentHashMap<>();

	private static final Function<ItemCreatedRS, String> TO_ID = EntryCreatedAsyncRS::getId;

	private static final int DEFAULT_RETRY_COUNT = 5;
	private static final int DEFAULT_RETRY_TIMEOUT = 2;

	private static final int ITEM_FINISH_MAX_RETRIES = 10;
	private static final int ITEM_FINISH_RETRY_TIMEOUT = 10;

	private static final int LOG_REMOVE_FACTOR = 100;

	private static final Predicate<Throwable> INTERNAL_CLIENT_EXCEPTION_PREDICATE = throwable -> throwable instanceof InternalReportPortalClientException;
	private static final Predicate<Throwable> TEST_ITEM_FINISH_RETRY_PREDICATE = throwable -> (throwable instanceof ReportPortalException
			&& ErrorType.FINISH_ITEM_NOT_ALLOWED.equals(((ReportPortalException) throwable).getError().getErrorType()))
			|| INTERNAL_CLIENT_EXCEPTION_PREDICATE.test(throwable);

	private static final RetryWithDelay DEFAULT_REQUEST_RETRY = new RetryWithDelay(
			INTERNAL_CLIENT_EXCEPTION_PREDICATE,
			DEFAULT_RETRY_COUNT,
			TimeUnit.SECONDS.toMillis(DEFAULT_RETRY_TIMEOUT)
	);
	private static final RetryWithDelay TEST_ITEM_FINISH_REQUEST_RETRY = new RetryWithDelay(
			TEST_ITEM_FINISH_RETRY_PREDICATE,
			ITEM_FINISH_MAX_RETRIES,
			TimeUnit.SECONDS.toMillis(ITEM_FINISH_RETRY_TIMEOUT)
	);

	/**
	 * Default Agent name for cases where real name is not known.
	 */
	public static final String CUSTOM_AGENT = "CUSTOM";

	/**
	 * Messages queue to track items execution order
	 */
	protected final ComputationConcurrentHashMap queue = new ComputationConcurrentHashMap();

	/**
	 * Mapping between virtual item Maybes and their emitters
	 */
	protected final Map<Maybe<String>, PublishSubject<String>> virtualItems = new ConcurrentHashMap<>();

	/**
	 * Collection of disposables from virtual item subscriptions
	 */
	protected final Queue<Disposable> virtualItemDisposables = new ConcurrentLinkedQueue<>();

	protected final Queue<Completable> logCompletables = new ConcurrentLinkedQueue<>();

	protected final StartLaunchRQ startRq;
	protected final Maybe<ProjectSettingsResource> projectSettings;
	protected final Maybe<ApiInfo> apiInfo;
	private final Supplier<Maybe<String>> launch;
	private final PublishSubject<SaveLogRQ> logEmitter;
	private final ExecutorService executor;
	private final Scheduler scheduler;
	private final LoggingSubscriber loggingSubscriber;
	private StatisticsService statisticsService;
	private volatile Boolean useMicroseconds;

	private static PublishSubject<SaveLogRQ> createLogEmitter(@Nonnull final ReportPortalClient client,
			@Nonnull final ListenerParameters parameters, @Nonnull final Scheduler scheduler,
			@Nonnull final FlowableSubscriber<BatchSaveOperatingRS> loggingSubscriber) {
		PublishSubject<SaveLogRQ> emitter = PublishSubject.create();
		RxJavaPlugins.onAssembly(new LogBatchingFlowable(new FlowableFromObservable<>(emitter), parameters))
				.flatMap((Function<List<SaveLogRQ>, Flowable<BatchSaveOperatingRS>>) rqs -> client.log(HttpRequestUtils.buildLogMultiPartRequest(
						rqs)).retry(DEFAULT_REQUEST_RETRY).toFlowable())
				.onBackpressureBuffer(parameters.getRxBufferSize(), false, true)
				.cache()
				.subscribeOn(scheduler)
				.subscribe(loggingSubscriber);
		return emitter;
	}

	private static Maybe<ProjectSettingsResource> getProjectSettings(@Nonnull final ReportPortalClient client,
			@Nonnull final Scheduler scheduler) {
		return ofNullable(client.getProjectSettings()).map(settings -> settings.subscribeOn(scheduler).cache()).orElse(Maybe.empty());
	}

	private static Maybe<ApiInfo> getApiInfo(@Nonnull final ReportPortalClient client, @Nonnull final Scheduler scheduler) {
		return ofNullable(client.getApiInfo()).map(info -> info.subscribeOn(scheduler).cache()).orElse(Maybe.empty());
	}

	private static StartLaunchRQ emptyStartLaunchForStatistics() {
		StartLaunchRQ result = new StartLaunchRQ();
		result.setAttributes(Collections.singleton(new ItemAttributesRQ(DefaultProperties.AGENT.getName(), CUSTOM_AGENT, true)));
		return result;
	}

	/**
	 * Constructs a {@link LaunchImpl} that starts a new launch on first use and
	 * configures a batched log emitter.
	 *
	 * @param reportPortalClient the client used to communicate with ReportPortal
	 * @param parameters         the agent listener parameters
	 * @param rq                 launch start request used to initialize the launch
	 * @param executorService    executor used to schedule reactive operations
	 * @param loggingSubscriber  subscriber which receives batched log upload results
	 * @throws InternalReportPortalClientException if the provided executor is already shut down
	 */
	protected LaunchImpl(@Nonnull final ReportPortalClient reportPortalClient, @Nonnull final ListenerParameters parameters,
			@Nonnull final StartLaunchRQ rq, @Nonnull final ExecutorService executorService,
			@Nonnull final LoggingSubscriber loggingSubscriber) {
		super(reportPortalClient, parameters);
		requireNonNull(parameters, "Parameters shouldn't be NULL");
		executor = requireNonNull(executorService);
		if (executor.isShutdown()) {
			throw new InternalReportPortalClientException("Executor service is already shut down");
		}
		scheduler = createScheduler(executor);
		statisticsService = new StatisticsService(parameters);
		startRq = rq;
		truncateAttributes(startRq);

		LOGGER.info("Rerun: {}", parameters.isRerun());

		launch = getLaunchSupplier(getClient(), getScheduler(), startRq);
		this.loggingSubscriber = loggingSubscriber;
		logEmitter = createLogEmitter(getClient(), getParameters(), getScheduler(), loggingSubscriber);
		projectSettings = getProjectSettings(getClient(), getScheduler());
		apiInfo = getApiInfo(getClient(), getScheduler());
	}

	/**
	 * Convenience constructor which uses a default {@link LoggingSubscriber} for batched log uploads.
	 *
	 * @param reportPortalClient the client used to communicate with ReportPortal
	 * @param parameters         the agent listener parameters
	 * @param rq                 launch start request used to initialize the launch
	 * @param executorService    executor used to schedule reactive operations
	 */
	protected LaunchImpl(@Nonnull final ReportPortalClient reportPortalClient, @Nonnull final ListenerParameters parameters,
			@Nonnull final StartLaunchRQ rq, @Nonnull final ExecutorService executorService) {
		this(reportPortalClient, parameters, rq, executorService, new LoggingSubscriber());
	}

	/**
	 * Constructs a {@link LaunchImpl} using an already created launch ID promise.
	 * No new launch will be started by this instance.
	 *
	 * @param reportPortalClient the client used to communicate with ReportPortal
	 * @param parameters         the agent listener parameters
	 * @param launchMaybe        an existing launch ID promise to be used by this instance
	 * @param executorService    executor used to schedule reactive operations
	 * @throws InternalReportPortalClientException if the provided executor is already shut down
	 */
	protected LaunchImpl(@Nonnull final ReportPortalClient reportPortalClient, @Nonnull final ListenerParameters parameters,
			@Nonnull final Maybe<String> launchMaybe, @Nonnull final ExecutorService executorService) {
		super(reportPortalClient, parameters);
		requireNonNull(parameters, "Parameters shouldn't be NULL");
		executor = requireNonNull(executorService);
		if (executor.isShutdown()) {
			throw new InternalReportPortalClientException("Executor service is already shut down");
		}
		scheduler = createScheduler(executor);
		statisticsService = new StatisticsService(parameters);
		startRq = emptyStartLaunchForStatistics();

		LOGGER.info("Rerun: {}", parameters.isRerun());
		launch = () -> launchMaybe.cache().subscribeOn(getScheduler());
		loggingSubscriber = new LoggingSubscriber();
		logEmitter = createLogEmitter(getClient(), getParameters(), getScheduler(), loggingSubscriber);
		projectSettings = getProjectSettings(getClient(), getScheduler());
		apiInfo = getApiInfo(getClient(), getScheduler());
	}

	private Supplier<Maybe<String>> getLaunchSupplier(@Nonnull final ReportPortalClient client, @Nonnull final Scheduler scheduler,
			@Nonnull final StartLaunchRQ startRq) {
		return new MemoizingSupplier<>(() -> {
			StartLaunchRQ myStartRq = clonePojo(startRq, StartLaunchRQ.class);
			myStartRq.setStartTime(convertIfNecessary(myStartRq.getStartTime()));
			return client.startLaunch(myStartRq)
					.retry(DEFAULT_REQUEST_RETRY)
					.map(StartLaunchRS::getId)
					.cache()
					.subscribeOn(scheduler);});
	}

	/**
	 * Creates or reuses an RxJava {@link Scheduler} backed by the provided executor.
	 * Ensures a single scheduler per executor instance.
	 *
	 * @param executorService the executor to back the scheduler
	 * @return a scheduler mapped to the provided executor
	 */
	protected Scheduler createScheduler(ExecutorService executorService) {
		return SCHEDULERS.computeIfAbsent(executorService, Schedulers::from);
	}

	/**
	 * Determines whether timestamps should use microsecond precision based on the
	 * ReportPortal server version. Versions greater than or equal to 5.13.2
	 * support microseconds.
	 * <p>
	 * The value is computed once and cached for subsequent calls.
	 *
	 * @return {@code true} if server version greater or equal 5.13.2, otherwise {@code false}
	 */
	public boolean useMicroseconds() {
		if (useMicroseconds != null) {
			return useMicroseconds;
		}

		boolean result = false;
		try {
			ApiInfo info = apiInfo.blockingGet();
			String version = ofNullable(info).map(ApiInfo::getBuild).map(ApiInfo.Build::getVersion).orElse(null);
			if (StringUtils.isNotBlank(version)) {
				result = compareSemanticVersions(version, MICROSECONDS_MIN_VERSION) >= 0;
			}
		} catch (Exception e) {
			// Ignore and keep default false when API info is unavailable
		}
		this.useMicroseconds = result;
		return result;
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
	 * Returns current launch UUID promise as {@link Maybe}, empty if the launch is not started.
	 *
	 * @return Launch UUID promise.
	 */
	@Override
	@Nonnull
	public Maybe<String> getLaunch() {
		return launch.get();
	}

	/**
	 * Returns the statistics service responsible for sending analytics events.
	 *
	 * @return the current {@link StatisticsService}
	 */
	StatisticsService getStatisticsService() {
		return statisticsService;
	}

	private void truncateName(@Nonnull final StartTestItemRQ rq) {
		if (!getParameters().isTruncateFields() || rq.getName() == null || rq.getName().isEmpty()) {
			return;
		}
		String name = rq.getName();
		ListenerParameters params = getParameters();
		rq.setName(BasicUtils.truncateString(name, params.getTruncateItemNamesLimit(), params.getTruncateReplacement()));
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
				updated = new ItemAttributesRQ(
						updated.getKey(),
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

	@Nullable
	private Comparable<? extends Comparable<?>> convertIfNecessary(@Nullable Comparable<? extends Comparable<?>> dateTime) {
		if (dateTime == null) {
			return null;
		}
		boolean useMicroseconds = useMicroseconds();
		if (dateTime instanceof Instant && !useMicroseconds) {
			// Convert Instant to Date if microseconds are not supported
			return Date.from((Instant) dateTime);
		} else {
			// Not Instant return as is
			return dateTime;
		}
	}

	/**
	 * Starts the launch asynchronously on first subscription. Subsequent calls do
	 * not start the launch again due to internal caching.
	 *
	 * @param statistics whether to send analytics events for the launch
	 * @return a {@link Maybe} that emits the launch UUID once available
	 */
	@Nonnull
	protected Maybe<String> start(boolean statistics) {
		if (getExecutor().isShutdown()) {
			LOGGER.error("Unable to start Launch: executor service is already shut down. The data may be lost.");
			return Maybe.empty();
		}
		Maybe<String> myLaunch = getLaunch();

		ListenerParameters params = getParameters();
		if (params.isPrintLaunchUuid()) {
			myLaunch.subscribe(printLaunch(params));
		} else {
			myLaunch.subscribe(logMaybeResults("Launch start"));
		}
		if (statistics) {
			getStatisticsService().sendEvent(myLaunch, startRq);
		}
		return myLaunch;
	}

	/**
	 * Starts new launch in ReportPortal asynchronously (non-blocking). Does NOT start the same launch twice.
	 *
	 * @return Launch ID promise.
	 */
	@Nonnull
	public Maybe<String> start() {
		return start(System.getenv(DISABLE_PROPERTY) == null);
	}

	/**
	 * Creates a Completable that polls for completion of all virtual items.
	 * This method recursively checks if all virtual items have been populated with real IDs.
	 * If there are still virtual items pending, it schedules another check after a 100ms delay.
	 * When all virtual items are processed (the virtualItems map is empty), it completes.
	 *
	 * @return A Completable that completes when all virtual items are processed
	 */
	protected Completable createVirtualItemCompletable() {
		if (virtualItems.isEmpty()) {
			return Completable.complete();
		}
		// Poll every 100ms until all virtual items are processed
		return Completable.timer(100, TimeUnit.MILLISECONDS).andThen(Completable.defer(this::createVirtualItemCompletable));
	}

	/**
	 * Executes completion tasks with a timeout.
	 *
	 * @param completableTasks Completable tasks to execute
	 */
	protected void waitForCompletable(Completable... completableTasks) {
		if (completableTasks == null || completableTasks.length == 0) {
			return;
		}
		long timeoutInSeconds = getParameters().getReportingTimeout();
		// Wait for all items (including virtual) to be reported in a non-blocking way
		try {
			// Run all completion tasks concurrently but within the timeout
			Completable completable = completableTasks.length > 1 ? Completable.concatArray(completableTasks) : completableTasks[0];
			boolean result = completable.timeout(timeoutInSeconds, TimeUnit.SECONDS).blockingAwait(timeoutInSeconds, TimeUnit.SECONDS);

			if (!result) {
				LOGGER.error("Unable to finish launch items on ReportPortal. Timeout exceeded. The data may be lost.");
			}
		} catch (Exception e) {
			LOGGER.error("Unable to finish launch items on ReportPortal", e);
		}
	}

	private Completable completeLogCompletables() {
		Completable items = Completable.merge(logCompletables);
		logCompletables.clear();
		return items;
	}

	/**
	 * Waits for completion of all test items including virtual ones and log emitters.
	 * This method ensures all test results are properly reported to ReportPortal before the launch completes.
	 * It uses the timeout defined in the parameters to prevent indefinite waiting.
	 *
	 * @param itemCompletable A completable representing the test items to be completed before finishing the launch
	 */
	protected void waitForItemsCompletion(@Nullable Completable itemCompletable) {
		List<Completable> completables = new ArrayList<>();
		completables.add(getLaunch().ignoreElement());
		completables.add(createVirtualItemCompletable());
		if (itemCompletable != null) {
			completables.add(itemCompletable);
		}
		completables.add(completeLogCompletables());
		waitForCompletable(completables.toArray(new Completable[0]));
	}

	private void emitLog(@Nonnull final SaveLogRQ rq) {
		SaveLogRQ myRq = clonePojo(rq, SaveLogRQ.class);
		// Ensure file content is not lost during cloning
		ofNullable(myRq.getFile()).ifPresent(file -> file.setContent(rq.getFile().getContent()));
		myRq.setLogTime(convertIfNecessary(myRq.getLogTime()));
		logEmitter.onNext(myRq);
	}

	/**
	 * Finalizes the batched log emitter ensuring the last batch is sent before
	 * completing. Blocks within the configured reporting timeout while waiting for
	 * the final batch to be processed.
	 */
	protected void completeLogEmitter() {
		if (logEmitter.hasComplete()) {
			return;
		}
		// To ensure we sent all logs post one message (for the case when there were no logs at all) and wait for it to be sent
		// Use blocking get, since we are at the end of the flow and Maybe.map(..) will be stopped by system exit
		String launchUUID = getLaunch().blockingGet();
		int logBatchesSent = loggingSubscriber.getProcessedCount();
		emitLog(StaticStructuresUtils.getLastLogRQ(launchUUID));
		logEmitter.onComplete();
		Waiter waiter = new Waiter("Wait for last log batch sent").duration(getParameters().getReportingTimeout(), TimeUnit.SECONDS)
				.pollingEvery(100, TimeUnit.MILLISECONDS);
		waiter.till(() -> logBatchesSent < loggingSubscriber.getProcessedCount() ? true : null);
		waitForCompletable(logEmitter.ignoreElements());
	}

	/**
	 * Finishes launch in ReportPortal. Blocks until all items are reported correctly.
	 *
	 * @param request Launch finish request.
	 */
	public void finish(final FinishExecutionRQ request) {
		if (getExecutor().isShutdown()) {
			LOGGER.error("Unable to finish Launch: executor service is already shut down. The data may be lost.");
			return;
		}

		// Close and re-create statistics service
		getStatisticsService().close();
		statisticsService = new StatisticsService(getParameters());

		// Collect all items to be reported
		Completable finish = null;
		if (!queue.isEmpty()) {
			finish = Completable.concat(queue.values().stream().flatMap(i -> i.getChildren().stream()).collect(Collectors.toList()));
		}
		if (StringUtils.isBlank(getParameters().getLaunchUuid()) || !getParameters().isLaunchUuidCreationSkip()) {
			FinishExecutionRQ rq = clonePojo(request, FinishExecutionRQ.class);
			rq.setEndTime(convertIfNecessary(rq.getEndTime()));
			truncateAttributes(rq);
			Maybe<OperationCompletionRS> launchCompletable = getLaunch().flatMap(id -> getClient().finishLaunch(id, rq)
					.retry(DEFAULT_REQUEST_RETRY)
					.doOnSuccess(LOG_SUCCESS)
					.doOnError(LOG_ERROR));
			finish = ofNullable(finish).map(f -> f.andThen(launchCompletable)).orElse(launchCompletable).ignoreElement().cache();
		}

		// Finish all items
		waitForItemsCompletion(finish);

		// Dispose all collected virtual item disposables
		virtualItemDisposables.removeIf(d -> {
			d.dispose();
			return true;
		});

		completeLogEmitter();
	}

	private static <T> Maybe<T> createErrorResponse(Throwable cause) {
		LOGGER.error(cause.getMessage(), cause);
		return Maybe.error(cause);
	}

	/**
	 * Starts new root test item in ReportPortal asynchronously (non-blocking).
	 *
	 * @param request Item start request.
	 * @return Test Item ID promise.
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
		rq.setStartTime(convertIfNecessary(rq.getStartTime()));
		truncateName(rq);
		truncateAttributes(rq);

		String itemDescription = String.format("root test item [%s] '%s'", rq.getType(), rq.getName());
		final Maybe<String> item = getLaunch().flatMap((Function<String, Maybe<String>>) launchId -> {
			rq.setLaunchUuid(launchId);
			LOGGER.trace("Starting {} in thread: {}", itemDescription, Thread.currentThread().getName());
			return getClient().startTestItem(rq).retry(DEFAULT_REQUEST_RETRY).map(TO_ID);
		}).cache();
		item.subscribeOn(getScheduler()).subscribe(logMaybeResults("Start " + itemDescription));
		queue.getOrCompute(item).addToQueue(item.ignoreElement().onErrorComplete());
		LoggingContext.init(item);

		getStepReporter().setParent(item);
		return item;
	}

	/**
	 * Starts new test item in ReportPortal asynchronously (non-blocking).
	 *
	 * @param parentId Promise of parent item ID.
	 * @param request  Item start request.
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
		rq.setStartTime(convertIfNecessary(rq.getStartTime()));
		truncateName(rq);
		truncateAttributes(rq);

		String itemDescription = String.format("child test item [%s] '%s'", rq.getType(), rq.getName());
		final Maybe<String> item = RxJavaPlugins.onAssembly(Maybe.zip(
				getLaunch(), parentId, (lId, pId) -> {
					rq.setLaunchUuid(lId);
					LOGGER.trace("Starting {} in thread: {}", itemDescription, Thread.currentThread().getName());
					return getClient().startTestItem(pId, rq);
				}
		).flatMap(rs -> rs.retry(DEFAULT_REQUEST_RETRY).map(TO_ID)).cache());

		item.subscribeOn(getScheduler()).subscribe(logMaybeResults("Start " + itemDescription));
		queue.getOrCompute(item).withParent(parentId).addToQueue(item.ignoreElement().onErrorComplete());
		LoggingContext.init(item);

		getStepReporter().setParent(item);
		return item;
	}

	/**
	 * Starts new test item in ReportPortal asynchronously (non-blocking), ensure provided retry item ID starts first. Also sets flags
	 * <code>retry: true</code> and <code>retryOf: {retryOf argument value}</code>.
	 *
	 * @param parentId Promise of parent item ID.
	 * @param retryOf  Previous item ID promise, which is retried.
	 * @param rq       Item start request.
	 * @return Promise of Test Item ID.
	 */
	@Nonnull
	public Maybe<String> startTestItem(final Maybe<String> parentId, final Maybe<String> retryOf, final StartTestItemRQ rq) {
		return retryOf.flatMap((Function<String, Maybe<String>>) s -> {
			StartTestItemRQ myRq = clonePojo(rq, StartTestItemRQ.class);
			myRq.setRetry(true);
			myRq.setRetryOf(s);
			return startTestItem(parentId, myRq);
		}).cache();
	}

	/**
	 * Creates a virtual test item in ReportPortal.
	 * Virtual items are used as temporary placeholders until they are populated with real item IDs.
	 * This is useful for scenarios where item creation order needs to be decoupled from test execution order.
	 *
	 * @return Virtual test item ID promise that will be populated with a real ID later
	 */
	@Nonnull
	public Maybe<String> createVirtualItem() {
		PublishSubject<String> emitter = PublishSubject.create();
		Maybe<String> virtualItem = RxJavaPlugins.onAssembly(emitter.singleElement().cache());
		virtualItems.put(virtualItem, emitter);
		LoggingContext.init(virtualItem);
		return virtualItem;
	}

	/**
	 * Populates a virtual item with a real ID, triggering all subscribers waiting for this ID.
	 *
	 * @param virtualItem Virtual item ID promise.
	 * @param realId      Real ID to populate the virtual item with.
	 */
	private void populateVirtualItem(@Nonnull final Maybe<String> virtualItem, @Nonnull final String realId) {
		PublishSubject<String> emitter = virtualItems.remove(virtualItem);
		if (emitter != null) {
			emitter.onNext(realId);
			emitter.onComplete();
		} else {
			LOGGER.error("Unable to populate virtual item with ID: {}. No emitter found.", realId);
		}
	}

	/**
	 * Populates a virtual item with an error, triggering all subscribers waiting with the error.
	 *
	 * @param virtualItem Virtual item ID promise.
	 * @param cause       Error to populate the virtual item with.
	 */
	private void populateVirtualItem(@Nonnull final Maybe<String> virtualItem, @Nonnull final Throwable cause) {
		PublishSubject<String> emitter = virtualItems.remove(virtualItem);
		if (emitter != null) {
			emitter.onError(cause);
		} else {
			LOGGER.error("Unable to populate virtual item with error. No emitter found.", cause);
		}
	}

	/**
	 * Handles errors for virtual test items, populating the virtual item with the error.
	 *
	 * @param virtualItem Virtual item ID promise.
	 * @param request     Item start request.
	 * @return Error response as Maybe.
	 */
	private Maybe<String> handleVirtualItemError(final Maybe<String> virtualItem, final StartTestItemRQ request) {
		if (virtualItem == null) {
			return createErrorResponse(new NullPointerException("VirtualItem should not be null"));
		} else if (request == null) {
			Maybe<String> error = createErrorResponse(new NullPointerException("StartTestItemRQ should not be null"));
			Disposable errorDisposable = error.subscribe(
					id -> {
					}, e -> populateVirtualItem(virtualItem, e)
			);
			virtualItemDisposables.add(errorDisposable);
			return error;
		} else {
			return null;
		}
	}

	/**
	 * Creates a subscription to handle the item creation process for a virtual item.
	 * When the real item is created successfully, the virtual item is populated with the real ID.
	 * If there's an error creating the real item, the error is propagated to the virtual item.
	 * In both cases, the disposable is added to the virtualItemDisposables collection for later cleanup.
	 *
	 * @param virtualItem Virtual item ID promise to be populated with the real ID or error
	 * @param item        Real Test Item ID promise from the item creation request
	 */
	private void handleVirtualItemSubscription(Maybe<String> virtualItem, Maybe<String> item) {
		Disposable disposable = item.subscribe(
				id -> populateVirtualItem(virtualItem, id), e -> {
					LOGGER.error("Unable to start test item.", e);
					populateVirtualItem(virtualItem, e);
				}
		);
		virtualItemDisposables.add(disposable);
	}

	/**
	 * Starts new test item in ReportPortal asynchronously (non-blocking) and populates the provided virtual item with the real item ID.
	 *
	 * @param virtualItem Virtual item ID promise to populate with real ID.
	 * @param rq          Item start rq.
	 * @return Real Test Item ID promise.
	 */
	@Nonnull
	public Maybe<String> startVirtualTestItem(final Maybe<String> virtualItem, final StartTestItemRQ rq) {
		Maybe<String> error = handleVirtualItemError(virtualItem, rq);
		if (error != null) {
			return error;
		}
		Maybe<String> item = startTestItem(rq);
		handleVirtualItemSubscription(virtualItem, item);
		return item;
	}

	/**
	 * Starts new test item in ReportPortal asynchronously (non-blocking) and populates the provided virtual item with the real item ID.
	 *
	 * @param parentId    Promise of parent item ID.
	 * @param virtualItem Virtual item ID promise to populate with real ID.
	 * @param rq          Item start request.
	 * @return Real Test Item ID promise.
	 */
	@Nonnull
	public Maybe<String> startVirtualTestItem(final Maybe<String> parentId, final Maybe<String> virtualItem, final StartTestItemRQ rq) {
		Maybe<String> error = handleVirtualItemError(virtualItem, rq);
		if (error != null) {
			return error;
		}
		Maybe<String> item = startTestItem(parentId, rq);
		handleVirtualItemSubscription(virtualItem, item);
		return item;
	}

	/**
	 * Lookup for the Issue Type locator in project settings and fill missed external issue fields based on properties.
	 *
	 * @param issue Issue to complete
	 */
	protected void completeIssues(@Nonnull Issue issue) {
		String issueType = issue.getIssueType();
		if (StringUtils.isBlank(issueType)) {
			return;
		}
		ofNullable(projectSettings.blockingGet()).map(ProjectSettingsResource::getSubTypes)
				.ifPresent(subTypes -> subTypes.values().stream().flatMap(List::stream).forEach(value -> {
					if (issueType.equals(value.getLocator())) {
						return;
					}
					if (issueType.equalsIgnoreCase(value.getShortName())) {
						issue.setIssueType(value.getLocator());
						return;
					}
					if (issueType.equalsIgnoreCase(value.getLongName())) {
						issue.setIssueType(value.getLocator());
						return;
					}
					if (issueType.equals(value.getTypeRef())) {
						issue.setIssueType(value.getLocator());
					}
				}));

		if (ofNullable(issue.getExternalSystemIssues()).filter(issues -> !issues.isEmpty()).isEmpty()) {
			return;
		}
		ListenerParameters params = getParameters();
		Optional<String> btsUrl = ofNullable(params.getBtsUrl()).filter(StringUtils::isNotBlank);
		Optional<String> btsProjectId = ofNullable(params.getBtsProjectId()).filter(StringUtils::isNotBlank);
		Optional<String> btsIssueUrl = ofNullable(params.getBtsIssueUrl()).filter(StringUtils::isNotBlank);
		issue.getExternalSystemIssues().stream().filter(Objects::nonNull).forEach(externalIssue -> {
			if (StringUtils.isBlank(externalIssue.getTicketId())) {
				return;
			}
			if (btsUrl.isPresent() && StringUtils.isBlank(externalIssue.getBtsUrl())) {
				externalIssue.setBtsUrl(btsUrl.get());
			}
			if (btsProjectId.isPresent() && StringUtils.isBlank(externalIssue.getBtsProject())) {
				externalIssue.setBtsProject(btsProjectId.get());
			}
			if (btsIssueUrl.isPresent() && StringUtils.isBlank(externalIssue.getUrl())) {
				externalIssue.setUrl(btsIssueUrl.get());
			}
			if (StringUtils.isNotBlank(externalIssue.getUrl())) {
				if (StringUtils.isNotBlank(externalIssue.getTicketId())) {
					externalIssue.setUrl(externalIssue.getUrl().replace("{issue_id}", externalIssue.getTicketId()));
				}
				if (StringUtils.isNotBlank(externalIssue.getBtsProject())) {
					externalIssue.setUrl(externalIssue.getUrl().replace("{bts_project}", externalIssue.getBtsProject()));
				}
			}
		});
	}

	/**
	 * Finishes Test Item in ReportPortal asynchronously (non-blocking). Schedules finish after success of all child items.
	 *
	 * @param item    Item ID promise.
	 * @param request Item finish request.
	 * @return Promise of Test Item finish response.
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
		rq.setEndTime(convertIfNecessary(rq.getEndTime()));
		truncateAttributes(rq);

		//noinspection ReactiveStreamsUnusedPublisher
		getStepReporter().finishPreviousStep(ofNullable(rq.getStatus()).map(ItemStatus::valueOf).orElse(null));

		ItemStatus status = ofNullable(rq.getStatus()).map(ItemStatus::valueOf).orElse(null);
		if (rq.getIssue() == null) {
			if (status == ItemStatus.SKIPPED && !getParameters().getSkippedAnIssue()) {
				rq.setIssue(Launch.NOT_ISSUE);
			}
		} else {
			if (status == ItemStatus.FAILED || (status == ItemStatus.SKIPPED && getParameters().getSkippedAnIssue())) {
				completeIssues(rq.getIssue());
			} else if (status == ItemStatus.PASSED) {
				if (getParameters().isBtsIssueFail()) {
					rq.setStatus(ItemStatus.FAILED.name());
					rq.setIssue(StaticStructuresUtils.REDUNDANT_ISSUE);
				} else {
					rq.setIssue(null);
				}
			}
		}

		LaunchImpl.TreeItem treeItem = queue.get(item);
		if (null == treeItem) {
			treeItem = new LaunchImpl.TreeItem();
			LOGGER.error("Item {} not found in the cache", item);
		}

		if (getStepReporter().isFailed(item)) {
			rq.setStatus(ItemStatus.FAILED.name());
		}

		//wait for the children to complete
		Maybe<OperationCompletionRS> finishResponse = RxJavaPlugins.onAssembly(Maybe.zip(
				this.getLaunch(), item, (launchId, itemId) -> {
					// set launch UUID for the request
					rq.setLaunchUuid(launchId);
					LOGGER.trace("Finishing test item {} in thread: {}", itemId, Thread.currentThread().getName());
					// make the actual call to finish the test item
					return getClient().finishTestItem(itemId, rq)
							.retry(TEST_ITEM_FINISH_REQUEST_RETRY)
							.doOnSuccess(LOG_SUCCESS)
							.doOnError(LOG_ERROR);
				}
		).flatMap(m -> m).cache());

		Completable finishCompletion = Completable.concat(treeItem.getChildren())
				.andThen(finishResponse)
				.doAfterTerminate(() -> queue.remove(item)) //cleanup children
				.ignoreElement()
				.cache()
				.subscribeOn(getScheduler());
		finishCompletion.subscribe(logCompletableResults("Finish test item"));

		//find parent and add to its queue
		final Maybe<String> parent = treeItem.getParent();
		// If parent is not present, we use the launch ID, since the item is a root item
		queue.getOrCompute(Objects.requireNonNullElseGet(parent, this::getLaunch)).addToQueue(finishCompletion.onErrorComplete());

		getStepReporter().removeParent(item);
		LoggingContext.dispose();
		if (rq.hashCode() % LOG_REMOVE_FACTOR == 0) {
			logCompletables.removeIf(c -> c.test().completions() > 0);
		}
		return finishResponse;
	}

	private SaveLogRQ prepareRequest(@Nonnull final SaveLogRQ rq) throws IOException {
		SaveLogRQ.File file = rq.getFile();
		if (getParameters().isConvertImage() && null != file && isImage(file.getContentType())) {
			final TypeAwareByteSource source = convert(ByteSource.wrap(file.getContent()));
			file.setContent(source.read());
			file.setContentType(source.getMediaType());
		}
		return rq;
	}

	private SaveLogRQ prepareRequest(@Nonnull final String launchId, @Nonnull final SaveLogRQ rq) throws IOException {
		rq.setLaunchUuid(launchId);
		return prepareRequest(rq);
	}

	/**
	 * Logs message to the ReportPortal Launch.
	 *
	 * @param rq Log request.
	 */
	@Override
	public void log(@Nonnull final SaveLogRQ rq) {
		Maybe<SaveLogRQ> result = getLaunch().map(launchUuid -> {
			emitLog(prepareRequest(launchUuid, rq));
			return rq;
		}).cache();
		logCompletables.add(result.ignoreElement());
		result.subscribe(SubscriptionUtils.logMaybeResults("Log item"));
	}

	/**
	 * Logs message to the ReportPortal Launch.
	 *
	 * @param logSupplier Log Message Factory. Argument of the function will be actual launch UUID.
	 */
	@Override
	public void log(@Nonnull final java.util.function.Function<String, SaveLogRQ> logSupplier) {
		Maybe<SaveLogRQ> result = getLaunch().map(launchUuid -> {
			SaveLogRQ rq = prepareRequest(logSupplier.apply(launchUuid));
			emitLog(rq);
			return rq;
		}).cache();
		logCompletables.add(result.ignoreElement());
		result.subscribe(SubscriptionUtils.logMaybeResults("Log item"));
	}

	/**
	 * Logs message to the ReportPortal Launch.
	 *
	 * @param logItemUuid Test Item ID promise
	 * @param logSupplier Log Message Factory. Argument of the function will be actual launch UUID.
	 */
	@Override
	public void log(@Nonnull final Maybe<String> logItemUuid, @Nonnull final java.util.function.Function<String, SaveLogRQ> logSupplier) {
		Maybe<SaveLogRQ> result = RxJavaPlugins.onAssembly(Maybe.zip(
				getLaunch(), logItemUuid, (launchUuid, itemUuid) -> {
					SaveLogRQ rq = prepareRequest(launchUuid, logSupplier.apply(itemUuid));
					emitLog(rq);
					return rq;
				}
		).cache());
		logCompletables.add(result.ignoreElement());
		result.subscribe(SubscriptionUtils.logMaybeResults("Log item"));
	}

	/**
	 * Wrapper around TestItem entity to be able to track parent and children items
	 */
	protected static class TreeItem {
		private volatile Maybe<String> parent;
		private final List<Completable> children = new CopyOnWriteArrayList<>();

		/**
		 * Sets the parent item promise for this tree node.
		 *
		 * @param parent the parent item ID promise, may be {@code null} for a root
		 * @return this instance for chaining
		 */
		public LaunchImpl.TreeItem withParent(@Nullable Maybe<String> parent) {
			this.parent = parent;
			return this;
		}

		/**
		 * Adds a completion task representing a child operation to this node.
		 *
		 * @param completable the child completion task to add
		 */
		public void addToQueue(@Nonnull Completable completable) {
			this.children.add(completable);
		}

		/**
		 * Returns a snapshot list of child completion tasks.
		 *
		 * @return a new list containing this node's child completables
		 */
		@Nonnull
		public List<Completable> getChildren() {
			return new ArrayList<>(this.children);
		}

		/**
		 * Returns the parent item ID promise or {@code null} if this is a root.
		 *
		 * @return the parent item ID promise or {@code null}
		 */
		@Nullable
		public Maybe<String> getParent() {
			return parent;
		}
	}

	protected static class ComputationConcurrentHashMap extends ConcurrentHashMap<Maybe<String>, LaunchImpl.TreeItem> {
		/**
		 * Returns an existing tree item by key or creates and stores a new one
		 * atomically if absent.
		 *
		 * @param key the key representing an item ID promise
		 * @return the existing or newly created {@link LaunchImpl.TreeItem}
		 */
		public LaunchImpl.TreeItem getOrCompute(Maybe<String> key) {
			return computeIfAbsent(key, k -> new LaunchImpl.TreeItem());
		}
	}
}

