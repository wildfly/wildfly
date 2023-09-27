/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.container.interceptor;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

import org.jboss.logging.Logger;

/**
 * Simple interceptor, which throws an {@link IllegalArgumentException}.
 *
 * @author Josef Cacek
 */
public class FailingContainerInterceptor {

    private static Logger LOGGER = Logger.getLogger(FailingContainerInterceptor.class);

    // Private methods -------------------------------------------------------

    @AroundInvoke
    Object throwException(final InvocationContext invocationContext) throws Exception {
        LOGGER.trace("Throwing exception");
        throw new IllegalArgumentException("Blocking access to the bean.");
    }
}
