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

import com.epam.reportportal.utils.SubscriptionUtils;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

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
 * @see LoggingContext#init(Maybe)
 */
public class LoggingContext {
	private static final Queue<LoggingContext> USED_CONTEXTS = new ConcurrentLinkedQueue<>();
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
	 * @param itemUuid a Test Item UUID
	 * @return New Logging Context
	 */
	@Nonnull
	public static LoggingContext init(@Nonnull final Maybe<String> itemUuid) {
		LoggingContext context = new LoggingContext(itemUuid);
		createContext().push(context);
		return context;
	}

	/**
	 * Disposes current logging context
	 */
	public static void dispose() {
		ofNullable(getContext()).map(Deque::poll).ifPresent(USED_CONTEXTS::add);
	}

	public static Completable completed() {
		Completable completable = Completable.merge(USED_CONTEXTS.stream().map(LoggingContext::complete).collect(Collectors.toList()));
		Deque<LoggingContext> context = getContext();
		return ofNullable(context).map(ctx -> Completable.concat(Arrays.asList(
				completable,
				Completable.merge(ctx.stream().map(LoggingContext::complete).collect(Collectors.toList()))
		))).orElse(completable);
	}

	/**
	 * Messages queue to track items execution order
	 */
	private final Queue<Completable> completables = new ConcurrentLinkedQueue<>();
	/* a UUID of TestItem in ReportPortal to report into */
	private final Maybe<String> itemUuid;

	LoggingContext(@Nonnull final Maybe<String> itemUuid) {
		this.itemUuid = itemUuid;
	}

	/**
	 * Emits log. Basically, put it into processing pipeline
	 *
	 * @param logItemUuid Test Item ID promise
	 * @param logSupplier Log Message Factory. Key if the function is actual test item ID
	 */
	public void emit(@Nonnull final Maybe<String> logItemUuid, @Nonnull final java.util.function.Function<String, SaveLogRQ> logSupplier) {
		Launch launch = Launch.currentLaunch();
		if (launch == null) {
			return;
		}
		Maybe<String> future = logItemUuid.map(itemUuid -> {
			launch.log(logSupplier.apply(itemUuid));
			return itemUuid;
		});
		completables.add(future.ignoreElement());
		future.subscribe(SubscriptionUtils.logMaybeResults("LoggingContext"));
	}

	/**
	 * Emits log. Basically, put it into processing pipeline
	 *
	 * @param logSupplier Log Message Factory. Key if the function is actual test item ID
	 */
	public void emit(@Nonnull final java.util.function.Function<String, SaveLogRQ> logSupplier) {
		emit(itemUuid, logSupplier);
	}

	/**
	 * Complete the context.
	 *
	 * @return the instance will be completed when all logs are sent
	 */
	public Completable complete() {
		Completable completable = Completable.merge(completables);
		completables.clear();
		return completable;
	}
}
