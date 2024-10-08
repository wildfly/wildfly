/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.global.MapOperations;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.dmr.ModelNode;
import org.jgroups.protocols.Discovery;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.ChannelFactoryConfiguration;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceListAttributeDefinition;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a resource definition for a socket-based discovery protocol.
 * @author Paul Ferraro
 */
public class SocketDiscoveryProtocolResourceDefinitionRegistrar<A, P extends Discovery> extends AbstractProtocolResourceDefinitionRegistrar<P> {

    static final CapabilityReferenceListAttributeDefinition<OutboundSocketBinding> OUTBOUND_SOCKET_BINDINGS = new CapabilityReferenceListAttributeDefinition.Builder<>("socket-bindings", CapabilityReference.builder(CAPABILITY, OutboundSocketBinding.SERVICE_DESCRIPTOR).build())
            .setMinSize(1)
            .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
            .build();

    private static final Collection<AttributeDefinition> ATTRIBUTES = List.of(OUTBOUND_SOCKET_BINDINGS);

    static Stream<AttributeDefinition> attributes() {
        return Stream.concat(ATTRIBUTES.stream(), AbstractProtocolResourceDefinitionRegistrar.attributes());
    }

    SocketDiscoveryProtocolResourceDefinitionRegistrar(String name, Function<InetSocketAddress, A> hostTransformer, ResourceOperationRuntimeHandler parentRuntimeHandler) {
        super(new ProtocolResourceRegistration<>() {
            @Override
            public PathElement getPathElement() {
                return pathElement(name);
            }

            @Override
            public ResourceOperationRuntimeHandler getParentRuntimeHandler() {
                return parentRuntimeHandler;
            }

            @Override
            public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
                return ProtocolResourceRegistration.super.apply(builder)
                        .addAttributes(ATTRIBUTES)
                        .withOperationTransformation(ModelDescriptionConstants.ADD, new LegacyAddOperationTransformation(ATTRIBUTES))
                        .withOperationTransformation(Set.of(ModelDescriptionConstants.REMOVE, MapOperations.MAP_GET_DEFINITION.getName(), MapOperations.MAP_PUT_DEFINITION.getName(), MapOperations.MAP_REMOVE_DEFINITION.getName(), MapOperations.MAP_CLEAR_DEFINITION.getName()), LEGACY_OPERATION_TRANSFORMER)
                        ;
            }

            @Override
            public ServiceDependency<ProtocolConfiguration<P>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
                return ProtocolResourceRegistration.super.resolve(context, model).combine(OUTBOUND_SOCKET_BINDINGS.resolve(context, model), new BiFunction<>() {
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
