package com.piaoniu.permission.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author code4crafter@gmail.com
 *         Date: 16/8/16
 *         Time: 上午10:59
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface NeedPermission {

	String value();
}
