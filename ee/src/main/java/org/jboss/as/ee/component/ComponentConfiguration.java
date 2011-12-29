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
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.ee.component.interceptors.OrderedItemContainer;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.context.NamespaceContextSelector;
import org.jboss.as.server.deployment.reflect.ClassIndex;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.msc.service.Service;

import static org.jboss.as.ee.EeMessages.MESSAGES;

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
    private final ClassIndex classIndex;

    private final ClassLoader moduleClassLoder;

    private ComponentCreateServiceFactory componentCreateServiceFactory = ComponentCreateServiceFactory.BASIC;

    // Interceptor config
    private final OrderedItemContainer<InterceptorFactory> postConstructInterceptors = new OrderedItemContainer<InterceptorFactory>();
    private final OrderedItemContainer<InterceptorFactory> preDestroyInterceptors = new OrderedItemContainer<InterceptorFactory>();
    private final Map<Method, OrderedItemContainer<InterceptorFactory>> componentInterceptors = new IdentityHashMap<Method, OrderedItemContainer<InterceptorFactory>>();

    //TODO: move this into an EJB specific configuration
    private final Map<Method, OrderedItemContainer<InterceptorFactory>> timeoutInterceptors = new IdentityHashMap<Method, OrderedItemContainer<InterceptorFactory>>();

    // Component instance management
    private ManagedReferenceFactory instanceFactory;

    private final List<DependencyConfigurator<? extends Service<Component>>> createDependencies = new ArrayList<DependencyConfigurator<? extends Service<Component>>>();
    private final List<DependencyConfigurator<ComponentStartService>> startDependencies = new ArrayList<DependencyConfigurator<ComponentStartService>>();

    // Views
    private final List<ViewConfiguration> views = new ArrayList<ViewConfiguration>();

    private InterceptorFactory namespaceContextInterceptorFactory;

    private NamespaceContextSelector namespaceContextSelector;

    private final Set<Object> interceptorContextKeys = new HashSet<Object>();

    public ComponentConfiguration(final ComponentDescription componentDescription, final ClassIndex classIndex, final ClassLoader moduleClassLoder) {
        this.componentDescription = componentDescription;
        this.classIndex = classIndex;
        this.moduleClassLoder = moduleClassLoder;
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
        return classIndex.getModuleClass();
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
        return classIndex.getClassMethods();
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
     * Gets the around timeout interceptor list for a given method. This should not be called until
     * all interceptors have been added.
     *
     * @param method the component method
     * @return the deque
     */
    public List<InterceptorFactory> getAroundTimeoutInterceptors(Method method) {
        Map<Method, OrderedItemContainer<InterceptorFactory>> map = timeoutInterceptors;
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
        for (Method method : classIndex.getClassMethods()) {
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
     * Adds a timeout interceptor factory to every method on the component.
     *
     * @param factory  The interceptor factory to add
     * @param priority The interceptors relative order
     */
    public void addTimeoutViewInterceptor(InterceptorFactory factory, int priority) {
        for (Method method : classIndex.getClassMethods()) {
            OrderedItemContainer<InterceptorFactory> interceptors = timeoutInterceptors.get(method);
            if (interceptors == null) {
                timeoutInterceptors.put(method, interceptors = new OrderedItemContainer<InterceptorFactory>());
            }
            interceptors.add(factory, priority);
        }
    }

    /**
     * Adds a timeout interceptor factory to every method on the component.
     *
     * @param method   The method to add it to
     * @param factory  The interceptor factory to add
     * @param priority The interceptors relative order
     */
    public void addTimeoutViewInterceptor(final Method method, InterceptorFactory factory, int priority) {
        OrderedItemContainer<InterceptorFactory> interceptors = timeoutInterceptors.get(method);
        if (interceptors == null) {
            timeoutInterceptors.put(method, interceptors = new OrderedItemContainer<InterceptorFactory>());
        }
        interceptors.add(factory, priority);
    }


    /**
     * Get the create dependencies list.
     *
     * @return the create dependencies list
     */
    public List<DependencyConfigurator<? extends Service<Component>>> getCreateDependencies() {
        return createDependencies;
    }

    /**
     * Get the start dependencies list.
     *
     * @return the start dependencies list
     */
    public List<DependencyConfigurator<ComponentStartService>> getStartDependencies() {
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
     * <p/>
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
     * @param priority           The priority
     */
    public void addPostConstructInterceptor(InterceptorFactory interceptorFactory, int priority) {
        postConstructInterceptors.add(interceptorFactory, priority);
    }

    /**
     * Get the pre-destroy interceptors.
     * <p/>
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
     * @param priority           The factories priority
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

    public ClassIndex getClassIndex() {
        return classIndex;
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
            throw MESSAGES.nullVar("componentCreateServiceFactory");
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

    public ClassLoader getModuleClassLoder() {
        return moduleClassLoder;
    }

    /**
     * @return The components namespace context selector, if any
     */
    public NamespaceContextSelector getNamespaceContextSelector() {
        return namespaceContextSelector;
    }

    public void setNamespaceContextSelector(final NamespaceContextSelector namespaceContextSelector) {
        this.namespaceContextSelector = namespaceContextSelector;
    }

    public Set<Object> getInterceptorContextKeys() {
        return interceptorContextKeys;
    }
}
