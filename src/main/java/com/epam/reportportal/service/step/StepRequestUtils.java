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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Calendar;
import java.util.Date;

import static java.util.Optional.ofNullable;

public class StepRequestUtils {

	private StepRequestUtils() {
		//static only
	}

	public static StartTestItemRQ buildStartStepRequest(@Nonnull String name, @Nullable String description) {
		StartTestItemRQ request = new StartTestItemRQ();
		ofNullable(description).filter(d -> !d.isEmpty()).ifPresent(request::setDescription);
		request.setName(name);
		request.setStartTime(Calendar.getInstance().getTime());
		request.setType("STEP");
		request.setHasStats(false);
		return request;
	}

	public static FinishTestItemRQ buildFinishTestItemRequest(@Nonnull ItemStatus status, @Nonnull Date endTime) {
		FinishTestItemRQ finishTestItemRQ = new FinishTestItemRQ();
		finishTestItemRQ.setStatus(status.name());
		finishTestItemRQ.setEndTime(endTime);
		return finishTestItemRQ;
	}

	public static FinishTestItemRQ buildFinishTestItemRequest(@Nonnull ItemStatus status) {
		return buildFinishTestItemRequest(status, Calendar.getInstance().getTime());
	}
}
