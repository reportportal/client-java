package com.epam.reportportal.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
@Aspect
public class StepAspect {

	@Pointcut("@annotation(com.epam.reportportal.annotations.Step)")
	public void withNestedStepAnnotation() {

	}

	@Pointcut("execution(* *(..))")
	public void anyMethod() {

	}

	@Before("anyMethod() && withNestedStepAnnotation()")
	public void startNestedStep(final JoinPoint joinPoint) {

		System.err.println("AOP TEST");

//		MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
//		Step step = methodSignature.getMethod().getAnnotation(Step.class);
//
//		String value = step.value();
//		String name = "qwe";
//
//		Object[] args = joinPoint.getArgs();
//		String[] parameterNames = methodSignature.getParameterNames();
//
//		for (int i = 0; i < args.length; i++) {
//			ParameterResource parameterResource = new ParameterResource();
//			parameterResource.setKey(parameterNames[i]);
//			parameterResource.setValue(String.valueOf(args[i]));
//
//		}

	}
}
