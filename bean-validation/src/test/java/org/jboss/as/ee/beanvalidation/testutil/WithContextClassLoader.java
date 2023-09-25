/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.beanvalidation.testutil;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the context class loader to use for a given test.
 *
 * @author Gunnar Morling
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface WithContextClassLoader {

    /**
     * The context class loader to use for a given test.
     *
     * @return the context class loader to use for a given test
     */
    Class<? extends ClassLoader> value();

    /**
     * A marker type for setting the context class loader to {@code null}.
     *
     * @author Gunnar Morling
     */
    public static class NullClassLoader extends ClassLoader {
    }
}
