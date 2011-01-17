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

import java.util.List;
import org.jboss.as.ee.container.injection.ResourceInjection;
import org.jboss.as.ee.container.injection.ResourceInjectionResolver;
import org.jboss.as.ee.container.interceptor.MethodInterceptor;
import org.jboss.as.ee.container.interceptor.MethodInterceptorFactory;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.service.ServiceName;

/**
 * Factory responsible for crating {@link BeanContainer} instances.
 *
 * @author John Bailey
 */
public interface BeanContainerFactory {
    /**
     * Create the bean container instance.
     *
     * @param deploymentUnit  The current deployment unit
     * @param containerConfig The container configuration
     * @param injections      The containers resource injection instances
     * @param interceptors    The containers interceptor instances
     * @return A bean container service information
     */
    ConstructedBeanContainer createBeanContainer(final DeploymentUnit deploymentUnit, final BeanContainerConfig containerConfig, final List<ResourceInjection> injections, final List<MethodInterceptor> interceptors);

    /**
     * Return the resource injection resolver for this bean container type.
     *
     * @return The resolver
     */
    ResourceInjectionResolver getResourceInjectionResolver();

    /**
     * Return the method interceptor factory for this bean container type.
     *
     * @return The interceptor factory
     */
    MethodInterceptorFactory getMethodInterceptorFactory();

    /**
     * Interface used to capture the results of creating a bean container.  Provides the bean container instance as well
     * as the information required to setup the services required to run the container.
     */
    interface ConstructedBeanContainer {
        /**
         * Return the bean container instance.
         *
         * @return The container instance
         */
        BeanContainer<?> getBeanContainer();

        /**
         * Return the container service name.
         *
         * @return The service name
         */
        ServiceName getContainerServiceName();

        /**
         * The service name of the naming context this container's reference will be bound.
         *
         * @return The context service name
         */
        ServiceName getBindContextServiceName();

        /**
         * The name used when binding the container reference in the bind context.
         *
         * @return The bind name
         */
        String getBindName();

        /**
         * The service name for the naming context this container's environment entries will be bound.
         *
         * @return The environment context service name
         */
        ServiceName getEnvContextServiceName();
    }
}
