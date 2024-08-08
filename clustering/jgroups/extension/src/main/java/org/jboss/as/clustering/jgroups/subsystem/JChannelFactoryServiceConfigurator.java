/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jboss.as.clustering.jgroups.JChannelFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.network.SocketBindingManager;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.dmr.ModelNode;
import org.jgroups.protocols.TP;
import org.jgroups.stack.Protocol;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.ProtocolStackConfiguration;
import org.wildfly.clustering.jgroups.spi.RelayConfiguration;
import org.wildfly.clustering.jgroups.spi.TransportConfiguration;
import org.wildfly.common.function.Functions;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Builder for a service that provides a {@link ChannelFactory} for creating channels.
 * @author Paul Ferraro
 */
public enum JChannelFactoryServiceConfigurator implements ResourceServiceConfigurator {
    INSTANCE;

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String name = context.getCurrentAddressValue();
        boolean statisticsEnabled = StackResourceDefinition.Attribute.STATISTICS_ENABLED.resolveModelAttribute(context, model).asBoolean();

        Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);

        ServiceDependency<TransportConfiguration<TP>> transport = ServiceDependency.on(TransportConfiguration.SERVICE_DESCRIPTOR, name);
        Set<String> protocolNames = resource.getChildrenNames(ProtocolResourceDefinition.WILDCARD_PATH.getKey());
        List<ServiceDependency<ProtocolConfiguration<Protocol>>> protocols = new ArrayList<>(protocolNames.size());
        for (String protocolName : protocolNames) {
            protocols.add(ServiceDependency.on(ProtocolConfiguration.SERVICE_DESCRIPTOR, name, protocolName));
        }
        ServiceDependency<RelayConfiguration> relay = resource.hasChild(RelayResourceDefinition.PATH) ? ServiceDependency.on(RelayConfiguration.SERVICE_DESCRIPTOR, name) : ServiceDependency.of(null);

        ServiceDependency<ServerEnvironment> environment = ServiceDependency.on(ServerEnvironment.SERVICE_DESCRIPTOR);
        ServiceDependency<SocketBindingManager> socketBindingManager = ServiceDependency.on(SocketBindingManager.SERVICE_DESCRIPTOR);

        ProtocolStackConfiguration configuration = new ProtocolStackConfiguration() {
            @Override
            public boolean isStatisticsEnabled() {
                return statisticsEnabled;
            }

            @Override
            public TransportConfiguration<? extends TP> getTransport() {
                return transport.get();
            }

            @Override
            public List<ProtocolConfiguration<? extends Protocol>> getProtocols() {
                return protocols.stream().map(Supplier::get).collect(Collectors.toUnmodifiableList());
            }

            @Override
            public String getMemberName() {
                return environment.get().getNodeName();
            }

            @Override
            public Optional<RelayConfiguration> getRelay() {
                return Optional.ofNullable(relay.get());
            }

            @Override
            public SocketBindingManager getSocketBindingManager() {
                return socketBindingManager.get();
            }
        };
        return CapabilityServiceInstaller.builder(StackResourceDefinition.CHANNEL_FACTORY, JChannelFactory::new, Functions.constantSupplier(configuration))
                .requires(List.of(transport, relay, environment, socketBindingManager))
                .requires(protocols)
                .build();
    }
}
