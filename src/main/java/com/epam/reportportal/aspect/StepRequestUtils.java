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

package com.epam.reportportal.aspect;

import com.epam.reportportal.annotations.Step;
import com.epam.reportportal.annotations.UniqueID;
import com.epam.reportportal.aspect.StepNameUtils;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.Calendar;
import java.util.Date;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
class StepRequestUtils {

	private StepRequestUtils() {
		//static only
	}

	static StartTestItemRQ buildStartStepRequest(MethodSignature signature, Step step, JoinPoint joinPoint) {
		UniqueID uniqueIdAnnotation = signature.getMethod().getAnnotation(UniqueID.class);
		String uniqueId = uniqueIdAnnotation != null ? uniqueIdAnnotation.value() : null;
		String name = StepNameUtils.getStepName(step, signature, joinPoint);

		StartTestItemRQ request = new StartTestItemRQ();
		if (uniqueId != null && !uniqueId.trim().isEmpty()) {
			request.setUniqueId(uniqueId);
		}
		if (!step.description().isEmpty()) {
			request.setDescription(step.description());
		}
		request.setName(name);
		request.setStartTime(Calendar.getInstance().getTime());
		request.setType("STEP");
		request.setHasStats(false);

		return request;
	}

	static FinishTestItemRQ buildFinishStepRequest(String status, Date endTime) {
		FinishTestItemRQ rq = new FinishTestItemRQ();
		rq.setEndTime(endTime);
		rq.setStatus(status);
		return rq;
	}
}
