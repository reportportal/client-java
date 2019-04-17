package com.epam.reportportal.aspect;

import com.epam.reportportal.annotations.Step;
import com.epam.reportportal.aspect.util.StepRequestUtils;
import com.epam.reportportal.listeners.Statuses;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import com.google.common.base.Function;
import com.google.common.collect.Queues;
import io.reactivex.Maybe;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Deque;
import java.util.Map;

import static com.google.common.base.Throwables.getStackTraceAsString;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
@Aspect
public class StepAspect {

	public static final Logger LOGGER = LoggerFactory.getLogger(StepAspect.class);

	private static InheritableThreadLocal<String> currentLaunchId = new InheritableThreadLocal<String>();

	private static InheritableThreadLocal<Map<String, Launch>> launchMap = new InheritableThreadLocal<Map<String, Launch>>();

	private static InheritableThreadLocal<Deque<Maybe<Long>>> stepStack = new InheritableThreadLocal<Deque<Maybe<Long>>>() {
		@Override
		protected Deque<Maybe<Long>> initialValue() {
			return Queues.newArrayDeque();
		}
	};

	private static InheritableThreadLocal<Maybe<Long>> parentId = new InheritableThreadLocal<Maybe<Long>>();

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

			Maybe<Long> parent = stepStack.get().peek();
			if (parent == null) {
				parent = parentId.get();
			}

			StartTestItemRQ startStepRequest = StepRequestUtils.buildStartStepRequest(signature, step, joinPoint);

			Launch launch = launchMap.get().get(currentLaunchId.get());
			Maybe<Long> stepMaybe = launch.startTestItem(parent, startStepRequest);

			stepStack.get().push(stepMaybe);

		}

	}

	@AfterReturning(value = "anyMethod() && withStepAnnotation(step)", argNames = "step")
	public void finishNestedStep(Step step) {
		if (!step.isIgnored()) {
			Maybe<Long> stepId = stepStack.get().poll();
			if (stepId == null) {
				LOGGER.error("Id of the 'STEP' to finish retrieved from step stack is NULL");
				return;
			}
			FinishTestItemRQ finishStepRequest = StepRequestUtils.buildFinishStepRequest(Statuses.PASSED, Calendar.getInstance().getTime());
			launchMap.get().get(currentLaunchId.get()).finishTestItem(stepId, finishStepRequest);
		}
	}

	@AfterThrowing(value = "anyMethod() && withStepAnnotation(step)", throwing = "throwable", argNames = "step,throwable")
	public void failedNestedStep(Step step, final Throwable throwable) {

		if (!step.isIgnored()) {

			Maybe<Long> stepId = stepStack.get().poll();
			if (stepId == null) {
				LOGGER.error("Id of the 'STEP' to finish retrieved from step stack is NULL");
				return;
			}

			ReportPortal.emitLog(new Function<Long, SaveLogRQ>() {
				@Override
				public SaveLogRQ apply(Long itemId) {
					SaveLogRQ rq = new SaveLogRQ();
					rq.setTestItemId(itemId);
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
			launchMap.get().get(currentLaunchId.get()).finishTestItem(stepId, finishStepRequest);
		}

	}

	public static void setCurrentLaunchId(String id) {
		currentLaunchId.set(id);
	}

	public static void addLaunch(String key, Launch launch) {
		launchMap.get().put(key, launch);
		currentLaunchId.set(key);
	}

	public static void setParentId(Maybe<Long> parent) {
		parentId.set(parent);
	}
}
