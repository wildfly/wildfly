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

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.network.ClientMapping;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jgroups.protocols.FD_SOCK;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.ChannelFactoryConfiguration;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a resource definition for a legacy FD_SOCK protocol.
 * @author Paul Ferraro
 */
public class LegacyFailureDetectionProtocolResourceDefinitionRegistrar extends SocketProtocolResourceDefinitionRegistrar<FD_SOCK> {

    public LegacyFailureDetectionProtocolResourceDefinitionRegistrar(SocketProtocolResourceRegistration registration, ResourceOperationRuntimeHandler parentRuntimeHandler) {
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
    public ServiceDependency<ProtocolConfiguration<FD_SOCK>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        ServiceDependency<ProtocolConfiguration<FD_SOCK>> protocol = super.resolve(context, model);
        ServiceDependency<SocketBinding> serverBinding = FailureDetectionProtocolResourceDefinitionRegistrar.SocketBindingAttribute.SERVER.resolve(context, model);
        ServiceDependency<SocketBinding> clientBinding = FailureDetectionProtocolResourceDefinitionRegistrar.SocketBindingAttribute.CLIENT.resolve(context, model);
        return new ServiceDependency<>() {
            @Override
            public void accept(RequirementServiceBuilder<?> builder) {
                protocol.accept(builder);
                serverBinding.accept(builder);
                clientBinding.accept(builder);
            }

            @Override
            public ProtocolConfiguration<FD_SOCK> get() {
                return new ProtocolConfigurationDecorator<>(protocol.get()) {
                    @Override
                    public FD_SOCK createProtocol(ChannelFactoryConfiguration stackConfiguration) {
                        FD_SOCK protocol = super.createProtocol(stackConfiguration);
                        // If binding is undefined, protocol will use bind address of transport and a random ephemeral port
                        SocketBinding serverSocketBinding = serverBinding.get();
                        protocol.setStartPort((serverSocketBinding != null) ? serverSocketBinding.getAbsolutePort() : 0);

                        if (serverSocketBinding != null) {
                            protocol.setBindAddress(serverSocketBinding.getAddress());

                            List<ClientMapping> clientMappings = serverSocketBinding.getClientMappings();
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
                        SocketBinding serverSocketBinding = serverBinding.get();
                        if (serverSocketBinding != null) {
                            result.put("jgroups.fd_sock.srv_sock", serverSocketBinding);
                        }
                        SocketBinding clientSocketBinding = clientBinding.get();
                        if (clientSocketBinding != null) {
                            result.put("jgroups.fd.ping_sock", clientSocketBinding);
                        }
                        return result;
                    }
                };
            }
        };
    }
}
