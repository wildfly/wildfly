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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.network.ClientMapping;
import org.jboss.as.remoting.RemotingConnectorBindingInfoService;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.ejb.BeanManagerFactoryBuilderConfiguration;
import org.wildfly.clustering.registry.RegistryEntryProvider;
import org.wildfly.clustering.spi.CacheServiceNames;

/**
 * @author Jaikiran Pai
 */
public class EJBRemotingConnectorClientMappingsEntryProviderService extends AbstractService<RegistryEntryProvider<String, List<ClientMapping>>> {

    private final RegistryEntryProvider<String, List<ClientMapping>> registryEntryProvider = new ClientMappingEntryProvider();
    private final InjectedValue<ServerEnvironment> serverEnvironment = new InjectedValue<>();
    private final InjectedValue<RemotingConnectorBindingInfoService.RemotingConnectorInfo> remotingConnectorInfo = new InjectedValue<>();

    public ServiceBuilder<RegistryEntryProvider<String, List<ClientMapping>>> build(ServiceTarget target, ServiceName remotingServerInfoServiceName) {
        return target.addService(CacheServiceNames.REGISTRY_ENTRY.getServiceName(BeanManagerFactoryBuilderConfiguration.DEFAULT_CONTAINER_NAME), this)
                .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, this.serverEnvironment)
                .addDependency(remotingServerInfoServiceName, RemotingConnectorBindingInfoService.RemotingConnectorInfo.class, this.remotingConnectorInfo)
        ;
    }

    @Override
    public RegistryEntryProvider<String, List<ClientMapping>> getValue() {
        return this.registryEntryProvider;
    }

    List<ClientMapping> getClientMappings() {
        final List<ClientMapping> ret = new ArrayList<>();
        RemotingConnectorBindingInfoService.RemotingConnectorInfo info = this.remotingConnectorInfo.getValue();
        if (info.getSocketBinding().getClientMappings() != null && !info.getSocketBinding().getClientMappings().isEmpty()) {
            ret.addAll(info.getSocketBinding().getClientMappings());
        } else {
            // TODO: We use the textual form of IP address as the destination address for now.
            // This needs to be configurable (i.e. send either host name or the IP address). But
            // since this is a corner case (i.e. absence of any client-mappings for a socket binding),
            // this should be OK for now
            final String destinationAddress = info.getSocketBinding().getAddress().getHostAddress();
            final InetAddress clientNetworkAddress;
            try {
                clientNetworkAddress = InetAddress.getByName("::");
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
            final ClientMapping defaultClientMapping = new ClientMapping(clientNetworkAddress, 0, destinationAddress, info.getSocketBinding().getAbsolutePort());
            ret.add(defaultClientMapping);
        }
        return ret;
    }

    String getNodeName() {
        return this.serverEnvironment.getValue().getNodeName();
    }

    class ClientMappingEntryProvider implements RegistryEntryProvider<String, List<ClientMapping>> {

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
