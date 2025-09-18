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

package com.epam.reportportal.utils;

import com.epam.reportportal.listeners.ItemStatus;
import jakarta.annotation.Nullable;

public class StatusEvaluation {

	private StatusEvaluation() {
		throw new IllegalStateException("Static only class");
	}

	/**
	 * Calculate an Item status according to its child item status and current status. E.G.: SUITE-TEST or TEST-STEP.
	 * <p>
	 * Example 1:
	 * - Current status: {@link ItemStatus#FAILED}
	 * - Child item status: {@link ItemStatus#SKIPPED}
	 * Result: {@link ItemStatus#FAILED}
	 * <p>
	 * Example 2:
	 * - Current status: {@link ItemStatus#PASSED}
	 * - Child item status: {@link ItemStatus#SKIPPED}
	 * Result: {@link ItemStatus#PASSED}
	 * <p>
	 * Example 3:
	 * - Current status: {@link ItemStatus#PASSED}
	 * - Child item status: {@link ItemStatus#FAILED}
	 * Result: {@link ItemStatus#FAILED}
	 * <p>
	 * Example 4:
	 * - Current status: {@link ItemStatus#SKIPPED}
	 * - Child item status: {@link ItemStatus#FAILED}
	 * Result: {@link ItemStatus#FAILED}
	 *
	 * @param currentStatus an Item status
	 * @param childStatus   a status of its child element
	 * @return new status
	 */
	@Nullable
	public static ItemStatus evaluateStatus(@Nullable final ItemStatus currentStatus, @Nullable final ItemStatus childStatus) {
		if (childStatus == null) {
			return currentStatus;
		}
		if (currentStatus == null) {
			return childStatus;
		}
		switch (childStatus) {
			case PASSED:
				switch (currentStatus) {
					case SKIPPED:
					case STOPPED:
					case INFO:
					case WARN:
						return childStatus;
					default:
						return currentStatus;
				}
			case SKIPPED:
			case STOPPED:
			case INFO:
			case WARN:
				return currentStatus;
			case CANCELLED:
				switch (currentStatus) {
					case PASSED:
					case SKIPPED:
					case STOPPED:
					case INFO:
					case WARN:
						return ItemStatus.CANCELLED;
					default:
						return currentStatus;
				}
			case INTERRUPTED:
				switch (currentStatus) {
					case PASSED:
					case SKIPPED:
					case STOPPED:
					case INFO:
					case WARN:
					case CANCELLED:
						return ItemStatus.INTERRUPTED;
					default:
						return currentStatus;
				}
			default:
				return childStatus;
		}
	}
}
