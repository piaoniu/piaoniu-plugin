package com.piaoniu.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface EnumPub {

    String value() default "value";

    String desc() default "desc";
}
