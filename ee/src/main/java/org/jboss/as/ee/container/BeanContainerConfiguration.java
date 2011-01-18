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

package org.jboss.as.ee.container;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.jboss.as.ee.container.injection.ResourceInjectableConfiguration;
import org.jboss.as.ee.container.interceptor.LifecycleInterceptorConfiguration;
import org.jboss.as.ee.container.interceptor.MethodInterceptorConfiguration;

/**
 * The configuration for a {@link BeanContainer} use for constructing and installing a bean container instance.
 *
 * @author John Bailey
 */
public class BeanContainerConfiguration extends ResourceInjectableConfiguration {
    private final String name;
    private final String beanClass;
    private final BeanContainerFactory beanContainerFactory;
    private final List<LifecycleInterceptorConfiguration> postConstructMethods = new ArrayList<LifecycleInterceptorConfiguration>();
    private final List<LifecycleInterceptorConfiguration> preDestroyMethods = new ArrayList<LifecycleInterceptorConfiguration>();
    private final List<MethodInterceptorConfiguration> interceptorConfigurations = new ArrayList<MethodInterceptorConfiguration>();

    public BeanContainerConfiguration(final String name, final String beanClass, final BeanContainerFactory beanContainerFactory) {
        if (name == null) throw new IllegalArgumentException("Bean name can not be null");
        this.name = name;
        if (beanClass == null) throw new IllegalArgumentException("Bean class can not be null");
        this.beanClass = beanClass;
        if (beanContainerFactory == null) throw new IllegalArgumentException("Container factory can not be null");
        this.beanContainerFactory = beanContainerFactory;
    }

    /**
     * The bean container name.  This will often reflect the name of the EE component.
     *
     * @return The bean container name
     */
    public String getName() {
        return name;
    }

    /**
     * The bean's class.
     *
     * @return The bean class
     */
    public String getBeanClass() {
        return beanClass;
    }

    /**
     * The post-construct life-cycle methods.
     *
     * @return The post-construct life-cycle methods
     */
    public List<LifecycleInterceptorConfiguration> getPostConstructLifecycles() {
        return Collections.unmodifiableList(postConstructMethods);
    }

    /**
     * Add a post construct method to the configuration.
     *
     * @param postMethod The post-construct method
     */
    public void addPostConstructLifecycle(final LifecycleInterceptorConfiguration postMethod) {
        postConstructMethods.add(postMethod);
    }

    /**
     * The pre-destroy life-cycle methods.
     *
     * @return The pre-destroy life-cycle methods
     */
    public List<LifecycleInterceptorConfiguration> getPreDestroyLifecycles() {
        return Collections.unmodifiableList(preDestroyMethods);
    }

    /**
     * Add a pre-destroy method to the configuration.
     *
     * @param preDestroy The pre-destry method
     */
    public void addPreDestroyLifecycle(final LifecycleInterceptorConfiguration preDestroy) {
        preDestroyMethods.add(preDestroy);
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
        interceptorConfigurations.addAll(interceptorConfigurations);
    }

    /**
     * The bean container factory for this bean type.
     *
     * @return The bean container factory
     */
    public BeanContainerFactory getContainerFactory() {
        return beanContainerFactory;
    }
}
