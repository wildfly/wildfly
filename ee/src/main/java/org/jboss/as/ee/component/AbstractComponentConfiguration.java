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
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.proxy.ProxyFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.InjectedValue;

/**
 * The construction parameter set passed in to an abstract component.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractComponentConfiguration {

    private final String componentName;
    private final List<ComponentLifecycle> postConstructLifecycles = new ArrayList<ComponentLifecycle>();
    private final List<ComponentLifecycle> preDestroyLifecycles = new ArrayList<ComponentLifecycle>();
    private final List<LifecycleInterceptorFactory> postConstructInterceptorLifecycles = new ArrayList<LifecycleInterceptorFactory>();
    private final List<LifecycleInterceptorFactory> preDestroyInterceptorLifecycles = new ArrayList<LifecycleInterceptorFactory>();
    private final List<ResourceInjection> resourceInjections = new ArrayList<ResourceInjection>();
    private final Map<Class<?>,List<ResourceInjection>> interceptorResourceInjections = new IdentityHashMap<Class<?>,List<ResourceInjection>>();
    private final List<InterceptorFactory> componentSystemInterceptorFactories = new ArrayList<InterceptorFactory>();
    private final Map<Class<?>, ComponentInvocationHandler> views = new IdentityHashMap<Class<?>, ComponentInvocationHandler>();
    private final Map<Method, InterceptorFactory> interceptorFactoryMap = new IdentityHashMap<Method, InterceptorFactory>();
    private final Map<Class<?>, ProxyFactory<?>> proxyFactories = new IdentityHashMap<Class<?>, ProxyFactory<?>>();
    private final List<ComponentInjector> componentInjectors = new ArrayList<ComponentInjector>();
    private final Map<ServiceName, InjectedValue<Object>> dependencyInjections = new HashMap<ServiceName, InjectedValue<Object>>();
    private Class<?> componentClass;
    private Interceptor componentInterceptor;

    /**
     * Construct a new instance.
     *
     * @param description the original component description
     */
    protected AbstractComponentConfiguration(final AbstractComponentDescription description) {
        // Do not keep a reference to description, so it can be GC-ed.
        this.componentName = description.getComponentName();
    }

    /**
     * Get the component class.
     *
     * @return the component class
     */
    public Class<?> getComponentClass() {
        return componentClass;
    }

    public String getComponentName() {
        return componentName;
    }

    /**
     * Set the component class.
     *
     * @param componentClass the component class
     */
    public void setComponentClass(final Class<?> componentClass) {
        this.componentClass = componentClass;
    }

    public void addPostConstructLifecycle(ComponentLifecycle lifecycle) {
        postConstructLifecycles.add(lifecycle);
    }

    List<ComponentLifecycle> getPostConstructLifecycles() {
        return postConstructLifecycles;
    }

    public void addPostConstructInterceptorLifecycle(LifecycleInterceptorFactory lifecycle) {
        postConstructInterceptorLifecycles.add(lifecycle);
    }

    List<LifecycleInterceptorFactory> getPostConstructInterceptorLifecycles() {
        return postConstructInterceptorLifecycles;
    }

    public void addPreDestroyLifecycle(ComponentLifecycle lifecycle) {
        preDestroyLifecycles.add(lifecycle);
    }

    List<ComponentLifecycle> getPreDestroyLifecycles() {
        return preDestroyLifecycles;
    }

    public void addPreDestroyInterceptorLifecycle(LifecycleInterceptorFactory lifecycle) {
        preDestroyInterceptorLifecycles.add(lifecycle);
    }

    List<LifecycleInterceptorFactory> getPreDestroyInterceptorLifecycles() {
        return preDestroyInterceptorLifecycles;
    }

    public void addInterceptorResourceInjection(Class<?> interceptorClass, List<ResourceInjection> resourceInjections) {
        interceptorResourceInjections.put(interceptorClass, new ArrayList<ResourceInjection>(resourceInjections));
    }

    public List<ResourceInjection> getInterceptorResourceInjections(Class<?> interceptorClass) {
        return interceptorResourceInjections.get(interceptorClass);
    }

    List<ResourceInjection> getResourceInjections() {
        return resourceInjections;
    }

    protected List<InterceptorFactory> getComponentSystemInterceptorFactories() {
        return componentSystemInterceptorFactories;
    }

    Map<Class<?>, ComponentInvocationHandler> getViews() {
        return views;
    }

    Map<Method, InterceptorFactory> getInterceptorFactoryMap() {
        return interceptorFactoryMap;
    }

    Interceptor getComponentInterceptor() {
        return componentInterceptor;
    }

    void setComponentInterceptor(final Interceptor componentInterceptor) {
        this.componentInterceptor = componentInterceptor;
    }

    Map<Class<?>,ProxyFactory<?>> getProxyFactories() {
        return proxyFactories;
    }

    public void addComponentInjector(ComponentInjector injector) {
        this.componentInjectors.add(injector);
    }

    public List<ComponentInjector> getComponentInjectors() {
        return Collections.unmodifiableList(componentInjectors);
    }

    /**
     * Get an injected value from a service name.
     *
     * @param dependencyName the dependency name
     * @return the injected value holder
     */
    public InjectedValue<?> getInjection(ServiceName dependencyName) {
        final InjectedValue<?> injection = dependencyInjections.get(dependencyName);
        if (injection == null) {
            throw new IllegalStateException("No injection found for dependency " + dependencyName);
        }
        return injection;
    }

    Map<ServiceName, InjectedValue<Object>> getDependencyInjections() {
        return dependencyInjections;
    }

    /**
     * Construct a component by passing this configuration in to the component constructor.
     *
     * @return the component instance
     */
    public abstract AbstractComponent constructComponent();
}
