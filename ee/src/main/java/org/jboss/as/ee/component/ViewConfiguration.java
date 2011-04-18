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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.proxy.ProxyFactory;
import org.jboss.msc.service.ServiceName;

/**
 * A configuration of a component view.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class ViewConfiguration {
    private final ComponentConfiguration componentConfiguration;
    private final ServiceName viewServiceName;
    private final Map<Method, Deque<InterceptorFactory>> viewInterceptors = new IdentityHashMap<Method, Deque<InterceptorFactory>>();
    private final Map<Method, Deque<InterceptorFactory>> clientInterceptors = new IdentityHashMap<Method, Deque<InterceptorFactory>>();
    private final Deque<InterceptorFactory> viewPostConstructInterceptors = new ArrayDeque<InterceptorFactory>();
    private final Deque<InterceptorFactory> clientPostConstructInterceptors = new ArrayDeque<InterceptorFactory>();
    private final Deque<InterceptorFactory> viewPreDestroyInterceptors = new ArrayDeque<InterceptorFactory>();
    private final Deque<InterceptorFactory> clientPreDestroyInterceptors = new ArrayDeque<InterceptorFactory>();
    private final ProxyFactory<?> proxyFactory;
    private final List<BindingConfiguration> bindingConfigurations = new ArrayList<BindingConfiguration>();
    private final Class<?> viewClass;

    /**
     * Construct a new instance.
     *
     * @param viewClass the view class
     * @param componentConfiguration the associated component configuration
     * @param viewServiceName the service name of this view
     * @param proxyFactory the proxy factory to use to locally construct client proxy instances
     */
    public ViewConfiguration(final Class<?> viewClass, final ComponentConfiguration componentConfiguration, final ServiceName viewServiceName, final ProxyFactory<?> proxyFactory) {
        this.componentConfiguration = componentConfiguration;
        this.viewServiceName = viewServiceName;
        this.proxyFactory = proxyFactory;
        this.viewClass = viewClass;
    }

    /**
     * Get the component configuration for this view.
     *
     * @return the component configuration
     */
    public ComponentConfiguration getComponentConfiguration() {
        return componentConfiguration;
    }

    /**
     * Get the view service name for this view.
     *
     * @return the view service name
     */
    public ServiceName getViewServiceName() {
        return viewServiceName;
    }

    /**
     * Get the view interceptor deque for a method.  These interceptors are run sequentially on the "server side" of an
     * invocation.  The interceptor factories are used every time a new view instance is constructed, called with a
     * new factory context each time.  The factory may return the same interceptor instance or a new interceptor
     * instance as appropriate.
     *
     * @param method the method to look up
     * @return the interceptor deque for this method
     */
    public Deque<InterceptorFactory> getViewInterceptorDeque(Method method) {
        Map<Method, Deque<InterceptorFactory>> map = viewInterceptors;
        Deque<InterceptorFactory> deque = map.get(method);
        if (deque == null) {
            map.put(method, deque = new ArrayDeque<InterceptorFactory>());
        }
        return deque;
    }

    /**
     * Get the client interceptor deque for a method.  These interceptors are run sequentially on the "client side" of an
     * invocation.  The interceptor factories are used every time a new client proxy instance is constructed, called with a
     * new factory context each time.  The factory may return the same interceptor instance or a new interceptor
     * instance as appropriate.
     *
     * @param method the method to look up
     * @return the interceptor deque for this method
     */
    public Deque<InterceptorFactory> getClientInterceptorDeque(Method method) {
        Map<Method, Deque<InterceptorFactory>> map = clientInterceptors;
        Deque<InterceptorFactory> deque = map.get(method);
        if (deque == null) {
            map.put(method, deque = new ArrayDeque<InterceptorFactory>());
        }
        return deque;
    }

    /**
     * Get the post-construct interceptor deque for view instances.
     *
     * @return the interceptor deque
     */
    public Deque<InterceptorFactory> getViewPostConstructInterceptors() {
        return viewPostConstructInterceptors;
    }

    /**
     * Get the post-construct interceptor deque for client proxy instances.
     *
     * @return the interceptor deque
     */
    public Deque<InterceptorFactory> getClientPostConstructInterceptors() {
        return clientPostConstructInterceptors;
    }

    /**
     * Get the pre-destroy interceptor deque for view instances.
     *
     * @return the interceptor deque
     */
    public Deque<InterceptorFactory> getViewPreDestroyInterceptors() {
        return viewPreDestroyInterceptors;
    }

    /**
     * Get the pre-destroy interceptor deque for client proxy instances.
     *
     * @return the interceptor deque
     */
    public Deque<InterceptorFactory> getClientPreDestroyInterceptors() {
        return clientPreDestroyInterceptors;
    }

    /**
     * Get the client proxy factory to use to construct proxy instances.
     *
     * @return the proxy factory
     */
    public ProxyFactory<?> getProxyFactory() {
        return proxyFactory;
    }

    /**
     * Get the binding configurations for this view.
     *
     * @return the binding configurations
     */
    public List<BindingConfiguration> getBindingConfigurations() {
        return bindingConfigurations;
    }

    /**
     * Get the view class.
     *
     * @return the view class
     */
    public Class<?> getViewClass() {
        return viewClass;
    }

    /**
     * Adds a "server side" interceptor which will be applicable for all methods exposed by this view.
     * 
     * @param  interceptorFactory The interceptor to add
     * @see #getViewInterceptorDeque(java.lang.reflect.Method)
     */
    public void addViewInterceptor(InterceptorFactory interceptorFactory) {
        Method[] allMethodsOnView = this.proxyFactory.getCachedMethods();
        for (Method method : allMethodsOnView) {
            Deque<InterceptorFactory> interceptorsForMethod = this.getViewInterceptorDeque(method);
            interceptorsForMethod.add(interceptorFactory);
        }
    }

    /**
     * Adds a "client side" interceptor which will be applicable for all methods exposed by this view.
     *
     * @param  interceptorFactory The interceptor to add
     * @see #getClientInterceptorDeque(java.lang.reflect.Method) 
     */
    public void addClientViewInterceptor(InterceptorFactory interceptorFactory) {
        Method[] allMethodsOnView = this.proxyFactory.getCachedMethods();
        for (Method method : allMethodsOnView) {
            Deque<InterceptorFactory> interceptorsForMethod = this.getClientInterceptorDeque(method);
            interceptorsForMethod.add(interceptorFactory);
        }
    }
}
