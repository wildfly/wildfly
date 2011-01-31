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

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentFactory;
import org.jboss.as.ee.component.injection.ResourceInjection;
import org.jboss.as.ee.component.injection.ResourceInjectionResolver;
import org.jboss.as.ee.component.liefcycle.ComponentLifecycle;
import org.jboss.as.ee.naming.ContextNames;
import org.jboss.as.ee.naming.NamingContextConfig;
import org.jboss.as.naming.deployment.JndiName;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.msc.service.ServiceName;

/**
 * Manged-bean specific implementation of a {@link org.jboss.as.ee.component.ComponentFactory}.
 *
 * @author John Bailey
 */
public class ManagedBeanComponentFactory implements ComponentFactory {
    private final ResourceInjectionResolver injectionResolver = new ManagedBeanResourceInjectionResolver();

    public ConstructedComponent createComponent(final DeploymentUnit deploymentUnit, final String componentName, final Class<?> componentClass, final ClassLoader classLoader, final List<ResourceInjection> injections, final List<ComponentLifecycle> postConstructLifecycles, final List<ComponentLifecycle> preDestroyLifecycles, final Map<Method, InterceptorFactory> methodInterceptorFactories) {
        final ManagedBeanComponent<?> container = createContainer(componentClass, classLoader, injections, postConstructLifecycles, preDestroyLifecycles, methodInterceptorFactories);
        final ServiceName containerServiceName = ServiceNames.MANAGED_BEAN.append(deploymentUnit.getName(), componentName);
        final NamingContextConfig moduleContext = deploymentUnit.getAttachment(org.jboss.as.ee.naming.Attachments.MODULE_CONTEXT_CONFIG);
        final NamingContextConfig appContext = deploymentUnit.getAttachment(org.jboss.as.ee.naming.Attachments.APPLICATION_CONTEXT_CONFIG);
        final JndiName bindName = ContextNames.MODULE_CONTEXT_NAME.append(componentName);

        final ServiceName envContextServiceName = moduleContext.getContextServiceName().append("env");

        return new ConstructedComponent() {
            public Component<?> getComponent() {
                return container;
            }

            public ServiceName getComponentServiceName() {
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

            public ServiceName getCompContextServiceName() {
                return moduleContext.getContextServiceName();
            }

            public ServiceName getModuleContextServiceName() {
                return moduleContext.getContextServiceName();
            }

            public ServiceName getAppContextServiceName() {
                return appContext.getContextServiceName();
            }
        };
    }

    private <T> ManagedBeanComponent<T> createContainer(final Class<T> beanClass, final ClassLoader classLoader, final List<ResourceInjection> injections, final List<ComponentLifecycle> postConstructLifecycles, final List<ComponentLifecycle> preDestroyLifecycles, final Map<Method, InterceptorFactory> methodInterceptorFactories) {
        return new ManagedBeanComponent<T>(beanClass, classLoader, injections, postConstructLifecycles, preDestroyLifecycles, methodInterceptorFactories);
    }

    public ResourceInjectionResolver getResourceInjectionResolver() {
        return injectionResolver;
    }
}
