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

package org.jboss.as.ejb3.remote;

import org.jboss.as.clustering.registry.Registry;
import org.jboss.as.network.ClientMapping;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.remoting.AbstractStreamServerService;
import org.jboss.as.remoting.InjectedSocketBindingStreamServerService;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import java.util.Collections;
import java.util.List;

/**
 * @author Jaikiran Pai
 */
public class EJBRemotingConnectorClientMappingsEntryProviderService implements Service<EJBRemotingConnectorClientMappingsEntryProviderService> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb").append("remoting").append("connector").append("client-mapping-entry-provider-service");

    private final ServiceName remotingConnectorServiceName;
    private final InjectedValue<ServerEnvironment> environment = new InjectedValue<ServerEnvironment>();
    private volatile InjectedSocketBindingStreamServerService remotingServer;
    private final Registry.RegistryEntryProvider<String, List<ClientMapping>> registryEntryProvider = new ClientMappingEntryProvider();

    public EJBRemotingConnectorClientMappingsEntryProviderService(final ServiceName remotingConnectorServiceName) {
        this.remotingConnectorServiceName = remotingConnectorServiceName;
    }

    @Override
    public void start(StartContext context) throws StartException {
        // get the remoting server (which allows remoting connector to connect to it) service
        final ServiceContainer serviceContainer = context.getController().getServiceContainer();
        final ServiceController streamServerServiceController = serviceContainer.getRequiredService(this.remotingConnectorServiceName);
        final AbstractStreamServerService streamServerService = (AbstractStreamServerService) streamServerServiceController.getService();
        // we can only work off a remoting connector which is backed by a socketbinding
        if (streamServerService instanceof InjectedSocketBindingStreamServerService) {
            this.remotingServer = (InjectedSocketBindingStreamServerService) streamServerService;
        }
    }

    @Override
    public void stop(StopContext context) {
        this.remotingServer = null;
    }

    @Override
    public EJBRemotingConnectorClientMappingsEntryProviderService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public Registry.RegistryEntryProvider<String, List<ClientMapping>> getRegistryEntryProvider() {
        return this.registryEntryProvider;
    }

    public Injector<ServerEnvironment> getServerEnvironmentInjector() {
        return this.environment;
    }

    List<ClientMapping> getClientMappings() {
        if (this.remotingServer == null) {
            return Collections.emptyList();
        }
        final SocketBinding socketBinding = this.remotingServer.getSocketBinding();
        return socketBinding.getClientMappings();
    }

    String getNodeName() {
        return this.environment.getValue().getNodeName();
    }

    private class ClientMappingEntryProvider implements Registry.RegistryEntryProvider<String, List<ClientMapping>> {

        @Override
        public String getKey() {
            return EJBRemotingConnectorClientMappingsEntryProviderService.this.getNodeName();
        }

        @Override
        public List<ClientMapping> getValue() {
            return EJBRemotingConnectorClientMappingsEntryProviderService.this.getClientMappings();
        }
    }
}
