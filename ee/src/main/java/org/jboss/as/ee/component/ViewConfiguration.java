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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.ee.component.interceptors.OrderedItemContainer;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.proxy.ProxyFactory;
import org.jboss.msc.service.ServiceName;

/**
 * A configuration of a component view.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Stuart Douglas
 */
public class ViewConfiguration {
    private final ComponentConfiguration componentConfiguration;
    private final ServiceName viewServiceName;
    private final Map<Method, OrderedItemContainer<InterceptorFactory>> viewInterceptors = new IdentityHashMap<Method, OrderedItemContainer<InterceptorFactory>>();
    private final Map<Method, OrderedItemContainer<InterceptorFactory>> clientInterceptors = new IdentityHashMap<Method, OrderedItemContainer<InterceptorFactory>>();
    private final OrderedItemContainer<InterceptorFactory> clientPostConstructInterceptors = new OrderedItemContainer<InterceptorFactory>();
    private final OrderedItemContainer<InterceptorFactory> clientPreDestroyInterceptors = new OrderedItemContainer<InterceptorFactory>();
    private final ProxyFactory<?> proxyFactory;
    private final List<BindingConfiguration> bindingConfigurations = new ArrayList<BindingConfiguration>();
    private final Class<?> viewClass;
    private final Set<Method> asyncMethods = new HashSet<Method>();
    private final Map<Class<?>, Object> privateData = new HashMap<Class<?>, Object>();
    private final List<DependencyConfigurator<ViewService>> dependencies = new ArrayList<DependencyConfigurator<ViewService>>();
    private final Map<Method, Method> viewToComponentMethodMap = new HashMap<>();
    private ViewInstanceFactory viewInstanceFactory;

    /**
     * Construct a new instance.
     *
     * @param viewClass              the view class
     * @param componentConfiguration the associated component configuration
     * @param viewServiceName        the service name of this view
     * @param proxyFactory           the proxy factory to use to locally construct client proxy instances
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
     * Get the view interceptors for a method.  These interceptors are run sequentially on the "server side" of an
     * invocation.  The interceptor factories are used every time a new view instance is constructed, called with a
     * new factory context each time.  The factory may return the same interceptor instance or a new interceptor
     * instance as appropriate.
     *
     * @param method the method to look up
     * @return the interceptors for this method
     */
    public List<InterceptorFactory> getViewInterceptors(Method method) {
        OrderedItemContainer<InterceptorFactory> container = viewInterceptors.get(method);
        if (container == null) {
            return Collections.emptyList();
        }
        return container.getSortedItems();
    }

    /**
     * Adds an interceptor factory to all methods of a view
     *
     * @param interceptorFactory The factory to add
     * @param priority           The interceptor order
     */
    public void addViewInterceptor(InterceptorFactory interceptorFactory, int priority) {
        for (Method method : proxyFactory.getCachedMethods()) {
            addViewInterceptor(method, interceptorFactory, priority);
        }
    }

    /**
     * Adds a view interceptor to the given method
     *
     * @param method             The method to add
     * @param interceptorFactory The interceptor factory
     * @param priority           The priority
     */
    public void addViewInterceptor(Method method, InterceptorFactory interceptorFactory, int priority) {
        OrderedItemContainer<InterceptorFactory> container = viewInterceptors.get(method);
        if (container == null) {
            viewInterceptors.put(method, container = new OrderedItemContainer<InterceptorFactory>());
        }
        container.add(interceptorFactory, priority);
    }

    /**
     * Get the client interceptors for a method.  These interceptors are run sequentially on the "client side" of an
     * invocation.  The interceptor factories are used every time a new client proxy instance is constructed, called with a
     * new factory context each time.  The factory may return the same interceptor instance or a new interceptor
     * instance as appropriate.
     *
     * @param method the method to look up
     * @return the interceptors for this method
     */
    public List<InterceptorFactory> getClientInterceptors(Method method) {
        OrderedItemContainer<InterceptorFactory> container = clientInterceptors.get(method);
        if (container == null) {
            return Collections.emptyList();
        }
        return container.getSortedItems();
    }

    /**
     * Adds a client interceptor factory to all methods of a view
     *
     * @param interceptorFactory The factory to add
     * @param priority           The interceptor order
     */
    public void addClientInterceptor(InterceptorFactory interceptorFactory, int priority) {
        for (Method method : proxyFactory.getCachedMethods()) {
            addClientInterceptor(method, interceptorFactory, priority);
        }
    }

    /**
     * Adds a client interceptor to the given method
     *
     * @param method             The method to add
     * @param interceptorFactory The interceptor factory
     * @param priority           The priority
     */
    public void addClientInterceptor(Method method, InterceptorFactory interceptorFactory, int priority) {
        OrderedItemContainer<InterceptorFactory> container = clientInterceptors.get(method);
        if (container == null) {
            clientInterceptors.put(method, container = new OrderedItemContainer<InterceptorFactory>());
        }
        container.add(interceptorFactory, priority);
    }

    /**
     * Get the post-construct interceptors for client proxy instances.
     * <p/>
     * This method should only be called after all interceptors have been added.
     *
     * @return the interceptors
     */
    public List<InterceptorFactory> getClientPostConstructInterceptors() {
        return clientPostConstructInterceptors.getSortedItems();
    }

    /**
     * Adds a client post construct interceptor
     *
     * @param interceptorFactory The interceptor
     * @param priority           The interceptor order
     */
    public void addClientPostConstructInterceptor(final InterceptorFactory interceptorFactory, final int priority) {
        clientPostConstructInterceptors.add(interceptorFactory, priority);
    }

    /**
     * Get the pre-destroy interceptors for client proxy instances.
     * <p/>
     * This method should only be called after all interceptors have been added.
     *
     * @return the interceptors
     */
    public List<InterceptorFactory> getClientPreDestroyInterceptors() {
        return clientPreDestroyInterceptors.getSortedItems();
    }

    /**
     * Adds a client pre-destroy interceptor
     *
     * @param interceptorFactory The interceptor
     * @param priority           The interceptor order
     */
    public void addClientPreDestroyInterceptor(final InterceptorFactory interceptorFactory, final int priority) {
        clientPreDestroyInterceptors.add(interceptorFactory, priority);
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
     * Gets all async methods for the view
     * @return The async methods
     */
    public Set<Method> getAsyncMethods() {
        return Collections.unmodifiableSet(asyncMethods);
    }

    /**
     * Marks a method on the view as asynchronous
     *
     * @param method The method
     */
    public void addAsyncMethod(Method method) {
        this.asyncMethods.add(method);
    }

    public ViewInstanceFactory getViewInstanceFactory() {
        return viewInstanceFactory;
    }

    /**
     *
     * @param viewInstanceFactory The instance factory that is used to create the view instances
     */
    public void setViewInstanceFactory(final ViewInstanceFactory viewInstanceFactory) {
        this.viewInstanceFactory = viewInstanceFactory;
    }

    /**
     * Attaches arbitrary private data to this view instance
     *
     * @param type The type of data
     * @param data The data
     */
    public <T> void putPrivateData(final Class<T> type, T data ) {
        privateData.put(type, data);
    }

    /**
     * retrieves private data
     */
    public Map<Class<?>, Object> getPrivateData() {
        return privateData;
    }

    public List<DependencyConfigurator<ViewService>> getDependencies() {
        return dependencies;
    }

    public Map<Method, Method> getViewToComponentMethodMap() {
        return viewToComponentMethodMap;
    }
}
