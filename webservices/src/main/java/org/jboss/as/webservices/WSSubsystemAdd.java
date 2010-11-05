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
package org.jboss.as.webservices;

import java.net.UnknownHostException;

import javax.management.MBeanServer;

import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.BootUpdateContext;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.as.webservices.config.ServerConfigImpl;
import org.jboss.as.webservices.service.EndpointRegistryService;
import org.jboss.as.webservices.service.ServerConfigService;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.jboss.wsf.common.management.AbstractServerConfig;

/**
 *
 * @author alessio.soldano@jboss.com
 * @since 09-Nov-2010
 *
 */
public final class WSSubsystemAdd extends AbstractSubsystemAdd<WSSubsystemElement> {

    private static final long serialVersionUID = 3326871447114169145L;
    private static final ServiceName mbeanServiceName = ServiceName.JBOSS.append("mbean", "server");

    private WSConfigurationElement configuration;

    protected WSSubsystemAdd() {
        super(Namespace.CURRENT.getUriString());
    }

    public WSConfigurationElement getConfiguration() {
        return configuration;
    }

    public void setConfiguration(WSConfigurationElement configuration) {
        this.configuration = configuration;
    }

    @Override
    protected <P> void applyUpdate(final UpdateContext updateContext, final UpdateResultHandler<? super Void, P> resultHandler,
            final P param) {
        ServiceTarget serviceTarget = updateContext.getServiceTarget();
        addConfigService(serviceTarget, configuration);
        addRegistryService(serviceTarget);
    }

    protected void applyUpdateBootAction(BootUpdateContext updateContext) {
        applyUpdate(updateContext, UpdateResultHandler.NULL, null);
        //add the DUP for dealing with WS deployments
        WSDeploymentActivator.activate(updateContext);
    }

    private static void addConfigService(ServiceTarget serviceTarget, WSConfigurationElement configuration) {
        InjectedValue<MBeanServer> mbeanServer = new InjectedValue<MBeanServer>();
        InjectedValue<ServerEnvironment> serverEnvironment = new InjectedValue<ServerEnvironment>();
        AbstractServerConfig serverConfig = createServerConfig(configuration, mbeanServer, serverEnvironment);
        serviceTarget.addService(WSServices.CONFIG_SERVICE, new ServerConfigService(serverConfig))
                .addDependency(mbeanServiceName, MBeanServer.class, mbeanServer)
                .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, serverEnvironment)
                .setInitialMode(Mode.ACTIVE)
                .install();
    }

    private static void addRegistryService(ServiceTarget serviceTarget) {
        serviceTarget
                .addService(WSServices.REGISTRY_SERVICE, new EndpointRegistryService())
                .setInitialMode(Mode.ACTIVE)
                .install();
    }

    private static AbstractServerConfig createServerConfig(WSConfigurationElement configuration,
            InjectedValue<MBeanServer> mbeanServer, InjectedValue<ServerEnvironment> serverEnvironment) {
        AbstractServerConfig config = new ServerConfigImpl(mbeanServer, serverEnvironment);
        try {
            config.setWebServiceHost(configuration.getWebServiceHost());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        config.setModifySOAPAddress(configuration.isModifySOAPAddress());
        if (configuration.getWebServicePort() != null) {
            config.setWebServicePort(configuration.getWebServicePort());
        }
        if (configuration.getWebServiceSecurePort() != null) {
            config.setWebServicePort(configuration.getWebServiceSecurePort());
        }
        return config;
    }

    protected WSSubsystemElement createSubsystemElement() {
        WSSubsystemElement element = new WSSubsystemElement();
        element.setConfiguration(configuration);
        return element;
    }

}
