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

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.Interceptors;
import org.jboss.invocation.SimpleInterceptorFactoryContext;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import java.util.ArrayList;

/**
 * A service for creating a component.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ComponentCreateService implements Service<Component> {
    private final InjectedValue<DeploymentUnit> deploymentUnit = new InjectedValue<DeploymentUnit>();

    private final AbstractComponentConfiguration componentConfiguration;
    private AbstractComponent component;

    /**
     * Construct a new instance.
     *
     * @param componentConfiguration the component configuration
     */
    public ComponentCreateService(final AbstractComponentConfiguration componentConfiguration) {
        this.componentConfiguration = componentConfiguration;
    }

    /** {@inheritDoc} */
    public synchronized void start(final StartContext context) throws StartException {
        component = componentConfiguration.constructComponent();

        // do 'injection' on the component

        // First, system interceptors (one of which should associate)
        final ArrayList<Interceptor> rootInterceptors = new ArrayList<Interceptor>();
        final SimpleInterceptorFactoryContext interceptorFactoryContext = new SimpleInterceptorFactoryContext();
        // TODO: a contract for ComponentInterceptorFactory
        interceptorFactoryContext.getContextData().put(Component.class, component);
        for (InterceptorFactory factory : componentConfiguration.getComponentSystemInterceptorFactories()) {
            rootInterceptors.add(factory.create(interceptorFactoryContext));
        }

        rootInterceptors.add(DispatcherInterceptor.INSTANCE);
        component.setComponentInterceptor(Interceptors.getChainedInterceptor(rootInterceptors));
    }

    /** {@inheritDoc} */
    public synchronized void stop(final StopContext context) {
        component = null;
    }

    /** {@inheritDoc} */
    public synchronized Component getValue() throws IllegalStateException, IllegalArgumentException {
        Component component = this.component;
        if (component == null) {
            throw new IllegalStateException("Service not started");
        }
        return component;
    }

    /**
     * Get the deployment unit injector.
     *
     * @return the deployment unit injector
     */
    public Injector<DeploymentUnit> getDeploymentUnitInjector() {
        return deploymentUnit;
    }

    public AbstractComponentConfiguration getComponentConfiguration() {
        return componentConfiguration;
    }
}
