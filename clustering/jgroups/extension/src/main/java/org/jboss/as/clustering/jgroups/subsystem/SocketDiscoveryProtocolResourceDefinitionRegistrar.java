/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.global.MapOperations;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.dmr.ModelNode;
import org.jgroups.protocols.Discovery;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.ChannelFactoryConfiguration;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a resource definition for a socket-based discovery protocol.
 * @author Paul Ferraro
 */
public class SocketDiscoveryProtocolResourceDefinitionRegistrar<A, P extends Discovery> extends AbstractProtocolResourceDefinitionRegistrar<P> {

    SocketDiscoveryProtocolResourceDefinitionRegistrar(SocketDiscoveryProtocolResourceDescription description, Function<InetSocketAddress, A> hostTransformer, ResourceOperationRuntimeHandler parentRuntimeHandler) {
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
            public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
                return ProtocolResourceDescriptorConfigurator.super.apply(builder)
                        .withOperationTransformation(ModelDescriptionConstants.ADD, new LegacyAddOperationTransformation(List.of(SocketDiscoveryProtocolResourceDescription.OUTBOUND_SOCKET_BINDINGS)))
                        .withOperationTransformation(Set.of(ModelDescriptionConstants.REMOVE, MapOperations.MAP_GET_DEFINITION.getName(), MapOperations.MAP_PUT_DEFINITION.getName(), MapOperations.MAP_REMOVE_DEFINITION.getName(), MapOperations.MAP_CLEAR_DEFINITION.getName()), LEGACY_OPERATION_TRANSFORMER)
                        ;
            }

            @Override
            public ServiceDependency<ProtocolConfiguration<P>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
                return ProtocolResourceDescriptorConfigurator.super.resolve(context, model).combine(SocketDiscoveryProtocolResourceDescription.OUTBOUND_SOCKET_BINDINGS.resolve(context, model), new BiFunction<>() {
                    @Override
                    public ProtocolConfiguration<P> apply(ProtocolConfiguration<P> config, List<OutboundSocketBinding> bindings) {
                        return new ProtocolConfigurationDecorator<>(config) {
                            @Override
                            public P createProtocol(ChannelFactoryConfiguration stackConfiguration) {
                                P protocol = super.createProtocol(stackConfiguration);
                                if (!bindings.isEmpty()) {
                                    List<A> initialHosts = new ArrayList<>(bindings.size());
                                    for (OutboundSocketBinding binding : bindings) {
                                        try {
                                            initialHosts.add(hostTransformer.apply(new InetSocketAddress(binding.getResolvedDestinationAddress(), binding.getDestinationPort())));
                                        } catch (UnknownHostException e) {
                                            throw JGroupsLogger.ROOT_LOGGER.failedToResolveSocketBinding(e, binding);
                                        }
                                    }
                                    // In the absence of some common interface, we need to use reflection
                                    this.setValue(protocol, "initial_hosts", initialHosts);
                                }
                                return protocol;
                            }
                        };
                    }
                });
            }
        });
    }
}
