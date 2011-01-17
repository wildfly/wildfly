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
import java.util.ArrayList;
import java.util.List;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import javax.interceptor.ExcludeClassInterceptors;

/**
 * Proxy method handler implementation used to apply {@link MethodInterceptor} instances to method calls.
 *
 * @author John Bailey
 */
public class InterceptingProxyHandler<T> implements MethodHandler {
    private final T instance;
    private final List<MethodInterceptor> interceptors;

    /**
     * Create a proxy instance for give bean instance.
     *
     * @param beanClass The bean class to proxy
     * @param instance The bean instance to proxy
     * @param interceptors The interceptors to apply
     * @param <T> The bean type
     * @return A proxy instance
     */
    public static <T> T createProxy(final Class<T> beanClass, final T instance, final List<MethodInterceptor> interceptors) {
        final InterceptingProxyHandler<T> proxyHandler = new InterceptingProxyHandler<T>(instance, interceptors);

        final ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setSuperclass(beanClass);

        final Class<? extends T> type = castClass(proxyFactory.createClass(), beanClass);
        T proxy = null;
        try {
            proxy = type.newInstance();
        } catch (Throwable t) {
            throw new RuntimeException("Failed to create proxy instance", t);
        }
        ProxyObject.class.cast(proxy).setHandler(proxyHandler);
        return proxy;
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<? extends T> castClass(final Class<?> rawClass, final Class<T> expectedParent) {
        assert expectedParent.isAssignableFrom(rawClass);
        return (Class<? extends T>) rawClass;
    }

    public InterceptingProxyHandler(final T instance, final List<MethodInterceptor> interceptors) {
        this.instance = instance;
        this.interceptors = interceptors;
    }

    /**
     * Invoke a method on a bean instance method.
     *
     * @param proxy     The the proxy instance
     * @param method    The method invoked
     * @param arguments The arguments to the method invocation
     * @return The value of the invocation context execution
     */
    public Object invoke(final Object proxy, final Method method, final Method proceed, final Object[] arguments) {
        try {
            if (!method.isAnnotationPresent(ExcludeClassInterceptors.class)) {
                final List<MethodInterceptor> interceptorsToApply = new ArrayList<MethodInterceptor>();
                for(MethodInterceptor interceptor : interceptors) {
                    if(interceptor.getMethodFilter().intercepts(method)) {
                        interceptorsToApply.add(interceptor);
                    }
                }
                return new InvocationContext<T>(instance, method, arguments, interceptorsToApply).proceed();
            }
            return method.invoke(instance, arguments);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
