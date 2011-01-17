/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.container.interceptor;

import java.lang.reflect.Method;
import java.util.List;
import org.jboss.as.ee.container.injection.ResourceInjection;

/**
 * {@link MethodInterceptor} implementation used to support method interceptors annotated with @AroundInvoke.
 *
 * @author John Bailey
 */
public class AroundInvokeInterceptor<T> implements MethodInterceptor {
    private final Class<T> targetClass;
    private final Method aroundInvokeMethod;
    private final List<ResourceInjection> injections;
    private final MethodInterceptorFilter methodFilter;
    private final boolean acceptsInvocationContext;

    public static <T> AroundInvokeInterceptor<T> create(final Class<T> targetClass, final Method aroundInvokeMethod, final MethodInterceptorFilter methodFilter, final List<ResourceInjection> injections) {
        return new AroundInvokeInterceptor<T>(targetClass, aroundInvokeMethod, methodFilter, injections);
    }

    private AroundInvokeInterceptor(final Class<T> targetClass, final Method aroundInvokeMethod, final MethodInterceptorFilter methodFilter, final List<ResourceInjection> injections) {
        this.targetClass = targetClass;
        this.aroundInvokeMethod = aroundInvokeMethod;
        this.methodFilter = methodFilter;
        this.injections = injections;
        this.acceptsInvocationContext = aroundInvokeMethod.getParameterTypes().length == 1 && aroundInvokeMethod.getParameterTypes()[0].equals(javax.interceptor.InvocationContext.class);
    }

    public Object intercept(final InvocationContext<?> invocationContext) throws Exception {
        final T interceptor = setupInterceptor(targetClass, injections);
        return aroundInvokeMethod.invoke(interceptor, invocationContext);
    }

    protected T setupInterceptor(final Class<T> targetClass, final List<ResourceInjection> injections) {
        final T interceptor;
        try {
            interceptor = targetClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create interceptor instance for class: " + targetClass, e);
        }
        for(ResourceInjection injection : injections) {
            injection.inject(interceptor);
        }
        return interceptor;
    }

    public MethodInterceptorFilter getMethodFilter() {
        return methodFilter;
    }

    public boolean acceptsInvocationContext() {
        return acceptsInvocationContext;
    }
}
