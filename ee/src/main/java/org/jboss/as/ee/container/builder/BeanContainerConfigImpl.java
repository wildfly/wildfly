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
import org.jboss.as.ee.container.BeanContainerConfig;
import org.jboss.as.ee.container.BeanContainerFactory;
import org.jboss.as.ee.container.injection.ResourceInjectionConfiguration;
import org.jboss.as.ee.container.interceptor.MethodInterceptorConfiguration;

/**
 * @author John Bailey
 */
class BeanContainerConfigImpl implements BeanContainerConfig {
    private final String name;
    private final Class<?> beanClass;
    private final ClassLoader classLoader;
    private final ResourceInjectionConfiguration[] resourceInjections;
    private final Method[] postConstructMethods;
    private final Method[] preDestroyMethods;
    private final MethodInterceptorConfiguration[] interceptorConfigurations;
    private final BeanContainerFactory beanContainerFactory;

    BeanContainerConfigImpl(final String name, final Class<?> beanClass, final ClassLoader classLoader, final ResourceInjectionConfiguration[] resourceInjections, final Method[] postConstructMethods, final Method[] preDestroyMethods, final MethodInterceptorConfiguration[] interceptorConfigurations, final BeanContainerFactory beanContainerFactory) {
        this.name = name;
        this.beanClass = beanClass;
        this.classLoader = classLoader;
        this.resourceInjections = resourceInjections;
        this.postConstructMethods = postConstructMethods;
        this.preDestroyMethods = preDestroyMethods;
        this.interceptorConfigurations = interceptorConfigurations;
        this.beanContainerFactory = beanContainerFactory;
    }

    public String getName() {
        return name;
    }

    /**
     * The bean's class.
     *
     * @return The bean class
     */
    public Class<?> getBeanClass() {
        return beanClass;
    }

    /**
     * The classloader for the bean.
     *
     * @return The classloader
     */
    public ClassLoader getBeanClassLoader() {
        return classLoader;
    }

    public ResourceInjectionConfiguration[] getResourceInjectionConfigs() {
        return resourceInjections;
    }

    public Method[] getPostConstructMethods() {
        return postConstructMethods;
    }

    public Method[] getPreDestroyMethods() {
        return preDestroyMethods;
    }

    public MethodInterceptorConfiguration[] getMethodInterceptorConfigs() {
        return interceptorConfigurations;
    }

    public BeanContainerFactory getContainerFactory() {
        return beanContainerFactory;
    }
}
