/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.ee.component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;
import org.jboss.as.naming.ManagedReference;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.Interceptors;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ManagedReferenceLifecycleMethodInterceptor implements Interceptor {

    private final AtomicReference<ManagedReference> instanceRef;
    private final Method method;
    private final boolean withContext;
    private final boolean changeMethod;

    ManagedReferenceLifecycleMethodInterceptor(final AtomicReference<ManagedReference> instanceRef, final Method method, final boolean changeMethod) {
        this.changeMethod = changeMethod;
        this.method = method;
        this.instanceRef = instanceRef;
        withContext = method.getParameterTypes().length == 1;
    }

    /** {@inheritDoc} */
    public Object processInvocation(final InterceptorContext context) throws Exception {
        final ManagedReference reference = instanceRef.get();
        final Object instance = reference.getInstance();
        try {
            Method method = this.method;
            if (withContext) {
                if (changeMethod) {
                    final Method oldMethod = context.getMethod();
                    context.setMethod(method);
                    try {
                        return method.invoke(instance, context.getInvocationContext());
                    } finally {
                        context.setMethod(oldMethod);
                    }
                } else {
                    return method.invoke(instance, context.getInvocationContext());
                }
            } else {
                method.invoke(instance, null);
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
