/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.serverside.protocol;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

public class ProtocolSampleInterceptor {
    static final String PREFIX = "ProtocolInterceptor:";

    @AroundInvoke
    public Object aroundInvoke(final InvocationContext invocationContext) throws Exception {
        return PREFIX + invocationContext.proceed();
    }
}
