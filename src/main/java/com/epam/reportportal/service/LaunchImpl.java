package com.epam.reportportal.service;

import com.epam.reportportal.exception.ReportPortalException;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.utils.LaunchFile;
import com.epam.reportportal.utils.RetryWithDelay;
import com.epam.ta.reportportal.ws.model.*;
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
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static com.epam.reportportal.service.LoggingCallback.*;
import static com.google.common.collect.Lists.newArrayList;

public class LaunchImpl extends Launch {

	private static final Function<EntryCreatedRS, String> TO_ID = new Function<EntryCreatedRS, String>() {
		@Override
		public String apply(EntryCreatedRS rs) throws Exception {
			return rs.getId();
		}
	};
	private static final int ITEM_FINISH_MAX_RETRIES = 10;
	private static final int ITEM_FINISH_RETRY_TIMEOUT = 10;

	/**
	 * REST Client
	 */
	private final ReportPortalClient rpClient;

	/**
	 * Messages queue to track items execution order
	 */
	private final LoadingCache<Maybe<String>, LaunchImpl.TreeItem> QUEUE = CacheBuilder.newBuilder()
			.build(new CacheLoader<Maybe<String>, LaunchImpl.TreeItem>() {
				@Override
				public LaunchImpl.TreeItem load(Maybe<String> key) throws Exception {
					return new LaunchImpl.TreeItem();
				}
			});

	private Maybe<String> launch;
	private Maybe<LaunchFile> launchFile;

	LaunchImpl(ReportPortalClient rpClient, ListenerParameters parameters) {
		super(parameters);
		this.rpClient = Preconditions.checkNotNull(rpClient, "RestEndpoint shouldn't be NULL");
		Preconditions.checkNotNull(parameters, "Parameters shouldn't be NULL");
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
				.map(TO_ID)
				.doOnSuccess(new Consumer<String>() {
					@Override
					public void accept(String id) throws Exception {
						System.setProperty("rp.launch.id", id);
					}
				})
				.cache();
		this.launch.subscribeOn(Schedulers.computation()).subscribe();
		this.launchFile = LaunchFile.create(this.launch);
		return launch;
	}

	/**
	 * Provides ability to report in already started launch
	 *
	 * @param launch Launch to be used
	 * @return Launch to be used
	 */
	public Maybe<String> useLaunch(final Maybe<String> launch) {
		this.launch = launch;
		this.launch.subscribeOn(Schedulers.computation()).subscribe();
		return launch;
	}

	/**
	 * Finishes launch in ReportPortal. Blocks until all items are reported correctly
	 *
	 * @param rq Finish RQ
	 */
	public void finish(final FinishExecutionRQ rq) {
		final Maybe<?> finish = Completable.concat(QUEUE.getUnchecked(this.launch).getChildren())
				.andThen(this.launch.flatMap(new Function<String, Maybe<OperationCompletionRS>>() {
					@Override
					public Maybe<OperationCompletionRS> apply(String id) throws Exception {
						return rpClient.finishLaunch(id, rq).doOnSuccess(LOG_SUCCESS).doOnError(LOG_ERROR);
					}
				}))
				.ignoreElement()
				.andThen(launchFile.doOnSuccess(new Consumer<LaunchFile>() {
					@Override
					public void accept(LaunchFile launchFile) throws Exception {
						launchFile.remove();
					}
				}))
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
	public Maybe<String> startTestItem(final StartTestItemRQ rq) {
		final Maybe<String> testItem = this.launch.flatMap(new Function<String, Maybe<String>>() {
			@Override
			public Maybe<String> apply(String id) throws Exception {
				rq.setLaunchId(id);
				return rpClient.startTestItem(rq).doOnSuccess(logCreated("item")).doOnError(LOG_ERROR).map(TO_ID);

			}
		}).cache();
		testItem.subscribeOn(Schedulers.computation()).subscribe();
		QUEUE.getUnchecked(testItem).addToQueue(testItem.ignoreElement());
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
						return rpClient.startTestItem(parentId, rq).doOnSuccess(logCreated("item")).doOnError(LOG_ERROR).map(TO_ID);
					}
				});
			}
		}).cache();
		itemId.subscribeOn(Schedulers.computation()).subscribe();
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
	public void finishTestItem(final Maybe<String> itemId, final FinishTestItemRQ rq) {
		Preconditions.checkArgument(null != itemId, "ItemID should not be null");

		QUEUE.getUnchecked(launch).addToQueue(LoggingContext.complete());

		LaunchImpl.TreeItem treeItem = QUEUE.getIfPresent(itemId);
		if (null == treeItem) {
			treeItem = new LaunchImpl.TreeItem();
			LOGGER.error("Item {} not found in the cache", itemId);
		}

		//wait for the children to complete
		final Completable finishCompletion = Completable.concat(treeItem.getChildren())
				.andThen(itemId.flatMap(new Function<String, Maybe<OperationCompletionRS>>() {
					@Override
					public Maybe<OperationCompletionRS> apply(String itemId) throws Exception {
						return rpClient.finishTestItem(itemId, rq)
								.retry(new RetryWithDelay(new Predicate<Throwable>() {
									@Override
									public boolean test(Throwable throwable) throws Exception {
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
					public void accept(OperationCompletionRS operationCompletionRS) throws Exception {
						//cleanup children
						QUEUE.invalidate(itemId);
					}
				})
				.ignoreElement()
				.cache();
		finishCompletion.subscribeOn(Schedulers.computation()).subscribe();
		//find parent and add to its queue
		final Maybe<String> parent = treeItem.getParent();
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
		private Maybe<String> parent;
		private List<Completable> children = new CopyOnWriteArrayList<Completable>();

		synchronized LaunchImpl.TreeItem withParent(Maybe<String> parent) {
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

		synchronized Maybe<String> getParent() {
			return parent;
		}
	}
}
