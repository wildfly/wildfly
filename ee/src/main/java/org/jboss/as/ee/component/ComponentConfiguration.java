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

import org.jboss.as.ee.component.interceptors.OrderedItemContainer;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.invocation.InterceptorFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
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
    private final OrderedItemContainer<InterceptorFactory> postConstructInterceptors = new OrderedItemContainer<InterceptorFactory>();
    private final OrderedItemContainer<InterceptorFactory> preDestroyInterceptors = new OrderedItemContainer<InterceptorFactory>();
    private final Map<Method, OrderedItemContainer<InterceptorFactory>> componentInterceptors = new IdentityHashMap<Method, OrderedItemContainer<InterceptorFactory>>();

    // Component instance management
    private ManagedReferenceFactory instanceFactory;

    private final List<DependencyConfigurator> createDependencies = new ArrayList<DependencyConfigurator>();
    private final List<DependencyConfigurator> startDependencies = new ArrayList<DependencyConfigurator>();

    // Views
    private final List<ViewConfiguration> views = new ArrayList<ViewConfiguration>();

    private InterceptorFactory namespaceContextInterceptorFactory;

    public ComponentConfiguration(final ComponentDescription componentDescription, final EEModuleClassConfiguration moduleClassConfiguration) {
        this.componentDescription = componentDescription;
        this.moduleClassConfiguration = moduleClassConfiguration;
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
        return moduleClassConfiguration.getClassMethods();
    }

    /**
     * Gets the interceptor list for a given method. This should not be called until
     * all interceptors have been added.
     *
     * @param method the component method
     * @return the deque
     */
    public List<InterceptorFactory> getComponentInterceptors(Method method) {
        Map<Method, OrderedItemContainer<InterceptorFactory>> map = componentInterceptors;
        OrderedItemContainer<InterceptorFactory> interceptors = map.get(method);
        if (interceptors == null) {
            return Collections.emptyList();
        }
        return interceptors.getSortedItems();
    }

    /**
     * Adds an interceptor factory to every method on the component.
     *
     * @param factory    The interceptor factory to add
     * @param priority   The interceptors relative order
     * @param publicOnly If true then then interceptor is only added to public methods
     */
    public void addComponentInterceptor(InterceptorFactory factory, int priority, boolean publicOnly) {
        for (Method method : moduleClassConfiguration.getClassMethods()) {
            if (publicOnly && !Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            OrderedItemContainer<InterceptorFactory> interceptors = componentInterceptors.get(method);
            if (interceptors == null) {
                componentInterceptors.put(method, interceptors = new OrderedItemContainer<InterceptorFactory>());
            }
            interceptors.add(factory, priority);
        }
    }

    /**
     * Adds an interceptor factory to a given method. The method parameter *must* be retrived from either the
     * {@link org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex} or from {@link #getDefinedComponentMethods()},
     * as the methods are stored in an identity hash map
     *
     * @param method   The method to add the interceptor to
     * @param factory  The interceptor factory to add
     * @param priority The interceptors relative order
     */
    public void addComponentInterceptor(Method method, InterceptorFactory factory, int priority) {
        OrderedItemContainer<InterceptorFactory> interceptors = componentInterceptors.get(method);
        if (interceptors == null) {
            componentInterceptors.put(method, interceptors = new OrderedItemContainer<InterceptorFactory>());
        }
        interceptors.add(factory, priority);
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
     * Get the post-construct interceptors.
     *
     * This method should only be called after all interceptors have been added
     *
     * @return the sorted interceptors
     */
    public List<InterceptorFactory> getPostConstructInterceptors() {
        return postConstructInterceptors.getSortedItems();
    }

    /**
     * Adds a post construct interceptor
     *
     * @param interceptorFactory The interceptor to add
     * @param priority The priority
     */
    public void addPostConstructInterceptor(InterceptorFactory interceptorFactory, int priority) {
        postConstructInterceptors.add(interceptorFactory, priority);
    }

    /**
     * Get the pre-destroy interceptors.
     *
     * This method should only be called after all interceptors have been added
     *
     * @return the sorted interceptor
     */
    public List<InterceptorFactory> getPreDestroyInterceptors() {
        return preDestroyInterceptors.getSortedItems();
    }

    /**
     * Adds a pre destroy interceptor
     *
     * @param interceptorFactory The interceptor factory to add
     * @param priority The factories priority
     */
    public void addPreDestroyInterceptor(InterceptorFactory interceptorFactory, int priority) {
        preDestroyInterceptors.add(interceptorFactory, priority);
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

    public String toString() {
        return getClass().getName() + "[name=" + componentDescription.getComponentName() + " class=" + componentDescription.getComponentClassName() + "]";
    }

    public InterceptorFactory getNamespaceContextInterceptorFactory() {
        return namespaceContextInterceptorFactory;
    }

    public void setNamespaceContextInterceptorFactory(InterceptorFactory interceptorFactory) {
        this.namespaceContextInterceptorFactory = interceptorFactory;
    }
}
