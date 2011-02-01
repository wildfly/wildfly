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

package org.jboss.as.managedbean.component;

import java.util.List;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentFactory;
import org.jboss.as.ee.component.injection.ResourceInjection;
import org.jboss.as.ee.component.interceptor.ComponentInterceptorFactories;
import org.jboss.as.ee.component.lifecycle.ComponentLifecycle;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.modules.Module;

/**
 * Manged-bean specific implementation of a {@link org.jboss.as.ee.component.ComponentFactory}.
 *
 * @author John Bailey
 */
public class ManagedBeanComponentFactory implements ComponentFactory {
    public static final ManagedBeanComponentFactory INSTANCE = new ManagedBeanComponentFactory();

    private ManagedBeanComponentFactory() {
    }

    public Component createComponent(final DeploymentUnit deploymentUnit, final ComponentConfiguration componentConfiguration) {
        final Class<?> componentClass = componentConfiguration.getComponentClass();
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        final ClassLoader classLoader = module.getClassLoader();

        return createComponent(componentClass, classLoader,
                componentConfiguration.getResourceInjections(),
                componentConfiguration.getPostConstructLifecycles(),
                componentConfiguration.getPreDestroyLifecycles(),
                componentConfiguration.getComponentInterceptorFactories());
    }

    private <T> ManagedBeanComponent createComponent(final Class<T> beanClass, final ClassLoader classLoader, final List<ResourceInjection> injections, final List<ComponentLifecycle> postConstructLifecycles, final List<ComponentLifecycle> preDestroyLifecycles, final ComponentInterceptorFactories methodInterceptorFactories) {
        return new ManagedBeanComponent(beanClass, classLoader, injections, postConstructLifecycles, preDestroyLifecycles, methodInterceptorFactories);
    }
}
