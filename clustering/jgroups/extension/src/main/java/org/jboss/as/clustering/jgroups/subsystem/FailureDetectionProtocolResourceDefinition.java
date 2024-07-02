/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.clustering.controller.ResourceDescriptor;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.network.ClientMapping;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jgroups.protocols.FD_SOCK2;
import org.jgroups.protocols.TP;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.ProtocolStackConfiguration;
import org.wildfly.clustering.jgroups.spi.TransportConfiguration;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * @author Paul Ferraro
 */
public class FailureDetectionProtocolResourceDefinition extends SocketProtocolResourceDefinition<FD_SOCK2> {

    FailureDetectionProtocolResourceDefinition(String name, UnaryOperator<ResourceDescriptor> configurator, ResourceServiceConfigurator parentServiceConfigurator) {
        super(name, configurator, parentServiceConfigurator);
    }

    @Override
    public Map.Entry<Function<ProtocolConfiguration<FD_SOCK2>, ProtocolConfiguration<FD_SOCK2>>, Consumer<RequirementServiceBuilder<?>>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        String bindingName = Attribute.SOCKET_BINDING.resolveModelAttribute(context, model).asStringOrNull();
        String clientBindingName = Attribute.CLIENT_SOCKET_BINDING.resolveModelAttribute(context, model).asStringOrNull();

        ServiceDependency<SocketBinding> binding = (bindingName != null) ? ServiceDependency.on(SocketBinding.SERVICE_DESCRIPTOR, bindingName) : ServiceDependency.of(null);
        ServiceDependency<SocketBinding> clientBinding = (clientBindingName != null) ? ServiceDependency.on(SocketBinding.SERVICE_DESCRIPTOR, clientBindingName) : ServiceDependency.of(null);
        ServiceDependency<TransportConfiguration<TP>> transport = ServiceDependency.on(TransportConfiguration.SERVICE_DESCRIPTOR, context.getCurrentAddress().getParent().getLastElement().getValue());

        return Map.entry(new UnaryOperator<>() {
            @Override
            public ProtocolConfiguration<FD_SOCK2> apply(ProtocolConfiguration<FD_SOCK2> configuration) {
                return new ProtocolConfigurationDecorator<>(configuration) {
                    @Override
                    public FD_SOCK2 createProtocol(ProtocolStackConfiguration stackConfiguration) {
                        FD_SOCK2 protocol = super.createProtocol(stackConfiguration);
                        SocketBinding protocolBinding = binding.get();
                        SocketBinding transportBinding = transport.get().getSocketBinding();

                        InetSocketAddress protocolBindAddress = (protocolBinding != null) ? protocolBinding.getSocketAddress() : null;
                        InetSocketAddress transportBindAddress = transportBinding.getSocketAddress();
                        protocol.setBindAddress(((protocolBindAddress != null) ? protocolBindAddress : transportBindAddress).getAddress());

                        if (protocolBinding != null) {
                            protocol.setOffset(protocolBindAddress.getPort() - transportBindAddress.getPort());

                            List<ClientMapping> clientMappings = protocolBinding.getClientMappings();
                            if (!clientMappings.isEmpty()) {
                                // JGroups cannot select a client mapping based on the source address, so just use the first one
                                ClientMapping mapping = clientMappings.get(0);
                                try {
                                    protocol.setExternalAddress(InetAddress.getByName(mapping.getDestinationAddress()));
                                    protocol.setExternalPort(mapping.getDestinationPort());
                                } catch (UnknownHostException e) {
                                    throw new IllegalArgumentException(e);
                                }
                            }
                        }

                        SocketBinding clientSocketBinding = clientBinding.get();
                        if (clientSocketBinding != null) {
                            protocol.setClientBindPort(clientBinding.get().getSocketAddress().getPort());
                        }
                        return protocol;
                    }

                    @Override
                    public Map<String, SocketBinding> getSocketBindings() {
                        // Socket binding is optional
                        SocketBinding socketBinding = binding.get();
                        return (socketBinding != null) ? Map.of("jgroups.nio.server.fd_sock", socketBinding) : Map.of();
                    }
                };
            }
        }, binding.andThen(clientBinding).andThen(transport));
    }
}
