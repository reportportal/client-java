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

package com.epam.reportportal.service.step;

import com.epam.reportportal.listeners.ItemStatus;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import static java.util.Optional.ofNullable;

public class StepRequestUtils {

	private StepRequestUtils() {
		//static only
	}

	@Nonnull
	public static StartTestItemRQ buildStartStepRequest(@Nonnull String name, @Nullable String description,
			@Nonnull Comparable<? extends Comparable<?>> dateTime) {
		StartTestItemRQ request = new StartTestItemRQ();
		ofNullable(description).filter(d -> !d.isEmpty()).ifPresent(request::setDescription);
		request.setName(name);
		request.setStartTime(dateTime);
		request.setType("STEP");
		request.setHasStats(false);
		return request;
	}

	@Nonnull
	public static FinishTestItemRQ buildFinishTestItemRequest(@Nonnull ItemStatus status,
			@Nonnull Comparable<? extends Comparable<?>> endTime) {
		FinishTestItemRQ finishTestItemRQ = new FinishTestItemRQ();
		finishTestItemRQ.setStatus(status.name());
		finishTestItemRQ.setEndTime(endTime);
		return finishTestItemRQ;
	}
}
