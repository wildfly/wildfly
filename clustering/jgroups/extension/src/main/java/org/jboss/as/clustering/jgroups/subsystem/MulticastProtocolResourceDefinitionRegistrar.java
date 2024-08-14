/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.Map;
import java.util.function.BiFunction;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jgroups.protocols.MPING;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.ChannelFactoryConfiguration;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a resource definition for an multicast discovery protocol, i.e. MPING.
 * @author Paul Ferraro
 */
public class MulticastProtocolResourceDefinitionRegistrar extends AbstractProtocolResourceDefinitionRegistrar<MPING> {

    MulticastProtocolResourceDefinitionRegistrar(MulticastProtocolResourceDescription description, ResourceOperationRuntimeHandler parentRuntimeHandler) {
        super(new ProtocolResourceDescriptorConfigurator<>() {
            @Override
            public ProtocolResourceDescription getResourceDescription() {
                return description;
            }

            @Override
            public ResourceOperationRuntimeHandler getParentRuntimeHandler() {
                return parentRuntimeHandler;
            }

            @Override
            public ServiceDependency<ProtocolConfiguration<MPING>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
                return ProtocolResourceDescriptorConfigurator.super.resolve(context, model).combine(MulticastProtocolResourceDescription.SOCKET_BINDING.resolve(context, model), new BiFunction<>() {
                    @Override
                    public ProtocolConfiguration<MPING> apply(ProtocolConfiguration<MPING> config, SocketBinding binding) {
                        return new ProtocolConfigurationDecorator<>(config) {
                            @Override
                            public MPING createProtocol(ChannelFactoryConfiguration stackConfiguration) {
                                MPING protocol = super.createProtocol(stackConfiguration);
                                protocol.setBindAddr(binding.getAddress());
                                protocol.setMcastAddr(binding.getMulticastAddress());
                                protocol.setMcastPort(binding.getMulticastPort());
                                return protocol;
                            }

                            @Override
                            public Map<String, SocketBinding> getSocketBindings() {
                                return Map.of("jgroups.mping.mcast_sock", binding, "jgroups.mping.mcast-send-sock", binding);
                            }
                        };
                    }
                });
            }
        });
    }
}
