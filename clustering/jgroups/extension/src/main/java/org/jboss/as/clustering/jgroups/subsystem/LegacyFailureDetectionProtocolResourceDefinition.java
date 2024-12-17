/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.network.ClientMapping;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jgroups.protocols.FD_SOCK;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.ProtocolStackConfiguration;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * @author Paul Ferraro
 */
public class LegacyFailureDetectionProtocolResourceDefinition extends SocketProtocolResourceDefinition<FD_SOCK> {

    public LegacyFailureDetectionProtocolResourceDefinition(String name, UnaryOperator<ResourceDescriptor> configurator, ResourceServiceConfigurator parentServiceConfigurator) {
        super(name, configurator, parentServiceConfigurator);
    }

    @Override
    public Map.Entry<Function<ProtocolConfiguration<FD_SOCK>, ProtocolConfiguration<FD_SOCK>>, Consumer<RequirementServiceBuilder<?>>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        String bindingName = Attribute.SOCKET_BINDING.resolveModelAttribute(context, model).asStringOrNull();
        String clientBindingName = Attribute.CLIENT_SOCKET_BINDING.resolveModelAttribute(context, model).asStringOrNull();

        ServiceDependency<SocketBinding> binding = (bindingName != null) ? ServiceDependency.on(SocketBinding.SERVICE_DESCRIPTOR, bindingName) : ServiceDependency.of(null);
        ServiceDependency<SocketBinding> clientBinding = (clientBindingName != null) ? ServiceDependency.on(SocketBinding.SERVICE_DESCRIPTOR, clientBindingName) : ServiceDependency.of(null);

        return Map.entry(new UnaryOperator<>() {
            @Override
            public ProtocolConfiguration<FD_SOCK> apply(ProtocolConfiguration<FD_SOCK> configuration) {
                return new ProtocolConfigurationDecorator<>(configuration) {
                    @Override
                    public FD_SOCK createProtocol(ProtocolStackConfiguration stackConfiguration) {
                        FD_SOCK protocol = super.createProtocol(stackConfiguration);
                        // If binding is undefined, protocol will use bind address of transport and a random ephemeral port
                        SocketBinding socketBinding = binding.get();
                        protocol.setStartPort((socketBinding != null) ? socketBinding.getAbsolutePort() : 0);

                        if (socketBinding != null) {
                            protocol.setBindAddress(socketBinding.getAddress());

                            List<ClientMapping> clientMappings = socketBinding.getClientMappings();
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
                            protocol.setClientBindPort(clientSocketBinding.getSocketAddress().getPort());
                        }
                        return protocol;
                    }

                    @Override
                    public Map<String, SocketBinding> getSocketBindings() {
                        Map<String, SocketBinding> result = new TreeMap<>();
                        SocketBinding socketBinding = binding.get();
                        if (socketBinding != null) {
                            result.put("jgroups.fd_sock.srv_sock", socketBinding);
                        }
                        SocketBinding clientSocketBinding = clientBinding.get();
                        if (clientSocketBinding != null) {
                            result.put("jgroups.fd.ping_sock", clientSocketBinding);
                        }
                        return result;
                    }
                };
            }
        }, binding.andThen(clientBinding));
    }
}
