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
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Maybe;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.Calendar;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import static com.google.common.base.Throwables.getStackTraceAsString;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
@Aspect
public class StepAspect {
	private static final ConcurrentHashMap<Launch, Deque<Maybe<String>>> stepStack = new ConcurrentHashMap<>();

	@Pointcut("@annotation(step)")
	public void withStepAnnotation(Step step) {

	}

	@Pointcut("execution(* *.*(..))")
	public void anyMethod() {

	}

	@Before(value = "anyMethod() && withStepAnnotation(step)", argNames = "joinPoint,step")
	public void startNestedStep(JoinPoint joinPoint, Step step) {
		if (!step.isIgnored()) {
			MethodSignature signature = (MethodSignature) joinPoint.getSignature();

			Launch launch = Launch.currentLaunch();
			Deque<Maybe<String>> steps = stepStack.computeIfAbsent(launch, l -> new ConcurrentLinkedDeque<>());
			Maybe<String> parent = steps.peek();
			if (parent == null) {
				return;
			}

			StartTestItemRQ startStepRequest = StepRequestUtils.buildStartStepRequest(signature, step, joinPoint);
			Maybe<String> stepMaybe = launch.startTestItem(parent, startStepRequest);
			steps.push(stepMaybe);
		}
	}

	@AfterReturning(value = "anyMethod() && withStepAnnotation(step)", argNames = "step")
	public void finishNestedStep(Step step) {
		if (!step.isIgnored()) {
			Launch launch = Launch.currentLaunch();
			Deque<Maybe<String>> steps = stepStack.computeIfAbsent(launch, l -> new ConcurrentLinkedDeque<>());
			Maybe<String> stepId = steps.poll();
			if (stepId == null) {
				return;
			}

			FinishTestItemRQ finishStepRequest = StepRequestUtils.buildFinishStepRequest(ItemStatus.PASSED,
					Calendar.getInstance().getTime()
			);
			launch.finishTestItem(stepId, finishStepRequest);
		}
	}

	@AfterThrowing(value = "anyMethod() && withStepAnnotation(step)", throwing = "throwable", argNames = "step,throwable")
	public void failedNestedStep(Step step, final Throwable throwable) {

		if (!step.isIgnored()) {
			Launch launch = Launch.currentLaunch();
			Deque<Maybe<String>> steps = stepStack.computeIfAbsent(launch, l -> new ConcurrentLinkedDeque<>());
			Maybe<String> stepId = steps.poll();
			if (stepId == null) {
				return;
			}

			ReportPortal.emitLog(itemUuid -> {
				SaveLogRQ rq = new SaveLogRQ();
				rq.setItemUuid(itemUuid);
				rq.setLevel("ERROR");
				rq.setLogTime(Calendar.getInstance().getTime());
				if (throwable != null) {
					rq.setMessage(getStackTraceAsString(throwable));
				} else {
					rq.setMessage("Test has failed without exception");
				}
				rq.setLogTime(Calendar.getInstance().getTime());

				return rq;
			});

			FinishTestItemRQ finishStepRequest = StepRequestUtils.buildFinishStepRequest(ItemStatus.FAILED,
					Calendar.getInstance().getTime()
			);

			while (stepId != null) {
				launch.finishTestItem(stepId, finishStepRequest);
				stepId = steps.poll();
			}
		}
	}

	public static void setParentId(Launch launch, Maybe<String> parent) {
		stepStack.computeIfAbsent(launch, l -> new ConcurrentLinkedDeque<>()).push(parent);
	}
}
