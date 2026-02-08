package com.ersted.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Counted {
    String value() default "";
    String[] tags() default {};
    String description() default "";
    boolean recordErrors() default true;
}
