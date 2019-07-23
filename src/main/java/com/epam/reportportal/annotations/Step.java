package com.epam.reportportal.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation-marker for methods that are invoked during the test execution. Methods that are marked by this annotation
 * are represented in Report Portal as 'Nested Steps' with {@link com.epam.ta.reportportal.ws.model.StartTestItemRQ#hasStats} equal to 'false'
 * Methods marked with this annotation can be nested in other methods and will be attached (reported as a child)
 * to the 'closest' wrapper (either test method or another method marked with this annotation)
 *
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Step {

	String value() default "";

	String description() default "";

	boolean isIgnored() default false;

	StepTemplateConfig templateConfig() default @StepTemplateConfig;
}
