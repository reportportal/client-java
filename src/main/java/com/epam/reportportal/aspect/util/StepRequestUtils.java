package com.epam.reportportal.aspect.util;

import com.epam.reportportal.annotations.Step;
import com.epam.reportportal.annotations.UniqueID;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.Calendar;
import java.util.Date;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class StepRequestUtils {

	private StepRequestUtils() {
		//static only
	}

	public static StartTestItemRQ buildStartStepRequest(MethodSignature signature, Step step, JoinPoint joinPoint) {
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

	public static FinishTestItemRQ buildFinishStepRequest(String status, Date endTime) {
		FinishTestItemRQ rq = new FinishTestItemRQ();
		rq.setEndTime(endTime);
		rq.setStatus(status);
		return rq;
	}
}
