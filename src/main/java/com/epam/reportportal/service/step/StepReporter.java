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
import io.reactivex.Maybe;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.util.Date;

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
	void setParent(Maybe<String> parentUuid);

	Maybe<String> getParent();

	void removeParent(Maybe<String> parentUuid);

	boolean isFailed(Maybe<String> parentId);

	void sendStep(String name);

	void sendStep(String name, String... logs);

	void sendStep(@NotNull ItemStatus status, String name);

	void sendStep(@NotNull ItemStatus status, String name, String... logs);

	void sendStep(@NotNull ItemStatus status, String name, Throwable throwable);

	void sendStep(String name, File... files);

	void sendStep(@NotNull ItemStatus status, String name, File... files);

	void sendStep(@NotNull ItemStatus status, String name, Throwable throwable, File... files);

	void finishPreviousStep();

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
		public void setParent(Maybe<String> parentUuid) {}
		@Override
		public Maybe<String> getParent() {return Maybe.empty();}
		@Override
		public void removeParent(Maybe<String> parentUuid) {}
		@Override
		public boolean isFailed(Maybe<String> parentId) {return false;}
		@Override
		public void sendStep(String name) {}
		@Override
		public void sendStep(String name, String... logs) {}
		@Override
		public void sendStep(@NotNull ItemStatus status, String name) {}
		@Override
		public void sendStep(@NotNull ItemStatus status, String name, String... logs) {}
		@Override
		public void sendStep(@NotNull ItemStatus status, String name, Throwable throwable) {}
		@Override
		public void sendStep(String name, File... files) {}
		@Override
		public void sendStep(@NotNull ItemStatus status, String name, File... files) {}
		@Override
		public void sendStep(@NotNull ItemStatus status, String name, Throwable throwable, File... files) {}
		@Override
		public void finishPreviousStep() {}
	};
	// @formatter:on
}
