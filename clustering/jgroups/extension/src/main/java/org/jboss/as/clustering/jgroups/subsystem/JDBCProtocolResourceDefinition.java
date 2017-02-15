/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

import java.util.function.Consumer;

import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceBuilderFactory;
import org.jboss.as.clustering.jgroups.protocol.JDBCProtocol;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CapabilityReferenceRecorder;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelType;
import org.jgroups.stack.Protocol;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;

/**
 * Resource definition override for protocols that require a JDBC DataSource.
 * @author Paul Ferraro
 */
public class JDBCProtocolResourceDefinition<P extends Protocol & JDBCProtocol> extends ProtocolResourceDefinition<P> {

    enum Capability implements org.jboss.as.clustering.controller.Capability {
        DATA_SOURCE("org.wildfly.clustering.jgroups.jdbc-ping.data-source"),
        ;
        private final RuntimeCapability<Void> definition;

        Capability(String name) {
            this.definition = RuntimeCapability.Builder.of(name, true).build();
        }

        @Override
        public RuntimeCapability<Void> getDefinition() {
            return this.definition;
        }

        @Override
        public RuntimeCapability<Void> resolve(PathAddress address) {
            return this.definition.fromBaseCapability(address.getParent().getLastElement().getValue() + "." + address.getLastElement().getValue());
        }
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        DATA_SOURCE("data-source", ModelType.STRING, new CapabilityReference(Capability.DATA_SOURCE, CommonUnaryRequirement.DATA_SOURCE)),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, CapabilityReferenceRecorder reference) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(false)
                    .setRequired(true)
                    .setCapabilityReference(reference)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static void addTransformations(ModelVersion version, ResourceTransformationDescriptionBuilder builder) {

        ProtocolResourceDefinition.addTransformations(version, builder);
    }

    JDBCProtocolResourceDefinition(String name, Consumer<ResourceDescriptor> descriptorConfigurator, ResourceServiceBuilderFactory<ChannelFactory> parentBuilderFactory) {
        super(pathElement(name), descriptorConfigurator.andThen(descriptor -> descriptor
                .addAttributes(Attribute.class)
                .addCapabilities(Capability.class)
            ), address -> new JDBCProtocolConfigurationBuilder<>(address), parentBuilderFactory);
    }
}
