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

package org.jboss.as.managedbean.container;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Container to store an interceptor and its resource injections.
 *
 * @param <T> The managed bean interceptor type
 *
 * @author John E. Bailey
 */
public class ManagedBeanInterceptor<T> {
    private final Class<T> interceptorType;
    private final Method aroundInvokeMethod;
    private final List<ResourceInjection<?>> resourceInjections;

    /**
     * Create an instance.
     *
     * @param interceptorType The interceptor type
     * @param aroundInvokeMethod The @AroundInvoke method
     * @param resourceInjections The resouce injections
     */
    public ManagedBeanInterceptor(final Class<T> interceptorType, final Method aroundInvokeMethod, final List<ResourceInjection<?>> resourceInjections) {
        this.interceptorType = interceptorType;
        this.aroundInvokeMethod = aroundInvokeMethod;
        this.resourceInjections = resourceInjections;
    }

    AroundInvokeInterceptor<T> createInstance() throws IllegalAccessException, InstantiationException {
        final T interceptor = interceptorType.newInstance();
        for(ResourceInjection<?> resourceInjection : resourceInjections) {
            resourceInjection.inject(interceptor);
        }
        return new AroundInvokeInterceptor<T>(interceptor, aroundInvokeMethod);
    }

    static class AroundInvokeInterceptor<T> {
        private T target;
        private Method aroundInvokeMethod;

        AroundInvokeInterceptor(T target, Method aroundInvokeMethod) {
            this.target = target;
            this.aroundInvokeMethod = aroundInvokeMethod;
        }

        Object intercept(final InvocationContext<?> invocationContext) throws Exception {
            return aroundInvokeMethod.invoke(target, invocationContext);
        }
    }

    @Override
    public String toString() {
        return "ManagedBeanInterceptor{" +
                "interceptorType=" + interceptorType +
                '}';
    }
}
