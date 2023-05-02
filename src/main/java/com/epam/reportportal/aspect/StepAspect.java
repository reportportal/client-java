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
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;

import static java.util.Optional.ofNullable;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
@Aspect
public class StepAspect {
	private static final ThreadLocal<Boolean> FAILED_STEP_STATUS = ThreadLocal.withInitial(() -> false);

	@Pointcut("@annotation(step)")
	public void withStepAnnotation(Step step) {

	}

	@Pointcut("execution(* *.*(..))")
	public void anyMethod() {

	}

	public static void failFinishNestedStepStatus() {
		FAILED_STEP_STATUS.set(true);
	}

	@Before(value = "anyMethod() && withStepAnnotation(step)", argNames = "joinPoint,step")
	public void startNestedStep(JoinPoint joinPoint, Step step) {
		if (step.isIgnored()) {
			return;
		}
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		StartTestItemRQ startStepRequest = StepRequestUtils.buildStartStepRequest(signature, step, joinPoint);
		ofNullable(Launch.currentLaunch()).ifPresent(l -> l.getStepReporter().startNestedStep(startStepRequest));
	}

	@AfterReturning(value = "anyMethod() && withStepAnnotation(step)", argNames = "step")
	public void finishNestedStep(Step step) {
		if (step.isIgnored()) {
			return;
		}
		ofNullable(Launch.currentLaunch()).ifPresent(l -> l.getStepReporter().finishNestedStep(
				FAILED_STEP_STATUS.get() ? ItemStatus.FAILED : ItemStatus.PASSED
		));
		FAILED_STEP_STATUS.set(false);
	}

	@AfterThrowing(value = "anyMethod() && withStepAnnotation(step)", throwing = "throwable", argNames = "step,throwable")
	public void failedNestedStep(Step step, final Throwable throwable) {
		if (step.isIgnored()) {
			return;
		}
		ofNullable(Launch.currentLaunch()).ifPresent(l -> l.getStepReporter().finishNestedStep(throwable));
	}
}
