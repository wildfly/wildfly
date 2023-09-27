/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.serverside.remote;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;
import org.jboss.logging.Logger;

/**
 * A simple interceptor that adds a prefix to the result based on the result data type.
 */
public class LoggingInterceptor {
    private static final Logger log = Logger.getLogger(LoggingInterceptor.class);
    static final String PREFIX = "Intercepted";

    @AroundInvoke
    public Object logBeanAccess(final InvocationContext invocationContext) throws Exception {
        log.trace("Intercepted");
        Object result = invocationContext.proceed();
        return result instanceof String ? PREFIX + result : result;
    }
}
