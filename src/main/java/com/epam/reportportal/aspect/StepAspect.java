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
import com.epam.reportportal.aspect.util.StepRequestUtils;
import com.epam.reportportal.listeners.Statuses;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.issue.Issue;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import com.google.common.base.Function;
import com.google.common.collect.Queues;
import io.reactivex.Maybe;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.*;

import static com.epam.reportportal.service.LaunchImpl.NOT_ISSUE;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Throwables.getStackTraceAsString;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
@Aspect
public class StepAspect {

	private static InheritableThreadLocal<String> currentLaunchId = new InheritableThreadLocal<String>();

	private static InheritableThreadLocal<Map<String, Launch>> launchMap = new InheritableThreadLocal<Map<String, Launch>>() {
		@Override
		protected Map<String, Launch> initialValue() {
			return new HashMap<String, Launch>();
		}
	};

	private static InheritableThreadLocal<Deque<Maybe<String>>> stepStack = new InheritableThreadLocal<Deque<Maybe<String>>>() {
		@Override
		protected Deque<Maybe<String>> initialValue() {
			return Queues.newArrayDeque();
		}
	};

	private static InheritableThreadLocal<Maybe<String>> parentId = new InheritableThreadLocal<Maybe<String>>();

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

			Maybe<String> parent = stepStack.get().peek();
			if (parent == null) {
				parent = parentId.get();
			}

			StartTestItemRQ startStepRequest = StepRequestUtils.buildStartStepRequest(signature, step, joinPoint);

			Launch launch = launchMap.get().get(currentLaunchId.get());
			Maybe<String> stepMaybe = launch.startTestItem(parent, startStepRequest);
			stepStack.get().push(stepMaybe);
		}

	}

	@AfterReturning(value = "anyMethod() && withStepAnnotation(step)", argNames = "step")
	public void finishNestedStep(Step step) {
		if (!step.isIgnored()) {
			Maybe<String> stepId = stepStack.get().poll();
			if (stepId == null) {
				return;
			}
			FinishTestItemRQ finishStepRequest = StepRequestUtils.buildFinishStepRequest(Statuses.PASSED, Calendar.getInstance().getTime());
			launchMap.get().get(currentLaunchId.get()).finishTestItem(stepId, finishStepRequest);
		}
	}

	@AfterThrowing(value = "anyMethod() && withStepAnnotation(step)", throwing = "throwable", argNames = "step,throwable")
	public void failedNestedStep(Step step, final Throwable throwable) {

		if (!step.isIgnored()) {

			Maybe<String> stepId = stepStack.get().poll();
			if (stepId == null) {
				return;
			}

			ReportPortal.emitLog(new Function<String, SaveLogRQ>() {
				@Override
				public SaveLogRQ apply(String itemId) {
					SaveLogRQ rq = new SaveLogRQ();
					rq.setItemId(itemId);
					rq.setLevel("ERROR");
					rq.setLogTime(Calendar.getInstance().getTime());
					if (throwable != null) {
						rq.setMessage(getStackTraceAsString(throwable));
					} else {
						rq.setMessage("Test has failed without exception");
					}
					rq.setLogTime(Calendar.getInstance().getTime());

					return rq;
				}
			});

			FinishTestItemRQ finishStepRequest = StepRequestUtils.buildFinishStepRequest(Statuses.FAILED, Calendar.getInstance().getTime());

			while (stepId != null) {
				launchMap.get().get(currentLaunchId.get()).finishTestItem(stepId, finishStepRequest);
				stepId = stepStack.get().poll();
			}

			FinishTestItemRQ finishParentRequest = buildFinishParentRequest(Statuses.FAILED, Calendar.getInstance().getTime());
			launchMap.get().get(currentLaunchId.get()).finishTestItem(parentId.get(), finishParentRequest);
		}

	}

	private FinishTestItemRQ buildFinishParentRequest(String status, Date endTime) {
		FinishTestItemRQ rq = new FinishTestItemRQ();
		rq.setEndTime(endTime);
		rq.setStatus(status);
		// Allows indicate that SKIPPED is not to investigate items for WS
		if (Statuses.SKIPPED.equals(status) && !fromNullable(launchMap.get().get(currentLaunchId.get()).getParameters().getSkippedAnIssue())
				.or(false)) {
			Issue issue = new Issue();
			issue.setIssueType(NOT_ISSUE);
			rq.setIssue(issue);
		}
		return rq;
	}

	public static void setCurrentLaunchId(String id) {
		currentLaunchId.set(id);
	}

	public static void addLaunch(String key, Launch launch) {
		launchMap.get().put(key, launch);
		currentLaunchId.set(key);
	}

	public static void setParentId(Maybe<String> parent) {
		parentId.set(parent);
	}
}
