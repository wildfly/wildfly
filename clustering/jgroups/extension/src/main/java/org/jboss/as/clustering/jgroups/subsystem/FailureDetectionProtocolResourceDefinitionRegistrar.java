/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.network.ClientMapping;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jgroups.protocols.FD_SOCK2;
import org.jgroups.protocols.TP;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.ChannelFactoryConfiguration;
import org.wildfly.clustering.jgroups.spi.TransportConfiguration;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a resource definition for an FD_SOCK2 protocol.
 * @author Paul Ferraro
 */
public class FailureDetectionProtocolResourceDefinitionRegistrar extends SocketProtocolResourceDefinitionRegistrar<FD_SOCK2> {

    FailureDetectionProtocolResourceDefinitionRegistrar(SocketProtocolResourceRegistration registration, ResourceOperationRuntimeHandler parentRuntimeHandler) {
        super(new Configurator() {
            @Override
            public ResourceRegistration getResourceRegistration() {
                return registration;
            }

            @Override
            public ResourceOperationRuntimeHandler getParentRuntimeHandler() {
                return parentRuntimeHandler;
            }
        });
    }

    @Override
    public ServiceDependency<ProtocolConfiguration<FD_SOCK2>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        ServiceDependency<ProtocolConfiguration<FD_SOCK2>> protocol = super.resolve(context, model);
        ServiceDependency<SocketBinding> serverBinding = SocketBindingAttribute.SERVER.resolve(context, model);
        ServiceDependency<SocketBinding> clientBinding = SocketBindingAttribute.CLIENT.resolve(context, model);
        ServiceDependency<TransportConfiguration<TP>> transport = TRANSPORT.resolve(context, model);
        return new ServiceDependency<>() {
            @Override
            public void accept(RequirementServiceBuilder<?> builder) {
                protocol.accept(builder);
                serverBinding.accept(builder);
                clientBinding.accept(builder);
                transport.accept(builder);
            }

            @Override
            public ProtocolConfiguration<FD_SOCK2> get() {
                return new ProtocolConfigurationDecorator<>(protocol.get()) {
                    @Override
                    public FD_SOCK2 createProtocol(ChannelFactoryConfiguration stackConfiguration) {
                        FD_SOCK2 protocol = super.createProtocol(stackConfiguration);
                        SocketBinding protocolBinding = serverBinding.get();
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
                        SocketBinding socketBinding = serverBinding.get();
                        return (socketBinding != null) ? Map.of("jgroups.nio.server.fd_sock", socketBinding) : Map.of();
                    }
                };
            }
        };
    }
}
