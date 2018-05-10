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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.network.ClientMapping;
import org.jboss.as.remoting.RemotingConnectorBindingInfoService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.ejb.BeanManagerFactoryServiceConfiguratorConfiguration;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;
import org.wildfly.clustering.spi.ClusteringCacheRequirement;
import org.wildfly.clustering.spi.ClusteringRequirement;

/**
 * @author Jaikiran Pai
 */
public class EJBRemotingConnectorClientMappingsEntryProviderService implements CapabilityServiceBuilder<Map.Entry<String, List<ClientMapping>>>, Value<Map.Entry<String, List<ClientMapping>>> {

    private final InjectedValue<RemotingConnectorBindingInfoService.RemotingConnectorInfo> remotingConnectorInfo = new InjectedValue<>();
    private final ServiceName remotingServerInfoServiceName;

    private volatile ValueDependency<Group> group;
    private volatile String clientMappingsClusterName;
    private volatile ServiceName name;

    public EJBRemotingConnectorClientMappingsEntryProviderService(String clientMappingsClusterName, ServiceName remotingServerInfoServiceName) {
        this.clientMappingsClusterName = clientMappingsClusterName;
        this.remotingServerInfoServiceName = remotingServerInfoServiceName;
    }

    @Override
    public ServiceName getServiceName() {
        return this.name;
    }

    @Override
    public Builder<Map.Entry<String, List<ClientMapping>>> configure(OperationContext context) {
        this.name = ClusteringCacheRequirement.REGISTRY_ENTRY.getServiceName(context, this.clientMappingsClusterName, BeanManagerFactoryServiceConfiguratorConfiguration.CLIENT_MAPPINGS_CACHE_NAME);
        this.group = new InjectedValueDependency<>(ClusteringRequirement.GROUP.getServiceName(context, this.clientMappingsClusterName), Group.class);
        return this;
    }

    @Override
    public ServiceBuilder<Map.Entry<String, List<ClientMapping>>> build(ServiceTarget target) {
        ServiceBuilder<Map.Entry<String, List<ClientMapping>>> builder = target.addService(this.name, new ValueService<>(this))
                .addDependency(this.remotingServerInfoServiceName, RemotingConnectorBindingInfoService.RemotingConnectorInfo.class, this.remotingConnectorInfo)
                ;
        return this.group.register(builder);
    }

    @Override
    public Map.Entry<String, List<ClientMapping>> getValue() {
        return new AbstractMap.SimpleImmutableEntry<>(this.group.getValue().getLocalMember().getName(), this.getClientMappings());
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
}
