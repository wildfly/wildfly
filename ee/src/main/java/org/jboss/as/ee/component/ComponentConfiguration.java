/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jboss.as.ee.component.injection.ResourceInjectableConfiguration;
import org.jboss.as.ee.component.injection.ResourceInjectionDependency;
import org.jboss.as.ee.component.interceptor.ComponentInterceptorFactories;
import org.jboss.as.ee.component.lifecycle.ComponentLifecycle;
import org.jboss.as.ee.component.lifecycle.ComponentLifecycleConfiguration;
import org.jboss.as.ee.component.interceptor.MethodInterceptorConfiguration;
import org.jboss.msc.service.ServiceName;

/**
 * The configuration for a {@link Component} use for constructing and installing a component.
 *
 * @author John Bailey
 */
public class ComponentConfiguration extends ResourceInjectableConfiguration {
    private final String name;
    private final String componentClassName;
    private final ComponentFactory componentFactory;
    private final List<ComponentLifecycleConfiguration> postConstructConfiguration = new ArrayList<ComponentLifecycleConfiguration>();
    private final List<ComponentLifecycle> postConstructLifecycles = new ArrayList<ComponentLifecycle>();
    private final List<ComponentLifecycleConfiguration> preDestroyConfigurations = new ArrayList<ComponentLifecycleConfiguration>();
    private final List<ComponentLifecycle> preDestroyLifecycles = new ArrayList<ComponentLifecycle>();
    private final List<MethodInterceptorConfiguration> interceptorConfigurations = new ArrayList<MethodInterceptorConfiguration>();
    private final Set<ResourceInjectionDependency<?>> dependencies = new HashSet<ResourceInjectionDependency<?>>();
    private final ComponentInterceptorFactories componentInterceptorFactories = new ComponentInterceptorFactories();

    private Class<?> componentClass;
    private ServiceName bindContextServiceName;
    private ServiceName envContextServiceName;
    private ServiceName compContextServiceName;
    private ServiceName moduleContextServiceName;
    private ServiceName appContextServiceName;

    public ComponentConfiguration(final String name, final String componentClassName, final ComponentFactory componentFactory) {
        if (name == null) throw new IllegalArgumentException("Component name can not be null");
        this.name = name;
        if (componentClassName == null) throw new IllegalArgumentException("Component class can not be null");
        this.componentClassName = componentClassName;
        if (componentFactory == null) throw new IllegalArgumentException("Component factory can not be null");
        this.componentFactory = componentFactory;
    }

    /**
     * The component name.  This will often reflect the name of the EE component.
     *
     * @return The component name
     */
    public String getName() {
        return name;
    }

    /**
     * The component's class name.
     *
     * @return The bean class
     */
    public String getComponentClassName() {
        return componentClassName;
    }

    /**
     * The component factory
     *
     * @return The component factory
     */
    public ComponentFactory getComponentFactory() {
        return componentFactory;
    }

    /**
     * The component class
     *
     * @return The class
     */
    public Class<?> getComponentClass() {
        return componentClass;
    }

    /**
     * Set the component class
     *
     * @param componentClass the component class
     */
    public void setComponentClass(Class<?> componentClass) {
        this.componentClass = componentClass;
    }

    /**
     * The post-construct life-cycle methods.
     *
     * @return The post-construct life-cycle methods
     */
    public List<ComponentLifecycleConfiguration> getPostConstructLifecycleConfigurations() {
        return Collections.unmodifiableList(postConstructConfiguration);
    }

    /**
     * Add a post construct method to the configuration.
     *
     * @param postMethod The post-construct method
     */
    public void addPostConstructLifecycleConfiguration(final ComponentLifecycleConfiguration postMethod) {
        postConstructConfiguration.add(postMethod);
    }

    /**
     * The pre-destroy life-cycle methods.
     *
     * @return The pre-destroy life-cycle methods
     */
    public List<ComponentLifecycleConfiguration> getPreDestroyLifecycleConfigurations() {
        return Collections.unmodifiableList(preDestroyConfigurations);
    }

