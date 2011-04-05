/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;

import java.lang.reflect.Method;

/**
 * InterceptorFactory used for lifecycle methods.
 *
 * The factory holds extra information
 * about the method that is being called, so there is no need to call
 * {@link InterceptorContext#setMethod(java.lang.reflect.Method)} before invoking the
 * interceptor.
 *
 * @author Stuart Douglas
 */
public class MethodAwareInterceptorFactory implements InterceptorFactory {

    private final Method method;
    private final InterceptorFactory delegate;


    public MethodAwareInterceptorFactory(InterceptorFactory delegate, Method method) {
        this.delegate = delegate;
        this.method = method;
    }

    @Override
    public Interceptor create(InterceptorFactoryContext context) {
        return new MethodAwareInterceptor(method,delegate.create(context));
    }

    /**
     * Interceptor wrapper that automatically sets the correct method on the InterceptorContext
     */
    private static class MethodAwareInterceptor implements  Interceptor {

        private final Method method;
        private final Interceptor delegate;

        public MethodAwareInterceptor(Method method, Interceptor delegate) {
            this.method = method;
            this.delegate = delegate;
        }

        @Override
        public Object processInvocation(InterceptorContext context) throws Exception {
            final Method oldMethod = context.getMethod();
            try {
                context.setMethod(method);
                return delegate.processInvocation(context);
            } finally {
                context.setMethod(oldMethod);
            }
        }
    }
}
