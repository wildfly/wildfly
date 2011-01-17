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

package org.jboss.as.managedbean.container;

import java.util.List;
import org.jboss.as.ee.container.BeanContainer;
import org.jboss.as.ee.container.BeanContainerConfig;
import org.jboss.as.ee.container.BeanContainerFactory;
import org.jboss.as.ee.container.injection.ResourceInjection;
import org.jboss.as.ee.container.injection.ResourceInjectionResolver;
import org.jboss.as.ee.container.interceptor.DefaultMethodInterceptorFactory;
import org.jboss.as.ee.container.interceptor.MethodInterceptor;
import org.jboss.as.ee.container.interceptor.MethodInterceptorFactory;
import org.jboss.as.ee.naming.ContextNames;
import org.jboss.as.ee.naming.NamingContextConfig;
import org.jboss.as.naming.deployment.JndiName;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.service.ServiceName;

/**
 * Manged-bean specific implementation of a {@link BeanContainerFactory}.
 *
 * @author John Bailey
 */
public class ManagedBeanContainerFactory implements BeanContainerFactory {
    private final ResourceInjectionResolver injectionResolver = new ManagedBeanResourceInjectionResolver();
    private final MethodInterceptorFactory interceptorFactory = new DefaultMethodInterceptorFactory();

    public ConstructedBeanContainer createBeanContainer(final DeploymentUnit deploymentUnit, final BeanContainerConfig containerConfig, final List<ResourceInjection> injections, final List<MethodInterceptor> interceptors) {
        final ManagedBeanContainer<?> container = new ManagedBeanContainer<Object>(containerConfig, injections, interceptors);
        final ServiceName containerServiceName = ServiceNames.MANAGED_BEAN.append(deploymentUnit.getName(), containerConfig.getName());
        final NamingContextConfig moduleContext = deploymentUnit.getAttachment(org.jboss.as.ee.naming.Attachments.MODULE_CONTEXT_CONFIG);
        final JndiName bindName = ContextNames.MODULE_CONTEXT_NAME.append(containerConfig.getName());

        final ServiceName envContextServiceName = moduleContext.getContextServiceName().append("env");

        return new ConstructedBeanContainer() {
            public BeanContainer<?> getBeanContainer() {
                return container;
            }

            public ServiceName getContainerServiceName() {
                return containerServiceName;
            }

            public ServiceName getBindContextServiceName() {
                return moduleContext.getContextServiceName();
            }

            public String getBindName() {
                return bindName.getLocalName();
            }

            public ServiceName getEnvContextServiceName() {
                return envContextServiceName;
            }
        };
    }

    public ResourceInjectionResolver getResourceInjectionResolver() {
        return injectionResolver;
    }

    public MethodInterceptorFactory getMethodInterceptorFactory() {
        return interceptorFactory;
    }
}
