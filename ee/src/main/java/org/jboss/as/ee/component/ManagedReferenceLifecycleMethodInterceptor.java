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
final class ManagedReferenceLifecycleMethodInterceptor implements Interceptor {
    private final Object contextKey;
    private final Method method;
    private final boolean changeMethod;
    private final boolean lifecycleMethod;
    private final boolean withContext;

    /**
     * This is equivalent to calling <code>ManagedReferenceLifecycleMethodInterceptorFactory(Object, java.lang.reflect.Method, boolean, false)</code>
     *
     * @param contextKey
     * @param method       The method for which the interceptor has to be created
     * @param changeMethod True if during the interceptor processing, the {@link org.jboss.invocation.InterceptorContext#getMethod()}
     *                     is expected to return the passed <code>method</code>
     */
    ManagedReferenceLifecycleMethodInterceptor(final Object contextKey, final Method method, final boolean changeMethod) {
        this(contextKey, method, changeMethod, false);
    }

    /**
     * @param contextKey
     * @param method          The method for which the interceptor has to be created
     * @param changeMethod    True if during the interceptor processing, the {@link org.jboss.invocation.InterceptorContext#getMethod()}
     *                        is expected to return the passed <code>method</code>
     * @param lifecycleMethod If the passed <code>method</code> is a lifecycle callback method. False otherwise
     */
    ManagedReferenceLifecycleMethodInterceptor(final Object contextKey, final Method method, final boolean changeMethod, final boolean lifecycleMethod) {
        this.contextKey = contextKey;
        this.method = method;
        this.changeMethod = changeMethod;
        this.lifecycleMethod = lifecycleMethod;
        withContext = method.getParameterCount() == 1;
    }

    /**
     * {@inheritDoc}
     */
    public Object processInvocation(final InterceptorContext context) throws Exception {
        final ManagedReference reference = (ManagedReference) context.getPrivateData(ComponentInstance.class).getInstanceData(contextKey);
        final Object instance = reference.getInstance();
        try {
            final Method method = this.method;
            if (withContext) {
                final Method oldMethod = context.getMethod();
                try {
                    if (this.lifecycleMethod) {
                        // because InvocationContext#getMethod() is expected to return null for lifecycle methods
                        context.setMethod(null);
                        return method.invoke(instance, context.getInvocationContext());
                    } else if (this.changeMethod) {
                        context.setMethod(method);
                        return method.invoke(instance, context.getInvocationContext());
                    } else {
                        return method.invoke(instance, context.getInvocationContext());
                    }
                } finally {
                    // reset any changed method on the interceptor context
                    context.setMethod(oldMethod);
                }
            } else {
                method.invoke(instance);
                return context.proceed();
            }
        } catch (IllegalAccessException e) {
            final IllegalAccessError n = new IllegalAccessError(e.getMessage());
            n.setStackTrace(e.getStackTrace());
            throw n;
        } catch (InvocationTargetException e) {
            throw Interceptors.rethrow(e.getCause());
        }
    }

}