    /**
     * Add a pre-destroy method to the configuration.
     *
     * @param preDestroy The pre-destry method
     */
    public void addPreDestroyLifecycleConfiguration(final ComponentLifecycleConfiguration preDestroy) {
        preDestroyConfigurations.add(preDestroy);
    }

    /**
     * The configurations for any method interceptors for this bean type.
     *
     * @return The method interceptor configurations
     */
    public List<MethodInterceptorConfiguration> getMethodInterceptorConfigs() {
        return Collections.unmodifiableList(interceptorConfigurations);
    }

    /**
     * Add a method interceptor configuration to the bean configuration.
     *
     * @param interceptorConfiguration The interceptor configuration
     */
    public void addMethodInterceptorConfig(final MethodInterceptorConfiguration interceptorConfiguration) {
        interceptorConfigurations.add(interceptorConfiguration);
    }

    /**
     * Add method interceptor configurations to the bean configuration.
     *
     * @param interceptorConfigurations The interceptor configurations
     */
    public void addMethodInterceptorConfigs(final MethodInterceptorConfiguration... interceptorConfigurations) {
        for (MethodInterceptorConfiguration config : interceptorConfigurations) {
            addMethodInterceptorConfig(config);
        }
    }

    /**
     * Add method interceptor configurations to the bean configuration.
     *
     * @param interceptorConfigurations The interceptor configurations
     */
    public void addMethodInterceptorConfigs(final Collection<MethodInterceptorConfiguration> interceptorConfigurations) {
        this.interceptorConfigurations.addAll(interceptorConfigurations);
    }

    /**
     * The service name of the naming context this component's reference will be bound.
     *
     * @return The context service name
     */
    public ServiceName getBindContextServiceName() {
        return bindContextServiceName;
    }

    public void setBindContextServiceName(ServiceName bindContextServiceName) {
        this.bindContextServiceName = bindContextServiceName;
    }

    /**
     * The name used when binding the component reference in the bind context.
     *
     * @return The bind name
     */
    public String getBindName() {
        return getName();
    }

    /**
     * The service name for the naming context this component's environment entries will be bound.
     *
     * @return The environment context service name
     */
    public ServiceName getEnvContextServiceName() {
        return envContextServiceName;
    }

    public void setEnvContextServiceName(ServiceName envContextServiceName) {
        this.envContextServiceName = envContextServiceName;
    }

    /**
     * The service name for the naming context for this component.
     *
     * @return The component naming context
     */
    public ServiceName getCompContextServiceName() {
        return compContextServiceName;
    }

    public void setCompContextServiceName(ServiceName compContextServiceName) {
        this.compContextServiceName = compContextServiceName;
    }

    /**
     * The service name for the module context for this component.
     *
     * @return The module context name
     */
    public ServiceName getModuleContextServiceName() {
        return moduleContextServiceName;
    }

    public void setModuleContextServiceName(ServiceName moduleContextServiceName) {
        this.moduleContextServiceName = moduleContextServiceName;
    }

    /**
     * The service name for the app context for this component.
     *
     * @return The app context name
     */
    public ServiceName getAppContextServiceName() {
        return appContextServiceName;
    }

    public void setAppContextServiceName(ServiceName appContextServiceName) {
        this.appContextServiceName = appContextServiceName;
    }

    /**
     * The dependencies generated by this resource injection.
     *
     * @return The dependencies
     */
    public Set<ResourceInjectionDependency<?>> getDependencies() {
        return dependencies;
    }

    public void addDependency(final ResourceInjectionDependency<?> dependency) {
        dependencies.add(dependency);
    }


    public List<ComponentLifecycle> getPostConstructLifecycles() {
        return postConstructLifecycles;
    }

    public void addPostConstructLifecycle(final ComponentLifecycle componentLifecycle) {
        postConstructLifecycles.add(componentLifecycle);
    }

    public List<ComponentLifecycle> getPreDestroyLifecycles() {
        return preDestroyLifecycles;
    }

    public void addPreDestroyLifecycel(final ComponentLifecycle componentLifecycle) {
        preDestroyLifecycles.add(componentLifecycle);
    }

    public ComponentInterceptorFactories getComponentInterceptorFactories() {
        return componentInterceptorFactories;
    }
}
