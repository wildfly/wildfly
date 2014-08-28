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

package org.jboss.as.ee.component;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.Map;

import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.naming.context.NamespaceContextSelector;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.Interceptors;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * A service for creating a component.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class BasicComponentCreateService implements Service<Component> {
    private final InjectedValue<DeploymentUnit> deploymentUnit = new InjectedValue<DeploymentUnit>();

    private final ServiceName serviceName;
    private final String componentName;
    private final Class<?> componentClass;
    private final InterceptorFactory postConstruct;
    private final InterceptorFactory preDestroy;
    private final Map<Method, InterceptorFactory> componentInterceptors;
    private final NamespaceContextSelector namespaceContextSelector;

    private BasicComponent component;


    /**
     * Construct a new instance.
     *
     * @param componentConfiguration the component configuration
     */
    public BasicComponentCreateService(final ComponentConfiguration componentConfiguration) {
        serviceName = componentConfiguration.getComponentDescription().getCreateServiceName();
        componentName = componentConfiguration.getComponentName();
        postConstruct = Interceptors.getChainedInterceptorFactory(componentConfiguration.getPostConstructInterceptors());
        preDestroy = Interceptors.getChainedInterceptorFactory(componentConfiguration.getPreDestroyInterceptors());
        final IdentityHashMap<Method, InterceptorFactory> componentInterceptors = new IdentityHashMap<Method, InterceptorFactory>();
        for (Method method : componentConfiguration.getDefinedComponentMethods()) {
            if(requiresInterceptors(method, componentConfiguration)) {
                componentInterceptors.put(method, Interceptors.getChainedInterceptorFactory(componentConfiguration.getComponentInterceptors(method)));
            }
        }
        componentClass = componentConfiguration.getComponentClass();
        this.componentInterceptors = componentInterceptors;
        this.namespaceContextSelector = componentConfiguration.getNamespaceContextSelector();
    }

    protected boolean requiresInterceptors(final Method method, final ComponentConfiguration componentConfiguration) {
        return Modifier.isPublic(method.getModifiers()) && !Modifier.isFinal(method.getModifiers()) && componentConfiguration.getComponentDescription().isIntercepted();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void start(final StartContext context) throws StartException {
        component = createComponent();
    }

    /**
     * Create the component.
     *
     * @return the component instance
     */
    protected BasicComponent createComponent() {
        return new BasicComponent(this);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void stop(final StopContext context) {
        component = null;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Component getValue() throws IllegalStateException, IllegalArgumentException {
        Component component = this.component;
        if (component == null) {
            throw EeLogger.ROOT_LOGGER.serviceNotStarted();
        }
        return component;
    }

    /**
     * Get the deployment unit injector.
     *
     * @return the deployment unit injector
     */
    public InjectedValue<DeploymentUnit> getDeploymentUnitInjector() {
        return deploymentUnit;
    }

    /**
     * Get the component name.
     *
     * @return the component name
     */
    public String getComponentName() {
        return componentName;
    }

    /**
     * Get the post-construct interceptor factory.
     *
     * @return the post-construct interceptor factory
     */
    public InterceptorFactory getPostConstruct() {
        return postConstruct;
    }

    /**
     * Get the pre-destroy interceptor factory.
     *
     * @return the pre-destroy interceptor factory
     */
    public InterceptorFactory getPreDestroy() {
        return preDestroy;
    }

    /**
     * Get the component interceptor factory map.
     *
     * @return the component interceptor factories
     */
    public Map<Method, InterceptorFactory> getComponentInterceptors() {
        return componentInterceptors;
    }

    /**
     * Get the component class.
     *
     * @return the component class
     */
    public Class<?> getComponentClass() {
        return componentClass;
    }

    /**
     *
     * @return the namespace context selector for the component, or null if it does not have one
     */
    public NamespaceContextSelector getNamespaceContextSelector() {
        return namespaceContextSelector;
    }

    public ServiceName getServiceName() {
        return this.serviceName;
    }
}
