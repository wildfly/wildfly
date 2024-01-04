/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jboss.as.naming.ManagedReference;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.Interceptors;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ManagedReferenceMethodInterceptor implements Interceptor {
    private final Object contextKey;
    private final Method method;

    ManagedReferenceMethodInterceptor(final Object contextKey, final Method method) {
        this.contextKey = contextKey;
        this.method = method;
    }

    /**
     * {@inheritDoc}
     */
    public Object processInvocation(final InterceptorContext context) throws Exception {
        final ManagedReference reference = (ManagedReference) context.getPrivateData(ComponentInstance.class).getInstanceData(contextKey);
        final Object instance = reference.getInstance();
        try {
            return method.invoke(instance, context.getParameters());
        } catch (IllegalAccessException e) {
            final IllegalAccessError n = new IllegalAccessError(e.getMessage());
            n.setStackTrace(e.getStackTrace());
            throw n;
        } catch (InvocationTargetException e) {
            throw Interceptors.rethrow(e.getCause());
        }
    }
}

