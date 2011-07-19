/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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


import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEApplicationDescription;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.ejb3.component.pool.PooledInstanceInterceptor;
import org.jboss.as.ejb3.deployment.EjbJarDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

import java.util.Properties;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class MessageDrivenComponentDescription extends EJBComponentDescription {
    private final Properties activationProps;
    // by default we want to connect with HornetQ
    private String resourceAdapterName = "hornetq-ra";

    /**
     * Construct a new instance.
     *
     * @param componentName      the component name
     * @param componentClassName the component instance class name
     * @param ejbJarDescription  the module description
     */
    public MessageDrivenComponentDescription(final String componentName, final String componentClassName, final EjbJarDescription ejbJarDescription,
                                             final ServiceName deploymentUnitServiceName, final String messageListenerInterfaceName, final Properties activationProps) {
        super(componentName, componentClassName, ejbJarDescription, deploymentUnitServiceName);
        if (messageListenerInterfaceName == null || messageListenerInterfaceName.isEmpty())
            throw new IllegalArgumentException("Cannot set null or empty string as message listener interface");

        this.activationProps = activationProps;

        registerView(messageListenerInterfaceName, MethodIntf.MESSAGE_ENDPOINT);

        getConfigurators().add(new ComponentConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentDescription description, ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                description.addDependency(getResourceAdapterServiceName(), ServiceBuilder.DependencyType.REQUIRED);
            }
        });
    }

    @Override
    public ComponentConfiguration createConfiguration(EEApplicationDescription applicationDescription) {
        final ComponentConfiguration mdbComponentConfiguration = new ComponentConfiguration(this, applicationDescription.getClassConfiguration(getComponentClassName()));
        // setup the component create service
        mdbComponentConfiguration.setComponentCreateServiceFactory(new MessageDrivenComponentCreateServiceFactory());
        return mdbComponentConfiguration;
    }

    public Properties getActivationProps() {
        return activationProps;
    }

    public String getResourceAdapterName() {
        return resourceAdapterName;
    }

    public void setResourceAdapterName(String resourceAdapterName) {
        if (resourceAdapterName == null || resourceAdapterName.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource adapter name cannot be null or empty");
        }
        this.resourceAdapterName = resourceAdapterName;
    }

    @Override
    protected void setupViewInterceptors(ViewDescription view) {
        // let the super do its job
        super.setupViewInterceptors(view);

        // add the instance associating interceptor at the start of the interceptor chain
        view.getConfigurators().add(new ViewConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentConfiguration componentConfiguration, ViewDescription description, ViewConfiguration configuration) throws DeploymentUnitProcessingException {
                configuration.addViewInterceptor(PooledInstanceInterceptor.pooled(), InterceptorOrder.View.ASSOCIATING_INTERCEPTOR);
            }
        });

    }

    @Override
    protected void addCurrentInvocationContextFactory() {
        // add the current invocation context interceptor at the beginning of the component instance post construct chain
        this.getConfigurators().add(new ComponentConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentDescription description, ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                configuration.addPostConstructInterceptor(MessageDrivenInvocationContextInterceptor.FACTORY, InterceptorOrder.ComponentPostConstruct.EJB_SESSION_CONTEXT_INTERCEPTOR);
                configuration.addPreDestroyInterceptor(MessageDrivenInvocationContextInterceptor.FACTORY, InterceptorOrder.ComponentPostConstruct.EJB_SESSION_CONTEXT_INTERCEPTOR);
            }
        });
    }

    @Override
    protected void addCurrentInvocationContextFactory(ViewDescription view) {
        view.getConfigurators().add(new ViewConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentConfiguration componentConfiguration, ViewDescription description, ViewConfiguration configuration) throws DeploymentUnitProcessingException {
                configuration.addViewInterceptor(MessageDrivenInvocationContextInterceptor.FACTORY, InterceptorOrder.View.INVOCATION_CONTEXT_INTERCEPTOR);
            }
        });
    }

    // can't be part of Configuration
    ServiceName getResourceAdapterServiceName() {
        String raDeploymentName = resourceAdapterName;
        // See RaDeploymentParsingProcessor
        if (this.resourceAdapterName.endsWith(".rar")) {
            raDeploymentName = this.resourceAdapterName.substring(0, resourceAdapterName.indexOf(".rar"));
        }
        // See ResourceAdapterDeploymentService
        return ServiceName.of(raDeploymentName);
    }

    @Override
    public boolean isMessageDriven() {
        return true;
    }
}
