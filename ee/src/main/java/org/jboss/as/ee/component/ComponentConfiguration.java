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

import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.ee.component.interceptors.OrderedItemContainer;
import org.jboss.as.ee.concurrent.ConcurrentContext;
import org.jboss.as.naming.context.NamespaceContextSelector;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.Service;

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
    private final ClassReflectionIndex classIndex;
    private final ModuleLoader moduleLoader;
    private final ClassLoader moduleClassLoader;

    private final ConcurrentContext concurrentContext;

    private ComponentCreateServiceFactory componentCreateServiceFactory = ComponentCreateServiceFactory.BASIC;

    // Interceptor config
    private final OrderedItemContainer<List<InterceptorFactory>> aroundConstructInterceptors = new OrderedItemContainer<>();
    private final OrderedItemContainer<List<InterceptorFactory>> postConstructInterceptors = new OrderedItemContainer<>();
    private final OrderedItemContainer<List<InterceptorFactory>> preDestroyInterceptors = new OrderedItemContainer<>();
    private final OrderedItemContainer<List<InterceptorFactory>> prePassivateInterceptors = new OrderedItemContainer<>();
    private final OrderedItemContainer<List<InterceptorFactory>> postActivateInterceptors = new OrderedItemContainer<>();
    private final Map<Method, OrderedItemContainer<List<InterceptorFactory>>> componentInterceptors = new IdentityHashMap<>();

    //TODO: move this into an EJB specific configuration
    private final Map<Method, OrderedItemContainer<InterceptorFactory>> timeoutInterceptors = new IdentityHashMap<>();

    // Component instance management
    private ComponentFactory instanceFactory;

    private final List<DependencyConfigurator<? extends Service<Component>>> createDependencies = new ArrayList<DependencyConfigurator<? extends Service<Component>>>();
    private final List<DependencyConfigurator<ComponentStartService>> startDependencies = new ArrayList<DependencyConfigurator<ComponentStartService>>();

    // Views
    private final List<ViewConfiguration> views = new ArrayList<ViewConfiguration>();

    private InterceptorFactory namespaceContextInterceptorFactory;

    private NamespaceContextSelector namespaceContextSelector;

    private final Set<Object> interceptorContextKeys = new HashSet<Object>();

    /**
     * Contains a set of all lifecycle methods defined by the bean
     */
    private final Set<Method> lifecycleMethods = new HashSet<>();

    public ComponentConfiguration(final ComponentDescription componentDescription, final ClassReflectionIndex classIndex, final ClassLoader moduleClassLoader, final ModuleLoader moduleLoader) {
        this.componentDescription = componentDescription;
        this.classIndex = classIndex;
        this.moduleClassLoader = moduleClassLoader;
        this.moduleLoader = moduleLoader;
        this.concurrentContext = new ConcurrentContext();
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
        return classIndex.getIndexedClass();
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
        Map<Method, OrderedItemContainer<List<InterceptorFactory>>> map = componentInterceptors;
        OrderedItemContainer<List<InterceptorFactory>> interceptors = map.get(method);
        if (interceptors == null) {
            return Collections.emptyList();
        }
        List<List<InterceptorFactory>> sortedItems = interceptors.getSortedItems();
        List<InterceptorFactory> ret = new ArrayList<>();
        for(List<InterceptorFactory> item : sortedItems) {
            ret.addAll(item);
        }
        return ret;
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
        addComponentInterceptors(Collections.singletonList(factory), priority, publicOnly);
    }
    /**
     * Adds an interceptor factory to every method on the component.
     *
     * @param factory    The interceptor factory to add
     * @param priority   The interceptors relative order
     * @param publicOnly If true then then interceptor is only added to public methods
     */
    public void addComponentInterceptors(List<InterceptorFactory> factory, int priority, boolean publicOnly) {
        for (Method method : (Iterable<Method>)classIndex.getClassMethods()) {
            if (publicOnly && !Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            OrderedItemContainer<List<InterceptorFactory>> interceptors = componentInterceptors.get(method);
            if (interceptors == null) {
                componentInterceptors.put(method, interceptors = new OrderedItemContainer<List<InterceptorFactory>>());
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
        addComponentInterceptors(method, Collections.singletonList(factory), priority);
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
    public void addComponentInterceptors(Method method, List<InterceptorFactory> factory, int priority) {
        OrderedItemContainer<List<InterceptorFactory>> interceptors = componentInterceptors.get(method);
        if (interceptors == null) {
            componentInterceptors.put(method, interceptors = new OrderedItemContainer<List<InterceptorFactory>>());
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
        for (Method method : (Iterable<Method>)classIndex.getClassMethods()) {
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
     * Get the around-construct interceptors.
     * <p/>
     * This method should only be called after all interceptors have been added
     *
     * @return the sorted interceptors
     */
    public List<InterceptorFactory> getAroundConstructInterceptors() {
        List<List<InterceptorFactory>> sortedItems = aroundConstructInterceptors.getSortedItems();
        List<InterceptorFactory> interceptorFactories = new ArrayList<>();
        for(List<InterceptorFactory> i : sortedItems) {
            interceptorFactories.addAll(i);
        }
        return interceptorFactories;
    }

    /**
     * Adds an around-construct interceptor
     *
     * @param factories The interceptors to add
     * @param priority           The priority
     */
    public void addAroundConstructInterceptors(List<InterceptorFactory> factories, int priority) {
        aroundConstructInterceptors.add(factories, priority);
    }
    /**
     * Adds an around-construct interceptor
     *
     * @param interceptorFactory The interceptor to add
     * @param priority           The priority
     */
    public void addAroundConstructInterceptor(InterceptorFactory interceptorFactory, int priority) {
        aroundConstructInterceptors.add(Collections.singletonList(interceptorFactory), priority);
    }

    /**
     * Get the post-construct interceptors.
     * <p/>
     * This method should only be called after all interceptors have been added
     *
     * @return the sorted interceptors
     */
    public List<InterceptorFactory> getPostConstructInterceptors() {
        List<List<InterceptorFactory>> sortedItems = postConstructInterceptors.getSortedItems();
        List<InterceptorFactory> interceptorFactories = new ArrayList<>();
        for(List<InterceptorFactory> i : sortedItems) {
            interceptorFactories.addAll(i);
        }
        return interceptorFactories;
    }

    /**
     * Adds a post construct interceptor
     *
     * @param interceptorFactory The interceptor to add
     * @param priority           The priority
     */
    public void addPostConstructInterceptors(List<InterceptorFactory> interceptorFactory, int priority) {
        postConstructInterceptors.add(interceptorFactory, priority);
    }

    /**
     * Adds a post construct interceptor
     *
     * @param interceptorFactory The interceptor to add
     * @param priority           The priority
     */
    public void addPostConstructInterceptor(InterceptorFactory interceptorFactory, int priority) {
        postConstructInterceptors.add(Collections.singletonList(interceptorFactory), priority);
    }
    /**
     * Get the pre-destroy interceptors.
     * <p/>
     * This method should only be called after all interceptors have been added
     *
     * @return the sorted interceptor
     */
    public List<InterceptorFactory> getPreDestroyInterceptors() {
        List<List<InterceptorFactory>> sortedItems = preDestroyInterceptors.getSortedItems();
        List<InterceptorFactory> interceptorFactories = new ArrayList<>();
        for(List<InterceptorFactory> i : sortedItems) {
            interceptorFactories.addAll(i);
        }
        return interceptorFactories;
    }

    /**
     * Adds a pre destroy interceptor
     *
     * @param factories The interceptor factory to add
     * @param priority           The factories priority
     */
    public void addPreDestroyInterceptors(List<InterceptorFactory> factories, int priority) {
        preDestroyInterceptors.add(factories, priority);
    }
    /**
     * Adds a pre destroy interceptor
     *
     * @param interceptorFactory The interceptor factory to add
     * @param priority           The factories priority
     */
    public void addPreDestroyInterceptor(InterceptorFactory interceptorFactory, int priority) {
        preDestroyInterceptors.add(Collections.singletonList(interceptorFactory), priority);
    }

    /**
     * Get the pre-passivate interceptors.
     * <p/>
     * This method should only be called after all interceptors have been added
     *
     * @return the sorted interceptors
     */
    public List<InterceptorFactory> getPrePassivateInterceptors() {
        List<List<InterceptorFactory>> sortedItems = prePassivateInterceptors.getSortedItems();
        List<InterceptorFactory> interceptorFactories = new ArrayList<>();
        for(List<InterceptorFactory> i : sortedItems) {
            interceptorFactories.addAll(i);
        }
        return interceptorFactories;
    }

    /**
     * Adds a pre passivate interceptor
     *
     * @param factories The interceptor to add
     * @param priority           The priority
     */
    public void addPrePassivateInterceptors(List<InterceptorFactory> factories, int priority) {
        prePassivateInterceptors.add(factories, priority);
    }

    /**
     * Adds a pre passivate interceptor
     *
     * @param interceptorFactory The interceptor to add
     * @param priority           The priority
     */
    public void addPrePassivateInterceptor(InterceptorFactory interceptorFactory, int priority) {
        prePassivateInterceptors.add(Collections.singletonList(interceptorFactory), priority);
    }

    /**
     * Get the post-activate interceptors.
     * <p/>
     * This method should only be called after all interceptors have been added
     *
     * @return the sorted interceptors
     */
    public List<InterceptorFactory> getPostActivateInterceptors() {
        List<List<InterceptorFactory>> sortedItems = postActivateInterceptors.getSortedItems();
        List<InterceptorFactory> interceptorFactories = new ArrayList<>();
        for(List<InterceptorFactory> i : sortedItems) {
            interceptorFactories.addAll(i);
        }
        return interceptorFactories;
    }

    /**
     * Adds a post activate interceptor
     *
     * @param interceptorFactory The interceptor to add
     * @param priority           The priority
     */
    public void addPostActivateInterceptors(List<InterceptorFactory> interceptorFactory, int priority) {
        postActivateInterceptors.add(interceptorFactory, priority);
    }

    /**
     * Adds a post activate interceptor
     *
     * @param interceptorFactory The interceptor to add
     * @param priority           The priority
     */
    public void addPostActivateInterceptor(InterceptorFactory interceptorFactory, int priority) {
        postActivateInterceptors.add(Collections.singletonList(interceptorFactory), priority);
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
    public ComponentFactory getInstanceFactory() {
        return instanceFactory;
    }

    /**
     * Set the instance factory for this component.
     *
     * @param instanceFactory the instance factory
     */
    public void setInstanceFactory(final ComponentFactory instanceFactory) {
        this.instanceFactory = instanceFactory;
    }

    public ClassReflectionIndex getClassIndex() {
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
            throw EeLogger.ROOT_LOGGER.nullVar("componentCreateServiceFactory", "component", getComponentName());
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

    public ClassLoader getModuleClassLoader() {
        return moduleClassLoader;
    }

    public ModuleLoader getModuleLoader() {
        return moduleLoader;
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

    public ConcurrentContext getConcurrentContext() {
        return concurrentContext;
    }

    /**
     * Adds a lifecycle method to the lifecycle methods set
     *
     * @param method The lifecycle method
     */
    public void addLifecycleMethod(Method method) {
        lifecycleMethods.add(method);
    }

    /**
     * Returns a set of all lifecycle methods defined on the bean
     *
     * @return All lifecycle methods defined on the component class and its superclasses
     */
    public Set<Method> getLifecycleMethods() {
        return Collections.unmodifiableSet(lifecycleMethods);
    }
}
