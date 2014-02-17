/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.component;

import org.jboss.as.naming.ManagedReference;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.invocation.Interceptors;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * An {@link InterceptorFactory} responsible for creating {@link Interceptor} instance corresponding to a <code>container-interceptor</code>
 * applicable for an EJB
 *
 * @author Jaikiran Pai
 */
final class ContainerInterceptorMethodInterceptorFactory implements InterceptorFactory {
    private final ManagedReference interceptorInstanceRef;
    private final Method method;

    /**
     * @param interceptorInstanceRef The managed reference to the container-interceptor instance
     * @param method                 The method for which the interceptor has to be created
     */
    ContainerInterceptorMethodInterceptorFactory(final ManagedReference interceptorInstanceRef, final Method method) {
        this.interceptorInstanceRef = interceptorInstanceRef;
        this.method = method;
    }

    /**
     * Creates and returns a {@link Interceptor} which invokes the underlying container-interceptor during a method invocation
     *
     * @param context The interceptor factory context
     * @return
     */
    public Interceptor create(final InterceptorFactoryContext context) {
        return new ContainerInterceptorMethodInterceptor(this.interceptorInstanceRef, method);
    }


    /**
     * {@link Interceptor} responsible for invoking the underlying container-interceptor in its {@link #processInvocation(org.jboss.invocation.InterceptorContext)}
     * method
     */
    private static final class ContainerInterceptorMethodInterceptor implements Interceptor {

        private final ManagedReference interceptorInstanceRef;
        private final Method method;

        /**
         * @param interceptorInstanceRef The managed reference to the container-interceptor instance
         * @param method                 The method on which the interceptor applies
         */
        ContainerInterceptorMethodInterceptor(final ManagedReference interceptorInstanceRef, final Method method) {
            this.method = method;
            this.interceptorInstanceRef = interceptorInstanceRef;
        }

        /**
         * {@inheritDoc}
         */
        public Object processInvocation(final InterceptorContext context) throws Exception {
            // get the container-interceptor instance
            final Object interceptorInstance = interceptorInstanceRef.getInstance();
            try {
                final Method method = this.method;
                return method.invoke(interceptorInstance, context.getInvocationContext());
            } catch (IllegalAccessException e) {
                final IllegalAccessError n = new IllegalAccessError(e.getMessage());
                n.setStackTrace(e.getStackTrace());
                throw n;
            } catch (InvocationTargetException e) {
                throw Interceptors.rethrow(e.getCause());
            }
        }
    }
}
