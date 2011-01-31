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

package org.jboss.as.ee.component;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.jboss.as.ee.component.injection.ResourceInjection;
import org.jboss.as.ee.component.injection.ResourceInjectionResolver;
import org.jboss.as.ee.component.liefcycle.ComponentLifecycle;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.msc.service.ServiceName;

/**
 * Factory responsible for crating {@link Component} instances.
 *
 * @author John Bailey
 */
public interface ComponentFactory {
    /**
     * Create the component.
     *
     * @param deploymentUnit             The current deployment unit
     * @param componentName              The component name
     * @param componentClass             The component class
     * @param classLoader                The classloader
     * @param injections                 The component's resource injection instances
     * @param postConstructLifecycles    The post-constructor lifecycles
     * @param preDestroyLifecycles       the pre-destroy lifecycles
     * @param methodInterceptorFactories the method interceptor factories
     * @return Component service information
     */
    ConstructedComponent createComponent(final DeploymentUnit deploymentUnit, final String componentName, final Class<?> componentClass, final ClassLoader classLoader, final List<ResourceInjection> injections, final List<ComponentLifecycle> postConstructLifecycles, final List<ComponentLifecycle> preDestroyLifecycles, final Map<Method, InterceptorFactory> methodInterceptorFactories);

    /**
     * Return the resource injection resolver for this component type.
     *
     * @return The resolver
     */
    ResourceInjectionResolver getResourceInjectionResolver();

    /**
     * Interface used to capture the results of creating a component.  Provides the component instance as well
     * as the information required to setup the services required to run the component.
     */
    interface ConstructedComponent {
        /**
         * Return the component instance.
         *
         * @return The component instance
         */
        Component<?> getComponent();

        /**
         * Return the component service name.
         *
         * @return The service name
         */
        ServiceName getComponentServiceName();

        /**
         * The service name of the naming context this component's reference will be bound.
         *
         * @return The context service name
         */
        ServiceName getBindContextServiceName();

        /**
         * The name used when binding the component reference in the bind context.
         *
         * @return The bind name
         */
        String getBindName();

        /**
         * The service name for the naming context this component's environment entries will be bound.
         *
         * @return The environment context service name
         */
        ServiceName getEnvContextServiceName();

        /**
         * The service name for the naming context for this component.
         *
         * @return The component naming context
         */
        ServiceName getCompContextServiceName();

        /**
         * The service name for the module context for this component.
         *
         * @return The module context name
         */
        ServiceName getModuleContextServiceName();

        /**
         * The service name for the app context for this component.
         *
         * @return The app context name
         */
         ServiceName getAppContextServiceName();
    }
}
