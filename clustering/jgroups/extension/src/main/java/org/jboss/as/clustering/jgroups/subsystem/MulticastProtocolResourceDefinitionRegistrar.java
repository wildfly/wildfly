/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jgroups.protocols.MPING;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.ChannelFactoryConfiguration;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceAttributeDefinition;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a resource definition for an multicast discovery protocol, i.e. MPING.
 * @author Paul Ferraro
 */
public class MulticastProtocolResourceDefinitionRegistrar extends AbstractProtocolResourceDefinitionRegistrar<MPING> {
    enum Protocol implements ResourceRegistration {
        MPING
        ;

        private final PathElement path = StackResourceDefinitionRegistrar.Component.PROTOCOL.pathElement(this.name());

        @Override
        public PathElement getPathElement() {
            return this.path;
        }
    }

    static final CapabilityReferenceAttributeDefinition<SocketBinding> SOCKET_BINDING = new CapabilityReferenceAttributeDefinition.Builder<>(ModelDescriptionConstants.SOCKET_BINDING, CapabilityReference.builder(CAPABILITY, SocketBinding.SERVICE_DESCRIPTOR).build())
            .setRequired(true)
            .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
            .build();

    MulticastProtocolResourceDefinitionRegistrar(Protocol registration, ResourceOperationRuntimeHandler parentRuntimeHandler) {
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
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder).addAttributes(List.of(SOCKET_BINDING));
    }

    @Override
    public ServiceDependency<ProtocolConfiguration<MPING>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        return super.resolve(context, model).combine(SOCKET_BINDING.resolve(context, model), new BiFunction<>() {
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
}
