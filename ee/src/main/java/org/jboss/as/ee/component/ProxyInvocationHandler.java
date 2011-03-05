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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.SimpleInterceptorFactoryContext;

/**
 * An invocation handler for a component proxy.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ProxyInvocationHandler implements InvocationHandler {

    private final Interceptor interceptor;
    private volatile Map<Class<?>, Object> initialPrivateData = Collections.emptyMap();

    /**
     * Construct a new instance.
     *
     * @param interceptor the initial interceptor
     */
    public ProxyInvocationHandler(final Interceptor interceptor) {
        this.interceptor = interceptor;
    }

    /**
     * Construct a new instance.
     *
     * @param interceptorFactory the factory from which to construct a new interceptor instance
     */
    public ProxyInvocationHandler(final InterceptorFactory interceptorFactory) {
        interceptor = interceptorFactory.create(new SimpleInterceptorFactoryContext());
    }

    /** {@inheritDoc} */

    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        final InterceptorContext context = new InterceptorContext();
        // snapshot
        final Map<Class<?>, Object> privateData = initialPrivateData;
        for (Map.Entry<Class<?>, Object> entry : privateData.entrySet()) {
            final Class<?> key = entry.getKey();
            Object value = entry.getValue();
            putPrivate(context, key, value);
        }
        // special location for original proxy
        context.putPrivateData(Object.class, proxy);
        context.setParameters(args);
        context.setMethod(method);
        return interceptor.processInvocation(context);
    }

    /**
     * Set the initial value for the interceptor context private data with the given key.
     *
     * @param key the key
     * @param value the value
     * @param <T> the value type
     * @return the old value, or {@code null} if none
     */
    public <T> T putPrivateData(Class<T> key, T value) {
        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }
        synchronized (this) {
            final Map<Class<?>, Object> newMap = new IdentityHashMap<Class<?>, Object>(initialPrivateData);
            if (value == null) try {
                return key.cast(newMap.remove(key));
            } finally {
                initialPrivateData = newMap;
            } else try {
                return key.cast(newMap.put(key, value));
            } finally {
                initialPrivateData = newMap;
            }
        }
    }

    /**
     * Get the initial value for the interceptor context private data with the given key.
     *
     * @param key the key
     * @param <T> the value type
     * @return the value
     */
    public <T> T getPrivateData(Class<T> key) {
        return key.cast(initialPrivateData.get(key));
    }

    private static <T> void putPrivate(final InterceptorContext context, final Class<T> key, final Object value) {
        context.putPrivateData(key, key.cast(value));
    }
}
