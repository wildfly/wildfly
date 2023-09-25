/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.component.session;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;

import java.lang.reflect.Method;

/**
 * @author Stuart Douglas
 */
public class NotBusinessMethodInterceptor implements Interceptor {

    private final Method method;

    public NotBusinessMethodInterceptor(final Method method) {
        this.method = method;
    }

    @Override
    public Object processInvocation(final InterceptorContext context) throws Exception {
        throw EjbLogger.ROOT_LOGGER.failToCallBusinessOnNonePublicMethod(method);
    }
}
