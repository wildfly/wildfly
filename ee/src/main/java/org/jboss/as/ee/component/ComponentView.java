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
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.invocation.Interceptors;
import org.jboss.invocation.proxy.ProxyFactory;

import java.io.Serializable;

/**
 * A single view of a component.  This will return a proxy to a single instance of the component.  The proxy will
 * be based on the provided view interface.
 *
 * @author John Bailey
 */
public class ComponentView implements ManagedReferenceFactory {

    private final Class<?> viewClass;
    private final ProxyFactory<?> proxyFactory;
    private final AbstractComponent component;

    /**
     * Construct a new instance.
     *
     * @param component the component for this view
     * @param viewClass the view class
     * @param proxyFactory the proxy factory
     */
    public ComponentView(final AbstractComponent component, final Class<?> viewClass, final ProxyFactory<?> proxyFactory) {
        this.viewClass = viewClass;
        this.proxyFactory = proxyFactory;
        this.component = component;
    }

    @Override
    public ManagedReference getReference() {
        return new ManagedReference() {
            @Override
            public void release() {

            }

            @Override
            public Object getInstance() {
                try {
                    return viewClass.cast(proxyFactory.newInstance(new ProxyInvocationHandler(Interceptors.getChainedInterceptor(component.createClientInterceptor(viewClass), component.getComponentInterceptor()))));
                } catch (InstantiationException e) {
                    throw new InstantiationError(e.getMessage());
                } catch (IllegalAccessException e) {
                    throw new IllegalAccessError(e.getMessage());
                }
            }
        };
    }

    Class<?> getViewClass() {
        return viewClass;
    }

    public Object getViewForInstance(Serializable sessionId) {
        try {
            return viewClass.cast(proxyFactory.newInstance(new ProxyInvocationHandler(Interceptors.getChainedInterceptor(component.createClientInterceptor(viewClass,sessionId), component.getComponentInterceptor()))));
        } catch (InstantiationException e) {
            throw new InstantiationError(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    ProxyFactory<?> getProxyFactory() {
        return proxyFactory;
    }
}
