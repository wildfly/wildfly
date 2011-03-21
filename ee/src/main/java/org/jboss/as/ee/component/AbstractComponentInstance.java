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

import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactoryContext;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * An abstract base component instance.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractComponentInstance implements ComponentInstance {

    private static final long serialVersionUID = -8099216228976950066L;

    private final AbstractComponent component;
    private final Object instance;
    private final List<Interceptor> preDestroyInterceptors;
    private final InterceptorFactoryContext factoryContext;

    /**
     * This is an identity map.  This means that only <b>certain</b> {@code Method} objects will
     * match - specifically, they must equal the objects provided to the proxy.
     */
    private Map<Method, Interceptor> methodMap;

    /**
     * Construct a new instance.
     *
     * @param component the component
     * @param instance the object instance
     */
    protected AbstractComponentInstance(final AbstractComponent component, final Object instance, final List<Interceptor> preDestroyInterceptors,final InterceptorFactoryContext factoryContext) {
        this.component = component;
        this.instance = instance;
        this.preDestroyInterceptors = preDestroyInterceptors;
        this.factoryContext = factoryContext;
    }

    /** {@inheritDoc} */
    public Component getComponent() {
        return component;
    }

    /** {@inheritDoc} */
    public Object getInstance() {
        return instance;
    }

    @Override
    public Iterable<Interceptor> getPreDestroyInterceptors() {
        return preDestroyInterceptors;
    }

    /** {@inheritDoc} */
    public Interceptor getInterceptor(final Method method) throws IllegalStateException {
        Interceptor interceptor = methodMap.get(method);
        if (interceptor == null) {
            throw new IllegalStateException("Method does not exist");
        }
        return interceptor;
    }

    public InterceptorFactoryContext getInterceptorFactoryContext() {
        return factoryContext;
    }

    void setMethodMap(Map<Method, Interceptor> methodMap) {
        this.methodMap = methodMap;
    }
}
