package com.epam.reportportal.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface StepTemplateConfig {

	String METHOD_NAME_TEMPLATE = "method";
	String ITERABLE_START_PATTERN = "[";
	String ITERABLE_END_PATTERN = "]";
	String ITERABLE_ELEMENT_DELIMITER = ",";
	String ARRAY_START_PATTERN = "{";
	String ARRAY_END_PATTERN = "}";
	String ARRAY_ELEMENT_DELIMITER = ",";

	String methodNameTemplate() default METHOD_NAME_TEMPLATE;

	String iterableStartSymbol() default ITERABLE_START_PATTERN;

	String iterableEndSymbol() default ITERABLE_END_PATTERN;

	String iterableElementDelimiter() default ITERABLE_ELEMENT_DELIMITER;

	String arrayStartSymbol() default ARRAY_START_PATTERN;

	String arrayEndSymbol() default ARRAY_END_PATTERN;

	String arrayElementDelimiter() default ARRAY_ELEMENT_DELIMITER;
}
