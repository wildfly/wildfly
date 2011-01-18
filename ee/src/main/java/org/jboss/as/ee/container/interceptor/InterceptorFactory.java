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

package org.jboss.as.ee.container.interceptor;

import java.util.List;
import org.jboss.as.ee.container.BeanContainerConfiguration;
import org.jboss.as.ee.container.injection.ResourceInjection;
import org.jboss.as.server.deployment.DeploymentUnit;

/**
 * Factory used to create {@link MethodInterceptor} instances from {@link MethodInterceptorConfiguration} instances.
 *
 * @author John Bailey
 */
public interface InterceptorFactory {
    /**
     * Create a {@link MethodInterceptor} instance.
     *
     * @param deploymentUnit The current deployment unit
     * @param classLoader The classloader used for creation
     * @param configuration  The interceptor configuration
     * @param injections     The interceptors injections
     * @return The MethodInterceptor instance
     */
    MethodInterceptor createMethodInterceptor(final DeploymentUnit deploymentUnit, final ClassLoader classLoader, final MethodInterceptorConfiguration configuration, final List<ResourceInjection> injections) throws Exception;

    /**
     * Create a {@link MethodInterceptor} instance.
     *
     * @param deploymentUnit The current deployment unit
     * @param classLoader The classloader used for creation
     * @param configuration  The interceptor configuration
     * @return The Interceptor instance
     */
    LifecycleInterceptor createLifecycleInterceptor(final DeploymentUnit deploymentUnit, final ClassLoader classLoader, final BeanContainerConfiguration beanContainerConfig, final LifecycleInterceptorConfiguration configuration) throws Exception;
}
