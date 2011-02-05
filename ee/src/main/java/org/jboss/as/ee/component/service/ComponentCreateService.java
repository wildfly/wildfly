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

package org.jboss.as.ee.component.service;

import org.jboss.as.ee.component.AbstractComponent;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentFactory;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import javax.naming.Context;

/**
 * A service for creating a component.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ComponentCreateService implements Service<Component> {
    private final InjectedValue<DeploymentUnit> deploymentUnit = new InjectedValue<DeploymentUnit>();
    private final InjectedValue<Context> compContext = new InjectedValue<Context>();
    private final InjectedValue<Context> moduleContext = new InjectedValue<Context>();
    private final InjectedValue<Context> appContext = new InjectedValue<Context>();

    private final ComponentFactory componentFactory;
    private final ComponentConfiguration componentConfiguration;
    private Component component;

    /**
     * Construct a new instance.
     *
     * @param componentFactory the component factory
     * @param componentConfiguration the component configuration
     */
    public ComponentCreateService(final ComponentFactory componentFactory, final ComponentConfiguration componentConfiguration) {
        this.componentFactory = componentFactory;
        this.componentConfiguration = componentConfiguration;
    }

    /** {@inheritDoc} */
    public synchronized void start(final StartContext context) throws StartException {
        final Component component;
        try {
            this.component = component = componentFactory.createComponent(deploymentUnit.getValue(), componentConfiguration);
        } catch (DeploymentUnitProcessingException e) {
            throw new StartException(e);
        }
        // TODO: make these contexts part of config...?
        if (component instanceof AbstractComponent) {
            final AbstractComponent abstractComponent = AbstractComponent.class.cast(this.component);
            abstractComponent.setComponentContext(compContext.getValue());
            abstractComponent.setModuleContext(moduleContext.getValue());
            abstractComponent.setApplicationContext(appContext.getValue());
        }
    }

    /** {@inheritDoc} */
    public synchronized void stop(final StopContext context) {
        final Component component = this.component;
        this.component = null;
        if(component instanceof AbstractComponent) {
            final AbstractComponent abstractComponent = AbstractComponent.class.cast(component);
            abstractComponent.setComponentContext(null);
            abstractComponent.setModuleContext(null);
            abstractComponent.setApplicationContext(null);
        }
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

    /**
     * Get the component context injector.
     *
     * @return the injector
     */
    public Injector<Context> getCompContextInjector() {
        return compContext;
    }

    /**
     * Get the module context injector.
     *
     * @return the injector
     */
    public Injector<Context> getModuleContextInjector() {
        return moduleContext;
    }

    /**
     * Get the application context injector.
     *
     * @return the injector
     */
    public Injector<Context> getAppContextInjector() {
        return appContext;
    }
}
