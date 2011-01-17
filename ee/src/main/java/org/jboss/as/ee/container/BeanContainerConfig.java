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

import java.lang.reflect.Method;
import org.jboss.as.ee.container.interceptor.MethodInterceptorConfiguration;
import org.jboss.as.ee.container.injection.ResourceInjectionConfiguration;

/**
 * The configuration for a {@link BeanContainer} use for constructing and installing a bean container instance.
 *
 * @author John Bailey
 */
public interface BeanContainerConfig {

    /**
     * The bean container name.  This will often reflect the name of the EE component.
     *
     * @return The bean container name
     */
    String getName();

    /**
     * The bean's class.
     *
     * @return The bean class
     */
    Class<?> getBeanClass();

    /**
     * The classloader for the bean.
     *
     * @return The classloader
     */
    ClassLoader getBeanClassLoader();

    /**
     * The post-construct life-cycle methods.
     *
     * @return The post-construct life-cycle methods
     */
    Method[] getPostConstructMethods();

    /**
     * The pre-destroy life-cycle methods.
     *
     * @return The pre-destroy life-cycle methods
     */
    Method[] getPreDestroyMethods();

    /**
     * The configurations for any resource injections for this bean type.
     *
     * @return The resource injection configurations
     */
    ResourceInjectionConfiguration[] getResourceInjectionConfigs();

    /**
     * The configurations for any method interceptors for this bean type.
     *
     * @return The method interceptor configurations
     */
    MethodInterceptorConfiguration[] getMethodInterceptorConfigs();

    /**
     * The bean container factory for this bean type.
     *
     * @return The bean container factory
     */
    BeanContainerFactory getContainerFactory();
}
