/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.SimpleResourceDescriptorConfigurator;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jgroups.protocols.BasicTCP;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.ProtocolStackConfiguration;
import org.wildfly.clustering.jgroups.spi.TransportConfiguration;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceRecorder;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * @author Paul Ferraro
 */
public class SocketTransportResourceDefinition<T extends BasicTCP> extends TransportResourceDefinition<T> {

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        CLIENT_SOCKET_BINDING("client-socket-binding", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder
                        .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
                        .setCapabilityReference(CapabilityReferenceRecorder.builder(CAPABILITY, SocketBinding.SERVICE_DESCRIPTOR).build());
            }
        },
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(false)
                    .setRequired(false)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    ).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }

        @Override
        public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
            return builder;
        }
    }

    SocketTransportResourceDefinition(String name) {
        super(pathElement(name), new SimpleResourceDescriptorConfigurator<>(Attribute.class));
    }

    @Override
    public Map.Entry<Function<ProtocolConfiguration<T>, TransportConfiguration<T>>, Consumer<RequirementServiceBuilder<?>>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        String bindingName = Attribute.CLIENT_SOCKET_BINDING.resolveModelAttribute(context, model).asStringOrNull();

        ServiceDependency<SocketBinding> binding = (bindingName != null) ? ServiceDependency.on(SocketBinding.SERVICE_DESCRIPTOR, bindingName) : ServiceDependency.of(null);

        Map.Entry<Function<ProtocolConfiguration<T>, TransportConfiguration<T>>, Consumer<RequirementServiceBuilder<?>>> entry = super.resolve(context, model);
        return Map.entry(entry.getKey().andThen(new UnaryOperator<>() {
            @Override
            public TransportConfiguration<T> apply(TransportConfiguration<T> configuration) {
                return new TransportConfigurationDecorator<>(configuration) {
                    @Override
                    public T createProtocol(ProtocolStackConfiguration stackConfiguration) {
                        T transport = super.createProtocol(stackConfiguration);
                        SocketBinding clientBinding = binding.get();
                        if (clientBinding != null) {
                            InetSocketAddress socketAddress = clientBinding.getSocketAddress();
                            this.setValue(transport, "client_bind_addr", socketAddress.getAddress());
                            this.setValue(transport, "client_bind_port", socketAddress.getPort());
                        }
                        return transport;
                    }

                    @Override
                    public Map<String, SocketBinding> getSocketBindings() {
                        Map<String, SocketBinding> bindings = new TreeMap<>();
                        SocketBinding clientBinding = binding.get();
                        for (String serviceName : Set.of("jgroups.tcp.sock", "jgroups.nio.client")) {
                            bindings.put(serviceName, clientBinding);
                        }
                        return bindings;
                    }
                };
            }
        }), entry.getValue().andThen(binding));
    }
}
