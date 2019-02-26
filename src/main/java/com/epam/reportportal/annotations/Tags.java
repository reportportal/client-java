package com.epam.reportportal.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation provides to mark test steps or test context with specific tags.
 *
 * @author Ilya_Koshaleu
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Tags {

    /**
     * Returns an array of tags of specific test context.
     *
     * @return array of test step tags
     */
    String[] value() default {};
}
