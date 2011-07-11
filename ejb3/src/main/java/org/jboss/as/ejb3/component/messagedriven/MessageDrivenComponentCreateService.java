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

package org.jboss.as.ejb3.component.messagedriven;

import org.jboss.as.ee.component.BasicComponent;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ejb3.component.EJBComponentCreateService;
import org.jboss.as.ejb3.deployment.EjbJarConfiguration;
import org.jboss.as.ejb3.inflow.EndpointDeployer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.ResourceAdapter;
import java.util.Properties;

/**
 * @author Stuart Douglas
 */
public class MessageDrivenComponentCreateService extends EJBComponentCreateService {

    private final ServiceName raServiceName;
    private final Class<?> messageListenerInterface;
    private final String resourceAdapterName;
    private final Properties activationProps;

    /**
     * Construct a new instance.
     *
     * @param componentConfiguration the component configuration
     */
    public MessageDrivenComponentCreateService(final ComponentConfiguration componentConfiguration, final EjbJarConfiguration ejbJarConfiguration) {
        super(componentConfiguration, ejbJarConfiguration);

        final MessageDrivenComponentDescription componentDescription = (MessageDrivenComponentDescription) componentConfiguration.getComponentDescription();
        this.resourceAdapterName = componentDescription.getResourceAdapterName();
        this.raServiceName = componentDescription.getResourceAdapterServiceName();

        // see MessageDrivenComponentDescription.<init>
        this.messageListenerInterface = componentConfiguration.getViews().get(0).getViewClass();

        this.activationProps = componentDescription.getActivationProps();
    }

    @Override
    protected BasicComponent createComponent() {
        final ActivationSpec activationSpec = getEndpointDeployer().createActivationSpecs(resourceAdapterName, messageListenerInterface, activationProps, getDeploymentClassLoader());
        //final ActivationSpec activationSpec = null;
        final MessageDrivenComponent component = new MessageDrivenComponent(this, messageListenerInterface, activationSpec);
        // TODO: should be injected by start service
        final ResourceAdapter resourceAdapter = getRequiredService(raServiceName, ResourceAdapter.class).getValue();
        component.setResourceAdapter(resourceAdapter);
        try {
            activationSpec.setResourceAdapter(resourceAdapter);
        } catch (ResourceException e) {
            throw new RuntimeException(e);
        }
        return component;
    }

    private ClassLoader getDeploymentClassLoader() {
        return getComponentClass().getClassLoader();
    }

    private EndpointDeployer getEndpointDeployer() {
        return getEJBUtilities();
    }

    private <S> ServiceController<S> getRequiredService(final ServiceName serviceName, final Class<S> expectedType) {
        return (ServiceController<S>) getServiceRegistry().getRequiredService(serviceName);
    }

    private ServiceRegistry getServiceRegistry() {
        return getDeploymentUnitInjector().getValue().getServiceRegistry();
    }
}
