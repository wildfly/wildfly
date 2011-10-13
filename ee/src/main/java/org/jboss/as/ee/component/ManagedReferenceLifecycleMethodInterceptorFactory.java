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

import org.jboss.as.naming.ManagedReference;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ManagedReferenceLifecycleMethodInterceptorFactory implements InterceptorFactory {
    private final Object contextKey;
    private final Method method;
    private final boolean changeMethod;
    private final boolean lifecycleMethod;

    /**
     * This is equivalent to calling <code>ManagedReferenceLifecycleMethodInterceptorFactory(Object, java.lang.reflect.Method, boolean, false)</code>
     *
     * @param contextKey
     * @param method       The method for which the interceptor has to be created
     * @param changeMethod True if during the interceptor processing, the {@link org.jboss.invocation.InterceptorContext#getMethod()}
     *                     is expected to return the passed <code>method</code>
     */
    ManagedReferenceLifecycleMethodInterceptorFactory(final Object contextKey, final Method method, final boolean changeMethod) {
        this(contextKey, method, changeMethod, false);
    }

    /**
     * @param contextKey
     * @param method          The method for which the interceptor has to be created
     * @param changeMethod    True if during the interceptor processing, the {@link org.jboss.invocation.InterceptorContext#getMethod()}
     *                        is expected to return the passed <code>method</code>
     * @param lifecycleMethod If the passed <code>method</code> is a lifecycle callback method. False otherwise
     */
    ManagedReferenceLifecycleMethodInterceptorFactory(final Object contextKey, final Method method, final boolean changeMethod, final boolean lifecycleMethod) {
        this.contextKey = contextKey;
        this.method = method;
        this.changeMethod = changeMethod;
        this.lifecycleMethod = lifecycleMethod;
    }

    public Interceptor create(final InterceptorFactoryContext context) {
        @SuppressWarnings("unchecked")
        final AtomicReference<ManagedReference> ref = (AtomicReference<ManagedReference>) context.getContextData().get(contextKey);
        return new ManagedReferenceLifecycleMethodInterceptor(ref, method, changeMethod, this.lifecycleMethod);
    }
}
