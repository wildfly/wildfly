/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.net.InetSocketAddress;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.clustering.controller.ResourceServiceConfiguratorFactory;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.registry.AttributeAccess;

/**
 * @author Paul Ferraro
 */
public class SocketDiscoveryProtocolResourceDefinition<A> extends ProtocolResourceDefinition {

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<StringListAttributeDefinition.Builder> {
        OUTBOUND_SOCKET_BINDINGS("socket-bindings") {
            @Override
            public StringListAttributeDefinition.Builder apply(StringListAttributeDefinition.Builder builder) {
                return builder.setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
                        .setCapabilityReference(new CapabilityReference(Capability.PROTOCOL, CommonUnaryRequirement.OUTBOUND_SOCKET_BINDING))
                        ;
            }
        },
        ;
        private final AttributeDefinition definition;

        Attribute(String name) {
            this.definition = this.apply(new StringListAttributeDefinition.Builder(name)
                    .setRequired(true)
                    .setMinSize(1)
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
            return this.configurator.apply(descriptor)
                    .addAttributes(Attribute.class)
                    .setAddOperationTransformation(new LegacyAddOperationTransformation(Attribute.class))
                    .setOperationTransformation(LEGACY_OPERATION_TRANSFORMER)
                    ;
        }
    }

    private static class SocketDiscoveryProtocolConfigurationConfiguratorFactory<A> implements ResourceServiceConfiguratorFactory {
        private final Function<InetSocketAddress, A> hostTransformer;

        SocketDiscoveryProtocolConfigurationConfiguratorFactory(Function<InetSocketAddress, A> hostTransformer) {
            this.hostTransformer = hostTransformer;
        }

        @Override
        public ResourceServiceConfigurator createServiceConfigurator(PathAddress address) {
            return new SocketDiscoveryProtocolConfigurationServiceConfigurator<>(address, this.hostTransformer);
        }
    }

    SocketDiscoveryProtocolResourceDefinition(String name, Function<InetSocketAddress, A> hostTransformer, UnaryOperator<ResourceDescriptor> configurator, ResourceServiceConfiguratorFactory parentServiceConfiguratorFactory) {
        super(pathElement(name), new ResourceDescriptorConfigurator(configurator), new SocketDiscoveryProtocolConfigurationConfiguratorFactory<>(hostTransformer), parentServiceConfiguratorFactory);
    }
}
