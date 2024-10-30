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
import com.epam.reportportal.service.step.DefaultStepReporter;
import com.epam.reportportal.service.step.StepReporter;
import com.epam.ta.reportportal.ws.reporting.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.reporting.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.reporting.Issue;
import com.epam.ta.reportportal.ws.reporting.Issue.ExternalSystemIssue;
import com.epam.ta.reportportal.ws.reporting.OperationCompletionRS;
import com.epam.ta.reportportal.ws.reporting.StartTestItemRQ;
import io.reactivex.Maybe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Proxy;

import static java.util.Objects.requireNonNull;

/**
 * Launch object represents a lifecycle of a Launch on ReportPortal. It is used to manipulate its state: starting,
 * finishing, adding test results.
 */
public abstract class Launch {
	private static final ThreadLocal<Launch> CURRENT_LAUNCH = new InheritableThreadLocal<>();

	static final Logger LOGGER = LoggerFactory.getLogger(Launch.class);

	private final ListenerParameters parameters;

	private final StepReporter stepReporter;

	protected final ReportPortalClient client;

	Launch(@Nonnull ReportPortalClient reportPortalClient, @Nonnull ListenerParameters listenerParameters, @Nonnull StepReporter reporter) {
		parameters = requireNonNull(listenerParameters, "ListenerParameters shouldn't be NULL");
		stepReporter = requireNonNull(reporter, "StepReporter shouldn't be NULL");
		CURRENT_LAUNCH.set(this);
		client = reportPortalClient;
	}

	Launch(@Nonnull ReportPortalClient reportPortalClient, @Nonnull ListenerParameters listenerParameters) {
		parameters = requireNonNull(listenerParameters, "ListenerParameters shouldn't be NULL");
		stepReporter = new DefaultStepReporter(this);
		CURRENT_LAUNCH.set(this);
		client = requireNonNull(reportPortalClient, "ReportPortalClient shouldn't be NULL");
	}

	@Nonnull
	abstract public Maybe<String> start();

	/**
	 * Finishes launch in ReportPortal. Blocks until all items are reported correctly
	 *
	 * @param rq Finish RQ
	 */
	abstract public void finish(final FinishExecutionRQ rq);

	/**
	 * Starts new test item in ReportPortal asynchronously (non-blocking)
	 *
	 * @param rq Start RQ
	 * @return Test Item ID promise
	 */
	@Nonnull
	abstract public Maybe<String> startTestItem(final StartTestItemRQ rq);

	/**
	 * Starts new test item in ReportPortal asynchronously (non-blocking)
	 *
	 * @param rq       Start RQ
	 * @param parentId Parent ID
	 * @return Test Item ID promise
	 */
	@Nonnull
	abstract public Maybe<String> startTestItem(final Maybe<String> parentId, final StartTestItemRQ rq);

	/**
	 * Starts new test item in ReportPortal in respect of provided retry item ID.
	 *
	 * @param parentId promise of ID of parent
	 * @param retryOf  previous item ID promise
	 * @param rq       promise of ID of request
	 * @return Promise of Test Item ID
	 */
	@Nonnull
	abstract public Maybe<String> startTestItem(final Maybe<String> parentId, final Maybe<String> retryOf, final StartTestItemRQ rq);

	/**
	 * Finishes Test Item in ReportPortal. Non-blocking. Schedules finish after success of all child items
	 *
	 * @param itemId Item ID promise
	 * @param rq     Finish request
	 * @return Promise of Test Item finish response
	 */
	@Nonnull
	abstract public Maybe<OperationCompletionRS> finishTestItem(Maybe<String> itemId, final FinishTestItemRQ rq);

	@Nonnull
	public ListenerParameters getParameters() {
		// Sticking any thread which makes this call to the current Launch to be able to use Step Reporter and other methods
		CURRENT_LAUNCH.set(this);
		return parameters;
	}

	/**
	 * Returns a current launch in a link to the current thread.
	 *
	 * @return launch instance
	 */
	@Nullable
	public static Launch currentLaunch() {
		return CURRENT_LAUNCH.get();
	}

	/**
	 * Returns Nested Step reporter for the current launch.
	 *
	 * @return a {@link StepReporter} instance
	 */
	@Nonnull
	public StepReporter getStepReporter() {
		return stepReporter;
	}

	/**
	 * Returns ReportPortal client for the launch.
	 *
	 * @return a {@link ReportPortalClient} instance
	 */
	@Nonnull
	public ReportPortalClient getClient() {
		return client;
	}

	/**
	 * Returns current launch UUID {@link Maybe}, empty if the launch is not started.
	 *
	 * @return Launch UUID promise
	 */
	@Nonnull
	public abstract Maybe<String> getLaunch();

	/**
	 * Implementation for disabled Reporting
	 */
	public static final Launch NOOP_LAUNCH = new Launch((ReportPortalClient) Proxy.newProxyInstance(Launch.class.getClassLoader(),
			new Class[] { ReportPortalClient.class },
			new DummyReportPortalClientHandler()
	),
			new ListenerParameters(),
			StepReporter.NOOP_STEP_REPORTER
	) {
		@Override
		@Nonnull
		public Maybe<String> start() {
			return Maybe.empty();
		}

		@Override
		public void finish(FinishExecutionRQ rq) {

		}

		@Override
		@Nonnull
		public Maybe<String> startTestItem(StartTestItemRQ rq) {
			return Maybe.empty();
		}

		@Override
		@Nonnull
		public Maybe<String> startTestItem(Maybe<String> parentId, StartTestItemRQ rq) {
			return Maybe.empty();
		}

		@Override
		@Nonnull
		public Maybe<String> startTestItem(Maybe<String> parentId, Maybe<String> retryOf, StartTestItemRQ rq) {
			return Maybe.empty();
		}

		@Override
		@Nonnull
		public Maybe<OperationCompletionRS> finishTestItem(Maybe<String> itemId, FinishTestItemRQ rq) {
			return Maybe.empty();
		}

		@Nonnull
		@Override
		public Maybe<String> getLaunch() {
			return Maybe.empty();
		}
	};

	/**
	 * An Issue to remove 'To Investigate' mark from a skipped/failed Test Item
	 */
	public static final Issue NOT_ISSUE = StaticStructuresUtils.NOT_ISSUE;
}
