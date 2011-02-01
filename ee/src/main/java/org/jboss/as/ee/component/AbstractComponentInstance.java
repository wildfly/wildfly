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

import java.util.List;
import org.jboss.as.ee.component.interceptor.ComponentInstanceInterceptor;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorInvocationHandler;
import org.jboss.invocation.Interceptors;
import org.jboss.invocation.proxy.ProxyFactory;

/**
 * An abstract base component instance.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractComponentInstance implements ComponentInstance {

    private static final long serialVersionUID = -8099216228976950066L;

    private final AbstractComponent component;
    private final Interceptor interceptor;
    private final Object instance;

    /**
     * Construct a new instance.
     *
     * @param component the component
     * @param interceptor the interceptor for all invocations
     * @param instance the object instance
     */
    protected AbstractComponentInstance(final AbstractComponent component, final Interceptor interceptor, final Object instance) {
        this.component = component;
        this.interceptor = interceptor;
        this.instance = instance;
    }

    public Component getComponent() {
        return component;
    }

    public Interceptor getInterceptor() {
        return interceptor;
    }

    public Object getInstance() {
        return instance;
    }

    public Object createLocalClientProxy() {
        ProxyFactory<?> proxyFactory = component.getProxyFactory();
        List<Interceptor> list = component.getComponentLevelInterceptors();

        // Add the instance interceptor last
        list.add(new ComponentInstanceInterceptor(this, component.getMethodInterceptorFactories()));
        final Interceptor defaultChain = Interceptors.getChainedInterceptor(list);
        try {
            return proxyFactory.newInstance(new InterceptorInvocationHandler(defaultChain));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create proxy instance for bean component: " + component.getBeanClass(), e);
        }
    }
}
