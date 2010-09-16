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

package org.jboss.as.deployment.managedbean.container;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;

import javax.interceptor.ExcludeClassInterceptors;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Method handler used to proxy managed bean method invocations.  For each method called it will check to see if the method
 * supports interception and will execute a new {@link InvocationContext}.  If the method does not support interceptors,
 * it will run the method directly on the managed bean instance.
 *
 * @author John E. Bailey
 */
public class ManagedBeanProxyHandler<T> /* extends ProxyHandler<T> */ implements MethodHandler {
    private final List<ManagedBeanInterceptor.AroundInvokeInterceptor<?>> interceptors;
    private final T instance;

    public static <T> T createProxy(final Class<T> managedBeanClass, final T managedBean, final List<ManagedBeanInterceptor.AroundInvokeInterceptor<?>> interceptors) throws IllegalAccessException, InstantiationException {
        final ManagedBeanProxyHandler<T> handler = new ManagedBeanProxyHandler<T>(managedBean, interceptors);
        //return ProxyFactory.createProxy(managedBeanClass, );
        final ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setSuperclass(managedBeanClass);

        final Class<? extends T> type = castClass(proxyFactory.createClass(), managedBeanClass);
        T proxy = type.newInstance();
        ((ProxyObject)proxy).setHandler(handler);
        return proxy;
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<? extends T> castClass(final Class<?> rawClass, final Class<T> expectedParent) {
        assert expectedParent.isAssignableFrom(rawClass);
        return (Class<? extends T>) rawClass;
    }

    /**
     * Create an instance.
     *
     * @param managedBeanInstance The managed bean instance
     * @param interceptors The interceptor chain
     */
    private ManagedBeanProxyHandler(T managedBeanInstance, List<ManagedBeanInterceptor.AroundInvokeInterceptor<?>> interceptors) {
        //super(managedBeanInstance);
        this.instance = managedBeanInstance;
        this.interceptors = interceptors;
    }

    /**
     * Invoke a method on a managed bean instance method.
     *
     * @param instance The the managed bean instance
     * @param method The method invoked
     * @param arguments The arguments to the method invocation
     * @return The value of the invocation context execution
     */
    protected Object invokeMethod(T instance, Method method, Object[] arguments) {
        try {
            if(!method.isAnnotationPresent(ExcludeClassInterceptors.class)) {
                return new InvocationContext<T>(instance, method, arguments, interceptors).proceed();
            }
            return method.invoke(instance, arguments);
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public Object invoke(Object o, Method method, Method proceed, Object[] arguments) throws Throwable {
        return invokeMethod(instance, method, arguments);
    }
}
