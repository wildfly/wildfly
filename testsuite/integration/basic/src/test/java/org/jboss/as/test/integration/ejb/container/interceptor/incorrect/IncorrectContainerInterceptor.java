/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.container.interceptor.incorrect;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

import org.jboss.logging.Logger;

/**
 * Incorrect interceptor - contains 2 methods annotated with the {@link AroundInvoke}.
 *
 * @author Josef Cacek
 */
public class IncorrectContainerInterceptor {

    private static Logger LOGGER = Logger.getLogger(IncorrectContainerInterceptor.class);

    // Private methods -------------------------------------------------------

    @AroundInvoke
    Object method1(final InvocationContext invocationContext) throws Exception {
        LOGGER.trace("method1 invoked");
        return invocationContext.proceed();
    }

    @AroundInvoke
    Object method2(final InvocationContext invocationContext) throws Exception {
        LOGGER.trace("method2 invoked");
        return invocationContext.proceed();
    }
}
