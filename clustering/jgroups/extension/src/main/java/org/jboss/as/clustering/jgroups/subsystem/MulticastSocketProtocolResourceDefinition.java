/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.clustering.jgroups.subsystem.MulticastSocketProtocolResourceDefinition.Attribute.SOCKET_BINDING;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jgroups.protocols.MPING;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.ProtocolStackConfiguration;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceRecorder;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Resource definition override for protocols that require a socket-binding.
 * @author Paul Ferraro
 */
public class MulticastSocketProtocolResourceDefinition extends ProtocolResourceDefinition<MPING> {

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        SOCKET_BINDING(ModelDescriptionConstants.SOCKET_BINDING, ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
                        .setCapabilityReference(CapabilityReferenceRecorder.builder(CAPABILITY, SocketBinding.SERVICE_DESCRIPTOR).build())
                        ;
            }
        },
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setRequired(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    ).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    private static class ResourceDescriptorConfigurator implements UnaryOperator<ResourceDescriptor> {
        private final UnaryOperator<ResourceDescriptor> configurator;

        ResourceDescriptorConfigurator(UnaryOperator<ResourceDescriptor> configurator) {
            this.configurator = configurator;
        }

        @Override
        public ResourceDescriptor apply(ResourceDescriptor descriptor) {
            return this.configurator.apply(descriptor).addAttributes(Attribute.class);
        }
    }

    MulticastSocketProtocolResourceDefinition(String name, UnaryOperator<ResourceDescriptor> configurator, ResourceServiceConfigurator parentServiceConfigurator) {
        super(pathElement(name), new ResourceDescriptorConfigurator(configurator), parentServiceConfigurator);
    }

    @Override
    public Map.Entry<Function<ProtocolConfiguration<MPING>, ProtocolConfiguration<MPING>>, Consumer<RequirementServiceBuilder<?>>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        ServiceDependency<SocketBinding> binding = ServiceDependency.on(SocketBinding.SERVICE_DESCRIPTOR, SOCKET_BINDING.resolveModelAttribute(context, model).asString());
        return Map.entry(new UnaryOperator<>() {
            @Override
            public ProtocolConfiguration<MPING> apply(ProtocolConfiguration<MPING> configuration) {
                return new ProtocolConfigurationDecorator<>(configuration) {
                    @Override
                    public MPING createProtocol(ProtocolStackConfiguration stackConfiguration) {
                        SocketBinding multicastBinding = binding.get();
                        MPING protocol = super.createProtocol(stackConfiguration);
                        protocol.setBindAddr(multicastBinding.getAddress());
                        protocol.setMcastAddr(multicastBinding.getMulticastAddress());
                        protocol.setMcastPort(multicastBinding.getMulticastPort());
                        return protocol;
                    }

                    @Override
                    public Map<String, SocketBinding> getSocketBindings() {
                        SocketBinding multicastBinding = binding.get();
                        return Map.of("jgroups.mping.mcast_sock", multicastBinding, "jgroups.mping.mcast-send-sock", multicastBinding);
                    }
                };
            }
        }, binding);
    }
}
