/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jgroups.protocols.BasicTCP;
import org.wildfly.clustering.jgroups.spi.TransportConfiguration;
import org.wildfly.clustering.jgroups.spi.ChannelFactoryConfiguration;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceAttributeDefinition;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * @author Paul Ferraro
 */
public class SocketTransportResourceDefinitionRegistrar<T extends BasicTCP> extends AbstractTransportResourceDefinitionRegistrar<T> {

    static final CapabilityReferenceAttributeDefinition<SocketBinding> CLIENT_SOCKET_BINDING = new CapabilityReferenceAttributeDefinition.Builder<>("client-socket-binding", CapabilityReference.builder(CAPABILITY, SocketBinding.SERVICE_DESCRIPTOR).build())
            .setRequired(false)
            .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
            .build();

    private static final Collection<AttributeDefinition> ATTRIBUTES = List.of(CLIENT_SOCKET_BINDING);
    static Stream<AttributeDefinition> attributes() {
        return Stream.concat(ATTRIBUTES.stream(), AbstractTransportResourceDefinitionRegistrar.attributes());
    }

    SocketTransportResourceDefinitionRegistrar(String name, ResourceOperationRuntimeHandler parentRuntimeHandler) {
        super(new TransportResourceRegistration<>() {
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
                return TransportResourceRegistration.super.apply(builder)
                        .addAttributes(ATTRIBUTES)
                        ;
            }

            @Override
            public ServiceDependency<TransportConfiguration<T>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
                return TransportResourceRegistration.super.resolve(context, model).combine(CLIENT_SOCKET_BINDING.resolve(context, model), new BiFunction<>() {
                    @Override
                    public TransportConfiguration<T> apply(TransportConfiguration<T> config, SocketBinding binding) {
                        return new TransportConfigurationDecorator<>(config) {
                            @Override
                            public T createProtocol(ChannelFactoryConfiguration stackConfiguration) {
                                T transport = super.createProtocol(stackConfiguration);
                                if (binding != null) {
                                    InetSocketAddress socketAddress = binding.getSocketAddress();
                                    this.setValue(transport, "client_bind_addr", socketAddress.getAddress());
                                    this.setValue(transport, "client_bind_port", socketAddress.getPort());
                                }
                                return transport;
                            }

                            @Override
                            public Map<String, SocketBinding> getSocketBindings() {
                                Map<String, SocketBinding> bindings = super.getSocketBindings();
                                if (binding != null) {
                                    bindings = new TreeMap<>(bindings);
                                    for (String serviceName : Set.of("jgroups.tcp.sock", "jgroups.nio.client")) {
                                        bindings.put(serviceName, binding);
                                    }
                                }
                                return bindings;
                            }
                        };
                    }
                });
            }
        });
    }
}
