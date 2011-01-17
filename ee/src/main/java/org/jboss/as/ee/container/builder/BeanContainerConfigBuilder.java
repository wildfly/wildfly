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

package org.jboss.as.ee.container.builder;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.jboss.as.ee.container.BeanContainerConfig;
import org.jboss.as.ee.container.BeanContainerFactory;
import org.jboss.as.ee.container.interceptor.MethodInterceptorConfiguration;
import org.jboss.as.ee.container.injection.ResourceInjectionConfiguration;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Index;

/**
 * Builder used to create {@link BeanContainerConfig} instances.
 *
 * @author John Bailey
 */
public class BeanContainerConfigBuilder {
    private String name;
    private final Class<?> beanClass;
    private final ClassLoader beanClassLoader;
    private final List<ResourceInjectionConfiguration> resourceInjections = new ArrayList<ResourceInjectionConfiguration>();
    private final List<Method> postConstructMethods = new ArrayList<Method>();
    private final List<Method> preDestroyMethods = new ArrayList<Method>();
    private final List<MethodInterceptorConfiguration> interceptorConfigurations = new ArrayList<MethodInterceptorConfiguration>();
    private final BeanContainerFactory containerFactory;

    /**
     * Create a builder.
     *
     * @param beanClass The bean class
     * @param containerFactory The bean container factory
     * @return A new builder
     */
    public static BeanContainerConfigBuilder build(final Class<?> beanClass, final BeanContainerFactory containerFactory) {
        return build(beanClass, beanClass.getClassLoader(), containerFactory);
    }

    /**
     * Create a builder.
     *
     * @param beanClass The bean class
     * @param beanClassLoader The bean's classloader
     * @param containerFactory The bean container factory
     * @return A new builder
     */
    public static BeanContainerConfigBuilder build(final Class<?> beanClass, final ClassLoader beanClassLoader, final BeanContainerFactory containerFactory) {
        return new BeanContainerConfigBuilder(beanClass, beanClassLoader, containerFactory);
    }

    private BeanContainerConfigBuilder(final Class<?> beanClass, final ClassLoader beanClassLoader, final BeanContainerFactory containerFactory) {
        this.beanClass = beanClass;
        this.beanClassLoader = beanClassLoader;
        this.containerFactory = containerFactory;
    }

    /**
     * Create the {@link BeanContainerConfig} instance.
     *
     * @return A new config instance
     */
    public BeanContainerConfig create() {
        return new BeanContainerConfigImpl (
            name != null ? name : beanClass.getName(),
            beanClass,
            beanClassLoader,
            resourceInjections.toArray(new ResourceInjectionConfiguration[resourceInjections.size()]),
            postConstructMethods.toArray(new Method[postConstructMethods.size()]),
            preDestroyMethods.toArray(new Method[preDestroyMethods.size()]),
            interceptorConfigurations.toArray(new MethodInterceptorConfiguration[interceptorConfigurations.size()]),
            containerFactory
        );
    }

    /**
     * Process an annotation index entry to determine what injections and interceptors are found on this bean.
     *
     * @param classInfo Annotation index entry for the bean class
     * @param index The annotation index
     * @return The builder
     */
    public BeanContainerConfigBuilder processAnnotations(final ClassInfo classInfo, final Index index) {
        resourceInjections.addAll(ResourceInjectionConfiguration.from(classInfo, beanClass, beanClassLoader));
        interceptorConfigurations.addAll(MethodInterceptorConfiguration.from(classInfo, index, beanClass, beanClassLoader));
        return this;
    }

    /**
     * Set the bean name.
     *
     * @param name The bean name
     * @return The builder
     */
    public BeanContainerConfigBuilder setName(final String name) {
        this.name = name;
        return this;
    }

    /**
     * Add a post-construct method
     *
     * @param postConstructMethod The method
     * @return The builder
     */
    public BeanContainerConfigBuilder addPostConstruct(final Method postConstructMethod) {
        postConstructMethods.add(postConstructMethod);
        return this;
    }

    /**
     * Add a pre-destroy method.
     *
     * @param preDestroyMethod The method
     * @return The builder
     */
    public BeanContainerConfigBuilder addPreDestroy(final Method preDestroyMethod) {
        preDestroyMethods.add(preDestroyMethod);
        return this;
    }

    /**
     * Add a resource injection configuration.
     *
     * @param configuration The injection config
     * @return The builder
     */
    public BeanContainerConfigBuilder addResourceInjection(final ResourceInjectionConfiguration configuration) {
        resourceInjections.add(configuration);
        return this;
    }

    /**
     * Add an interceptor configuration.
     *
     * @param configuration The interceptor config
     * @return The builder
     */
    public BeanContainerConfigBuilder addInterceptor(final MethodInterceptorConfiguration configuration) {
        interceptorConfigurations.add(configuration);
        return this;
    }
}

