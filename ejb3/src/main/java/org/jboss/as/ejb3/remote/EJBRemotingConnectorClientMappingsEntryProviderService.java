/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.remote;

import static org.jboss.as.ejb3.subsystem.EJB3RemoteResourceDefinition.CONNECTOR_CAPABILITY_NAME;

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
import org.jboss.as.network.ProtocolSocketBinding;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.server.service.ClusteringRequirement;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.common.net.Inet;

/**
 * @author Jaikiran Pai
 */
public class EJBRemotingConnectorClientMappingsEntryProviderService extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, Supplier<Map.Entry<String, List<ClientMapping>>> {
    private static final ServiceName BASE_NAME = ServiceName.JBOSS.append("ejb", "remote", "client-mappings");

    private volatile SupplierDependency<ProtocolSocketBinding> remotingConnectorInfo;
    private final String containerName;
    private final String connectorName;

    private volatile SupplierDependency<Group> group;

    public EJBRemotingConnectorClientMappingsEntryProviderService(String containerName, String connectorName) {
        super(BASE_NAME.append(connectorName));
        this.containerName = containerName;
        this.connectorName = connectorName;
    }

    @Override
    public ServiceConfigurator configure(OperationContext context) {
        this.group = new ServiceSupplierDependency<>(ClusteringRequirement.GROUP.getServiceName(context, this.containerName));
        this.remotingConnectorInfo = new ServiceSupplierDependency<>(context.getCapabilityServiceName(CONNECTOR_CAPABILITY_NAME, connectorName, ProtocolSocketBinding.class));
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceName name = this.getServiceName();
        ServiceBuilder<?> builder = target.addService(name);
        Consumer<Map.Entry<String, List<ClientMapping>>> entry = new CompositeDependency(this.group, this.remotingConnectorInfo).register(builder).provides(name);
        Service service = new FunctionalService<>(entry, Function.identity(), this);
        return builder.setInstance(service);
    }

    @Override
    public Map.Entry<String, List<ClientMapping>> get() {
        return new AbstractMap.SimpleImmutableEntry<>(this.group.get().getLocalMember().getName(), this.getClientMappings());
    }

    /**
     * This method provides client-mapping entries for all connected Jakarta Enterprise Beans clients.
     * It returns either a set of user-defined client mappings for a multi-homed host or a single default client mapping for the single-homed host.
     * Hostnames are preferred over literal IP addresses for the destination address part (due to EJBCLIENT-349).
     *
     * @return the client mappings for this host
     */
    List<ClientMapping> getClientMappings() {
        final List<ClientMapping> ret = new ArrayList<>();
        ProtocolSocketBinding info = this.remotingConnectorInfo.get();

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
