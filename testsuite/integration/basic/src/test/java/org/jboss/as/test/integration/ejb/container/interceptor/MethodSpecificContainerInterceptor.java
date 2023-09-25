/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.container.interceptor;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

/**
 * Simple interceptor, which adds its classname in front of the result of {@link InvocationContext#proceed()}.
 *
 * @author Jaikiran Pai
 */
public class MethodSpecificContainerInterceptor {

    @SuppressWarnings("unused")
    @AroundInvoke
    private Object iAmAroundInvoke(final InvocationContext invocationContext) throws Exception {
        return this.getClass().getName() + " " + invocationContext.proceed();
    }
}
