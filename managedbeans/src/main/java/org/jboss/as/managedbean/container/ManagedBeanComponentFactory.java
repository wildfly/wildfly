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

package org.jboss.as.managedbean.container;

import java.util.List;
import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentFactory;
import org.jboss.as.ee.component.injection.ResourceInjection;
import org.jboss.as.ee.component.interceptor.ComponentInterceptorFactories;
import org.jboss.as.ee.component.lifecycle.ComponentLifecycle;
import org.jboss.as.ee.naming.ContextNames;
import org.jboss.as.ee.naming.NamingContextConfig;
import org.jboss.as.naming.deployment.JndiName;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceName;

/**
 * Manged-bean specific implementation of a {@link org.jboss.as.ee.component.ComponentFactory}.
 *
 * @author John Bailey
 */
public class ManagedBeanComponentFactory implements ComponentFactory {
    public static final ManagedBeanComponentFactory INSTANCE = new ManagedBeanComponentFactory();

    private ManagedBeanComponentFactory() {
    }

    public ConstructedComponent createComponent(final DeploymentUnit deploymentUnit, final ComponentConfiguration componentConfiguration) {
        final Class<?> componentClass = componentConfiguration.getAttachment(Attachments.COMPONENT_CLASS);
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        final ClassLoader classLoader = module.getClassLoader();

        final ManagedBeanComponent<?> container = createComponent(componentClass, classLoader,
                componentConfiguration.getAttachment(Attachments.RESOURCE_INJECTIONS),
                componentConfiguration.getAttachment(Attachments.POST_CONSTRUCTS),
                componentConfiguration.getAttachment(Attachments.PRE_DESTROYS),
                componentConfiguration.getAttachment(Attachments.COMPONENT_INTERCEPTOR_FACTORIES));

        final ServiceName containerServiceName = ServiceNames.MANAGED_BEAN.append(deploymentUnit.getName(), componentConfiguration.getName());
        final NamingContextConfig moduleContext = deploymentUnit.getAttachment(org.jboss.as.ee.naming.Attachments.MODULE_CONTEXT_CONFIG);
        final NamingContextConfig appContext = deploymentUnit.getAttachment(org.jboss.as.ee.naming.Attachments.APPLICATION_CONTEXT_CONFIG);
        final JndiName bindName = ContextNames.MODULE_CONTEXT_NAME.append(componentConfiguration.getName());

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

    private <T> ManagedBeanComponent<T> createComponent(final Class<T> beanClass, final ClassLoader classLoader, final List<ResourceInjection> injections, final List<ComponentLifecycle> postConstructLifecycles, final List<ComponentLifecycle> preDestroyLifecycles, final ComponentInterceptorFactories methodInterceptorFactories) {
        return new ManagedBeanComponent<T>(beanClass, classLoader, injections, postConstructLifecycles, preDestroyLifecycles, methodInterceptorFactories);
    }
}
