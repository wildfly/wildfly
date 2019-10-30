package org.jboss.as.test.integration.weld.interceptor.bridgemethods;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.interceptor.InterceptorBinding;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 *
 */
@InterceptorBinding
@Inherited
@Target({ TYPE, METHOD })
@Retention(RUNTIME)
public @interface SomeInterceptorBinding {
}
