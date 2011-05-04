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

import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.invocation.InterceptorFactory;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The construction parameter set passed in to an abstract component.
 * <p/>
 * <h4>Interceptors</h4>
 * The interceptor factories provided herein are assembled from the component's EE module class as well as the EE
 * module classes of the declared interceptor classes for this component by way of a configurator.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class ComponentConfiguration {

    private final ComponentDescription componentDescription;

    // Core component config
    private final EEModuleClassConfiguration moduleClassConfiguration;
    private ComponentCreateServiceFactory componentCreateServiceFactory = ComponentCreateServiceFactory.BASIC;

    // Interceptor config
    private final Deque<InterceptorFactory> postConstructInterceptors = new ArrayDeque<InterceptorFactory>();
    private final Deque<InterceptorFactory> preDestroyInterceptors = new ArrayDeque<InterceptorFactory>();
    private final Map<Method, Deque<InterceptorFactory>> componentInterceptors = new IdentityHashMap<Method, Deque<InterceptorFactory>>();

    private final List<InterceptorFactory> componentSystemInterceptorFactories = new ArrayList<InterceptorFactory>();
    private final List<InterceptorFactory> componentInstanceSystemInterceptorFactories = new ArrayList<InterceptorFactory>();

    // Component instance management
    private ManagedReferenceFactory instanceFactory;

    private final List<DependencyConfigurator> createDependencies = new ArrayList<DependencyConfigurator>();
    private final List<DependencyConfigurator> startDependencies = new ArrayList<DependencyConfigurator>();

    // Bindings
    private final List<BindingConfiguration> bindingConfigurations = new ArrayList<BindingConfiguration>();

    // Views
    private final List<ViewConfiguration> views = new ArrayList<ViewConfiguration>();

    public ComponentConfiguration(final ComponentDescription componentDescription, final EEModuleClassConfiguration moduleClassConfiguration) {
        this.componentDescription = componentDescription;
        this.moduleClassConfiguration = moduleClassConfiguration;
        this.instanceFactory = new DefaultConstructorManagedReferenceFactory(moduleClassConfiguration.getModuleClass());
    }

    /**
     * Get the component description.
     *
     * @return the component description
     */
    public ComponentDescription getComponentDescription() {
        return componentDescription;
    }

    /**
     * Get the component class.
     *
     * @return the component class
     */
    public Class<?> getComponentClass() {
        return moduleClassConfiguration.getModuleClass();
    }

    /**
     * Get the component name.
     *
     * @return the component name
     */
    public String getComponentName() {
        return componentDescription.getComponentName();
    }

    /**
     * Get the set of currently known component methods.  This is an identity set.
     *
     * @return the set of methods
     */
    public Set<Method> getDefinedComponentMethods() {
        return componentInterceptors.keySet();
    }

    /**
     * Get the interceptor deque for a component method, creating a new one if necessary.
     *
     * @param method the component method
     * @return the deque
     */
    public Deque<InterceptorFactory> getComponentInterceptorDeque(Method method) {
        Map<Method, Deque<InterceptorFactory>> map = componentInterceptors;
        Deque<InterceptorFactory> deque = map.get(method);
        if (deque == null) {
            map.put(method, deque = new ArrayDeque<InterceptorFactory>());
        }
        return deque;
    }

    /**
     * Get the binding configurations for this component.  This list contains bindings which are specific to the
     * component.
     *
     * @return the binding configurations
     */
    public List<BindingConfiguration> getBindingConfigurations() {
        return bindingConfigurations;
    }

    /**
     * Get the create dependencies list.
     *
     * @return the create dependencies list
     */
    public List<DependencyConfigurator> getCreateDependencies() {
        return createDependencies;
    }

    /**
     * Get the start dependencies list.
     *
     * @return the start dependencies list
     */
    public List<DependencyConfigurator> getStartDependencies() {
        return startDependencies;
    }

    /**
     * Get the list of views for this component.
     *
     * @return the list of views
     */
    public List<ViewConfiguration> getViews() {
        return views;
    }

    /**
     * Get the post-construct interceptor deque.
     *
     * @return the deque
     */
    public Deque<InterceptorFactory> getPostConstructInterceptors() {
        return postConstructInterceptors;
    }

    /**
     * Get the pre-destroy interceptor deque.
     *
     * @return the deque
     */
    public Deque<InterceptorFactory> getPreDestroyInterceptors() {
        return preDestroyInterceptors;
    }

    /**
     * Get the application name.
     *
     * @return the application name
     */
    public String getApplicationName() {
        return componentDescription.getApplicationName();
    }

    /**
     * Get the module name.
     *
     * @return the module name
     */
    public String getModuleName() {
        return componentDescription.getModuleName();
    }

    /**
     * Get the instance factory for this component.
     *
     * @return the instance factory
     */
    public ManagedReferenceFactory getInstanceFactory() {
        return instanceFactory;
    }

    /**
     * Set the instance factory for this component.
     *
     * @param instanceFactory the instance factory
     */
    public void setInstanceFactory(final ManagedReferenceFactory instanceFactory) {
        this.instanceFactory = instanceFactory;
    }

    public EEModuleClassConfiguration getModuleClassConfiguration() {
        return moduleClassConfiguration;
    }

    /**
     * Get the component create service factory for this component.
     *
     * @return the component create service factory
     */
    public ComponentCreateServiceFactory getComponentCreateServiceFactory() {
        return componentCreateServiceFactory;
    }

    public List<InterceptorFactory> getComponentInstanceSystemInterceptorFactories() {
        return componentInstanceSystemInterceptorFactories;
    }

    /**
     * Set the component create service factory for this component.
     *
     * @param componentCreateServiceFactory the component create service factory
     */
    public void setComponentCreateServiceFactory(final ComponentCreateServiceFactory componentCreateServiceFactory) {
        if (componentCreateServiceFactory == null) {
            throw new IllegalArgumentException("componentCreateServiceFactory is null");
        }
        this.componentCreateServiceFactory = componentCreateServiceFactory;
    }

    public List<InterceptorFactory> getComponentSystemInterceptorFactories() {
        return componentSystemInterceptorFactories;
    }

    public String toString() {
        return getClass().getName() + "[name=" + componentDescription.getComponentName() + " class=" + componentDescription.getComponentClassName() + "]";
    }
}
