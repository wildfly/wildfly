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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.network.ClientMapping;
import org.jboss.as.remoting.RemotingConnectorBindingInfoService;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.ejb.BeanManagerFactoryServiceConfiguratorConfiguration;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.spi.ClusteringCacheRequirement;
import org.wildfly.clustering.spi.ClusteringRequirement;
import org.wildfly.common.net.Inet;

/**
 * @author Jaikiran Pai
 */
public class EJBRemotingConnectorClientMappingsEntryProviderService implements CapabilityServiceConfigurator, Supplier<Map.Entry<String, List<ClientMapping>>> {

    private final SupplierDependency<RemotingConnectorBindingInfoService.RemotingConnectorInfo> remotingConnectorInfo;

    private volatile SupplierDependency<Group> group;
    private volatile String clientMappingsClusterName;
    private volatile ServiceName name;

    public EJBRemotingConnectorClientMappingsEntryProviderService(String clientMappingsClusterName, ServiceName remotingConnectorInfoServiceName) {
        this.clientMappingsClusterName = clientMappingsClusterName;
        this.remotingConnectorInfo = new ServiceSupplierDependency<>(remotingConnectorInfoServiceName);
    }

    @Override
    public ServiceName getServiceName() {
        return this.name;
    }

    @Override
    public ServiceConfigurator configure(OperationContext context) {
        this.name = ClusteringCacheRequirement.REGISTRY_ENTRY.getServiceName(context, this.clientMappingsClusterName, BeanManagerFactoryServiceConfiguratorConfiguration.CLIENT_MAPPINGS_CACHE_NAME);
        this.group = new ServiceSupplierDependency<>(ClusteringRequirement.GROUP.getServiceName(context, this.clientMappingsClusterName));
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.name);
        Consumer<Map.Entry<String, List<ClientMapping>>> entry = new CompositeDependency(this.group, this.remotingConnectorInfo).register(builder).provides(this.name);
        Service service = new FunctionalService<>(entry, Function.identity(), this);
        return builder.setInstance(service);
    }

    @Override
    public Map.Entry<String, List<ClientMapping>> get() {
        return new AbstractMap.SimpleImmutableEntry<>(this.group.get().getLocalMember().getName(), this.getClientMappings());
    }

    /**
     * This method provides client-mapping entries for all connected EJB clients.
     * It returns either a set of user-defined client mappings for a multi-homed host or a single default client mapping for the single-homed host.
     * Hostnames are preferred over literal IP addresses for the destination address part (due to EJBCLIENT-349).
     *
     * @return the client mappings for this host
     */
    List<ClientMapping> getClientMappings() {
        final List<ClientMapping> ret = new ArrayList<>();
        RemotingConnectorBindingInfoService.RemotingConnectorInfo info = this.remotingConnectorInfo.get();

        if (info.getSocketBinding().getClientMappings() != null && !info.getSocketBinding().getClientMappings().isEmpty()) {
            ret.addAll(info.getSocketBinding().getClientMappings());
        } else {
            // for the destination, prefer the hostname over the literal IP address
            final InetAddress destination = info.getSocketBinding().getAddress();
            final String destinationName = Inet.toURLString(destination, true);

            // for the network, send a CIDR that is compatible with the address we are sending
            final InetAddress clientNetworkAddress;
            try {
                if (destination instanceof Inet4Address) {
                    clientNetworkAddress = InetAddress.getByName("0.0.0.0");
                } else {
                    clientNetworkAddress = InetAddress.getByName("::");
                }
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
            final ClientMapping defaultClientMapping = new ClientMapping(clientNetworkAddress, 0, destinationName, info.getSocketBinding().getAbsolutePort());
            ret.add(defaultClientMapping);
        }
        return ret;
    }
}
