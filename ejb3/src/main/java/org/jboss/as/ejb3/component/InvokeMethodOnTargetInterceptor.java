/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.Interceptors;

/**
 *
 * Interceptor that directly invokes the target from the interceptor context and returns the result
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class InvokeMethodOnTargetInterceptor implements Interceptor {
    public static final Class<Object[]> PARAMETERS_KEY = Object[].class;

    private final Method method;

    InvokeMethodOnTargetInterceptor(Method method) {
        this.method = method;
    }

    public static InterceptorFactory factory(final Method method) {
        return new ImmediateInterceptorFactory(new InvokeMethodOnTargetInterceptor(method));
    }

    @Override
    public Object processInvocation(final InterceptorContext context) throws Exception {
        final Object instance = context.getPrivateData(ComponentInstance.class).getInstance();
        try {
            return method.invoke(instance, context.getPrivateData(PARAMETERS_KEY));
        } catch (InvocationTargetException e) {
            throw Interceptors.rethrow(e.getCause());
        }
    }
}
