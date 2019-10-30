/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelType;

/**
 * @author Paul Ferraro
 */
public class SocketDiscoveryProtocolResourceDefinition<A> extends ProtocolResourceDefinition {

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<StringListAttributeDefinition.Builder> {
        OUTBOUND_SOCKET_BINDINGS("socket-bindings", ModelType.LIST) {
            @Override
            public StringListAttributeDefinition.Builder apply(StringListAttributeDefinition.Builder builder) {
                return builder.setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
                        .setCapabilityReference(new CapabilityReference(Capability.PROTOCOL, CommonUnaryRequirement.OUTBOUND_SOCKET_BINDING))
                        ;
            }
        },
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type) {
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

    static void addTransformations(ModelVersion version, ResourceTransformationDescriptionBuilder builder) {

        ProtocolResourceDefinition.addTransformations(version, builder);
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
