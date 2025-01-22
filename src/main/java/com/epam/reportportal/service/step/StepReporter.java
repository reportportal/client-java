/*
 *  Copyright 2020 EPAM Systems
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.epam.reportportal.service.step;

import com.epam.reportportal.listeners.ItemStatus;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.function.Supplier;

/**
 * A class for manual nested step reporting.
 * <p>
 * Usage:
 * <code>
 * StepReporter stepReporter = Launch.currentLaunch().getStepReporter();
 * stepReporter.sendStep("My step name");
 * // step actions
 * stepReporter.sendStep(ItemStatus.FAILED, "My failure step name", new File("screenshot/fail.jpg"));
 * </code>
 */
public interface StepReporter {

	void setParent(@Nullable Maybe<String> parentUuid);

	@Nullable
	Maybe<String> getParent();

	void removeParent(@Nullable Maybe<String> parentUuid);

	boolean isFailed(@Nullable Maybe<String> parentId);

	@Nonnull
	Maybe<String> sendStep(@Nonnull String name);

	@Nonnull
	Maybe<String> sendStep(@Nonnull String name, @Nullable String... logs);

	@Nonnull
	Maybe<String> sendStep(@Nonnull ItemStatus status, @Nonnull String name);

	@Nonnull
	Maybe<String> sendStep(@Nonnull ItemStatus status, @Nonnull String name, @Nullable String... logs);

	@Nonnull
	Maybe<String> sendStep(@Nonnull ItemStatus status, @Nonnull String name, @Nullable Throwable throwable);

	@Nonnull
	Maybe<String> sendStep(@Nonnull String name, @Nullable File... files);

	@Nonnull
	Maybe<String> sendStep(@Nonnull ItemStatus status, @Nonnull String name, @Nullable File... files);

	@Nonnull
	Maybe<String> sendStep(@Nonnull ItemStatus status, @Nonnull String name, @Nullable Throwable throwable, @Nullable File... files);

	@Nonnull
	Maybe<String> finishPreviousStep(@Nullable ItemStatus status);

	@Nonnull
	Maybe<String> finishPreviousStep();

	@Nonnull
	Maybe<String> startNestedStep(@Nonnull StartTestItemRQ startStepRequest);

	@Nonnull
	Maybe<OperationCompletionRS> finishNestedStep(@Nonnull ItemStatus status);

	@Nonnull
	Maybe<OperationCompletionRS> finishNestedStep();

	@Nonnull
	Maybe<OperationCompletionRS> finishNestedStep(@Nullable Throwable throwable);

	@Nonnull
	Maybe<OperationCompletionRS> finishNestedStep(@Nonnull FinishTestItemRQ finishStepRequest);

	/**
	 * Report a step with specified name.
	 *
	 * @param name step name
	 * @return step ID
	 */
	@Nonnull
	Maybe<String> step(@Nonnull String name);

	/**
	 * Report a step with specified status and name.
	 *
	 * @param status step status
	 * @param name   step name
	 * @return step ID
	 */
	@Nonnull
	Maybe<String> step(@Nonnull ItemStatus status, @Nonnull String name);

	/**
	 * Wrap passed actions as a separate step and report it.
	 *
	 * @param stepSuccessStatus step status in case of graceful finish
	 * @param name              step name
	 * @param actions           action function to execute
	 * @param <T>               return type
	 * @return actions result
	 */
	@Nullable
	<T> T step(@Nonnull ItemStatus stepSuccessStatus, @Nonnull String name, @Nonnull Supplier<T> actions);

	/**
	 * Wrap passed actions as a separate step and report it.
	 *
	 * @param name    step name
	 * @param actions action function to execute
	 * @param <T>     return type
	 * @return actions result
	 */
	@Nullable
	<T> T step(@Nonnull String name, @Nonnull Supplier<T> actions);

	/**
	 * Set execution status for current step. This status will be applied to the step no matter what will happen with
	 * the step next. E.G. if you set {@link ItemStatus#PASSED} with the method and then throw an exception the status
	 * of the step will stay passed.
	 *
	 * @param status wanted step status
	 */
	default void setStepStatus(@Nonnull ItemStatus status) {
	}

	class StepEntry {
		private final Maybe<String> itemId;
		private final Date timestamp;
		private final FinishTestItemRQ finishTestItemRQ;

