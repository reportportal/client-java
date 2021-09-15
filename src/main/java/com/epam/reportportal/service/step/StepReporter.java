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
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
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

	void sendStep(@Nonnull String name);

	void sendStep(@Nonnull String name, @Nullable String... logs);

	void sendStep(@Nonnull ItemStatus status, @Nonnull String name);

	void sendStep(@Nonnull ItemStatus status, @Nonnull String name, @Nullable String... logs);

	void sendStep(@Nonnull ItemStatus status, @Nonnull String name, @Nullable Throwable throwable);

	void sendStep(@Nonnull String name, @Nullable File... files);

	void sendStep(@Nonnull ItemStatus status, @Nonnull String name, @Nullable File... files);

	void sendStep(@Nonnull ItemStatus status, @Nonnull String name, @Nullable Throwable throwable, @Nullable File... files);

	void finishPreviousStep(@Nullable ItemStatus status);

	void finishPreviousStep();

	@Nonnull
	Maybe<String> startNestedStep(@Nonnull StartTestItemRQ startStepRequest);

	void finishNestedStep();

	void finishNestedStep(@Nullable Throwable throwable);

	/**
	 * Report a step with specified name.
	 *
	 * @param name step name
	 */
	void step(@Nonnull String name);

	/**
	 * Report a step with specified status and name.
	 *
	 * @param status step status
	 * @param name   step name
	 */
	void step(@Nonnull ItemStatus status, @Nonnull String name);

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

	class StepEntry {
		private final Maybe<String> itemId;
		private final Date timestamp;
		private final FinishTestItemRQ finishTestItemRQ;

		public StepEntry(Maybe<String> itemId, Date timestamp, FinishTestItemRQ finishTestItemRQ) {
			this.itemId = itemId;
			this.timestamp = timestamp;
			this.finishTestItemRQ = finishTestItemRQ;
		}

		public Maybe<String> getItemId() {
			return itemId;
		}

		public Date getTimestamp() {
			return timestamp;
		}

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
		public Maybe<String> getParent() {return Maybe.empty();}
		@Override
		public void removeParent(@Nullable Maybe<String> parentUuid) {}
		@Override
		public boolean isFailed(@Nullable Maybe<String> parentId) {return false;}
		@Override
		public void sendStep(@Nonnull String name) {}
		@Override
		public void sendStep(@Nonnull String name, @Nullable String... logs) {}
		@Override
		public void sendStep(@Nonnull ItemStatus status, @Nonnull String name) {}
		@Override
		public void sendStep(@Nonnull ItemStatus status, @Nonnull String name, @Nullable String... logs) {}
		@Override
		public void sendStep(@Nonnull ItemStatus status, @Nonnull String name, @Nullable Throwable throwable) {}
		@Override
		public void sendStep(@Nonnull String name, @Nullable File... files) {}
		@Override
		public void sendStep(@Nonnull ItemStatus status, @Nonnull String name, @Nullable File... files) {}
		@Override
		public void sendStep(@Nonnull ItemStatus status, @Nonnull String name, @Nullable Throwable throwable, @Nullable File... files) {}
		@Override
		public void finishPreviousStep(@Nullable ItemStatus status) {}
		@Override
		public void finishPreviousStep() {}
		@Override
		@Nonnull
		public Maybe<String> startNestedStep(@Nonnull StartTestItemRQ startStepRequest) {return Maybe.empty();}
		@Override
		public void finishNestedStep() {}
		@Override
		public void finishNestedStep(@Nullable Throwable throwable) {}
		@Override
		public void step(@Nonnull String name) {}
		@Override
		public void step(@Nonnull ItemStatus status, @Nonnull String name) {}
		@Override
		@Nullable
		public <T> T step(@Nonnull String name, @Nonnull Supplier<T> actions) {return null;}
	};
	// @formatter:on
}
