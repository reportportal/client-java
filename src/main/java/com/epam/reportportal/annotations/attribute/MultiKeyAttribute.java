package com.epam.reportportal.annotations.attribute;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MultiKeyAttribute {

	String[] keys() default {};

	String value() default "";

	boolean isSystem() default false;

	boolean isNullKey() default false;
}
