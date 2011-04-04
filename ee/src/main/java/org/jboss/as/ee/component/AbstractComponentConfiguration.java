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

import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.proxy.ProxyFactory;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.InjectedValue;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * The construction parameter set passed in to an abstract component.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractComponentConfiguration {

    private final String componentName;
    private final Deque<InterceptorFactory> postConstruct = new ArrayDeque<InterceptorFactory>();
    private final Deque<InterceptorFactory> preDestroy = new ArrayDeque<InterceptorFactory>();;
    private final List<ResourceInjection> resourceInjections = new ArrayList<ResourceInjection>();
    private final Map<Class<?>,List<ResourceInjection>> interceptorResourceInjections = new IdentityHashMap<Class<?>,List<ResourceInjection>>();
    private final List<InterceptorFactory> componentSystemInterceptorFactories = new ArrayList<InterceptorFactory>();
    private final Map<Method, InterceptorFactory> interceptorFactoryMap = new IdentityHashMap<Method, InterceptorFactory>();
    private final Map<Class<?>, ProxyFactory<?>> proxyFactories = new IdentityHashMap<Class<?>, ProxyFactory<?>>();
    private final List<ComponentInjector> componentInjectors = new ArrayList<ComponentInjector>();
    private final Map<ServiceName, InjectedValue<Object>> dependencyInjections = new HashMap<ServiceName, InjectedValue<Object>>();
    private final Map<Class<?>, ServiceName> viewServices = new HashMap<Class<?>, ServiceName>();
    private Class<?> componentClass;
    private List<InterceptorFactory> componentInstanceSystemInterceptorFactories = new LinkedList<InterceptorFactory>();
    private Collection<Method> componentMethods;


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

    Map<Method, InterceptorFactory> getInterceptorFactoryMap() {
        return interceptorFactoryMap;
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

    public <T> T getInjectionValue(ServiceName dependencyName, Class<T> valueType) {
        return valueType.cast(getInjection(dependencyName).getValue());
    }

    Map<ServiceName, InjectedValue<Object>> getDependencyInjections() {
        return dependencyInjections;
    }


    protected Map<Class<?>, ServiceName> getViewServices() {
        return viewServices;
    }

    /**
     * Construct a component by passing this configuration in to the component constructor.
     *
     * @return the component instance
     */
    public abstract AbstractComponent constructComponent();

    protected void addComponentInstanceSystemInterceptorFactory(InterceptorFactory factory) {
        componentInstanceSystemInterceptorFactories.add(factory);
    }

    List<? extends InterceptorFactory> getComponentInstanceSystemInterceptorFactories() {
        return componentInstanceSystemInterceptorFactories;
    }

    @Deprecated
    Collection<Method> getComponentMethods() {
        return componentMethods;
    }

    @Deprecated
    void setComponentMethods(Collection<Method> componentMethods) {
        this.componentMethods = componentMethods;
    }

    public Deque<InterceptorFactory> getPostConstruct() {
        return postConstruct;
    }

    public Deque<InterceptorFactory> getPreDestroy() {
        return preDestroy;
    }
}