		public StepEntry(@Nonnull Maybe<String> itemId, @Nonnull Date stepStartTime, @Nonnull FinishTestItemRQ finishTestItemRQ) {
			this.itemId = itemId;
			this.timestamp = stepStartTime;
			this.finishTestItemRQ = finishTestItemRQ;
		}

		public StepEntry(@Nonnull Maybe<String> itemId, @Nonnull FinishTestItemRQ finishTestItemRQ) {
			this(itemId, Calendar.getInstance().getTime(), finishTestItemRQ);
		}

		@Nonnull
		public Maybe<String> getItemId() {
			return itemId;
		}

		@Nonnull
		public Date getTimestamp() {
			return timestamp;
		}

		@Nonnull
		public FinishTestItemRQ getFinishTestItemRQ() {
			return finishTestItemRQ;
		}
	}

	/**
	 * A StepReporter which does nothing, specially for disabled RP listeners.
	 */
	// @formatter:off
	StepReporter NOOP_STEP_REPORTER = new StepReporter() {
		@Override
		public void setParent(@Nullable Maybe<String> parentUuid) {}
		@Override
		@Nullable
		public Maybe<String> getParent() {return null;}
		@Override
		public void removeParent(@Nullable Maybe<String> parentUuid) {}
		@Override
		public boolean isFailed(@Nullable Maybe<String> parentId) {return false;}
		@Override
		@Nonnull
		public Maybe<String> sendStep(@Nonnull String name) {
			return Maybe.empty();
		}
		@Override
		@Nonnull
		public Maybe<String> sendStep(@Nonnull String name, @Nullable String... logs) {
			return Maybe.empty();
		}
		@Override
		@Nonnull
		public Maybe<String> sendStep(@Nonnull ItemStatus status, @Nonnull String name) {
			return Maybe.empty();
		}
		@Override
		@Nonnull
		public Maybe<String> sendStep(@Nonnull ItemStatus status, @Nonnull String name, @Nullable String... logs) {
			return Maybe.empty();
		}
		@Override
		@Nonnull
		public Maybe<String> sendStep(@Nonnull ItemStatus status, @Nonnull String name, @Nullable Throwable throwable) {
			return Maybe.empty();
		}
		@Override
		@Nonnull
		public Maybe<String> sendStep(@Nonnull String name, @Nullable File... files) {
			return Maybe.empty();
		}
		@Override
		@Nonnull
		public Maybe<String> sendStep(@Nonnull ItemStatus status, @Nonnull String name, @Nullable File... files) {
			return Maybe.empty();
		}
		@Override
		@Nonnull
		public Maybe<String> sendStep(@Nonnull ItemStatus status, @Nonnull String name, @Nullable Throwable throwable, @Nullable File... files) {
			return Maybe.empty();
		}
		@Override
		@Nonnull
		public Maybe<String> finishPreviousStep(@Nullable ItemStatus status) {return Maybe.empty();}
		@Override
		@Nonnull
		public Maybe<String> finishPreviousStep() {return Maybe.empty();}
		@Override
		@Nonnull
		public Maybe<String> startNestedStep(@Nonnull StartTestItemRQ startStepRequest) {return Maybe.empty();}
		@Override
		@Nonnull
		public Maybe<OperationCompletionRS> finishNestedStep(@Nonnull ItemStatus status) {return Maybe.empty();}
		@Override
		@Nonnull
		public Maybe<OperationCompletionRS> finishNestedStep() {return Maybe.empty();}
		@Override
		@Nonnull
		public Maybe<OperationCompletionRS> finishNestedStep(@Nullable Throwable throwable) {return Maybe.empty();}
		@Override
		@Nonnull
		public Maybe<OperationCompletionRS> finishNestedStep(@Nonnull FinishTestItemRQ finishStepRequest) {return Maybe.empty();}
		@Override
		@Nonnull
		public Maybe<String> step(@Nonnull String name) {
			return Maybe.empty();
		}
		@Override
		@Nonnull
		public Maybe<String> step(@Nonnull ItemStatus status, @Nonnull String name) {
			return Maybe.empty();
		}
		@Override
		@Nullable
		public <T> T step(@Nonnull ItemStatus stepSuccessStatus, @Nonnull String name, @Nonnull Supplier<T> actions) {return null;}
		@Override
		@Nullable
		public <T> T step(@Nonnull String name, @Nonnull Supplier<T> actions) {return null;}
	};
	// @formatter:on
}
