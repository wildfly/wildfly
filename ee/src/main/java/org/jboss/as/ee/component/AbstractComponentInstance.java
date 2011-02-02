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

import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Map;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.SimpleInterceptorFactoryContext;

/**
 * An abstract base component instance.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractComponentInstance implements ComponentInstance {

    private static final long serialVersionUID = -8099216228976950066L;

    private final AbstractComponent component;
    private final Object instance;

    /**
     * This is an identity map.  This means that only <b>certain</b> {@code Method} objects will
     * match - specifically, they must equal the objects provided to the proxy.
     */
    private final Map<Method, Interceptor> methodMap;

    /**
     * Construct a new instance.
     *
     * @param component the component
     * @param instance the object instance
     */
    protected AbstractComponentInstance(final AbstractComponent component, final Object instance) {
        this.component = component;
        this.instance = instance;
        final Map<Method, InterceptorFactory> factoryMap = component.getInterceptorFactoryMap();
        final Map<Method, Interceptor> methodMap = new IdentityHashMap<Method, Interceptor>(factoryMap.size());
        final SimpleInterceptorFactoryContext factoryContext = new SimpleInterceptorFactoryContext();
        factoryContext.getContextData().put(AbstractComponent.INSTANCE_KEY, instance);
        for (Map.Entry<Method, InterceptorFactory> entry : factoryMap.entrySet()) {
            methodMap.put(entry.getKey(), entry.getValue().create(factoryContext));
        }
        this.methodMap = methodMap;
    }

    /** {@inheritDoc} */
    public Component getComponent() {
        return component;
    }

    /** {@inheritDoc} */
    public Object getInstance() {
        return instance;
    }

    /** {@inheritDoc} */
    public Interceptor getInterceptor(final Method method) throws IllegalStateException {
        Interceptor interceptor = methodMap.get(method);
        if (interceptor == null) {
            throw new IllegalStateException("Method does not exist");
        }
        return interceptor;
    }
}
