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
public @interface Attributes {

	String[] component() default {};

	String[] e2e() default {};

	String[] persona() default {};

	String[] product() default {};

	String[] vertical() default {};

	Attribute[] attributes() default {};

	MultiKeyAttribute[] multiKeyAttributes() default {};

	MultiValueAttribute[] multiValueAttributes() default {};
}
