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
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceBuilderFactory;
import org.jboss.as.clustering.jgroups.protocol.JDBCProtocol;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
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

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        DATA_SOURCE("data-source", ModelType.STRING, builder -> builder.setCapabilityReference(new CapabilityReference(Capability.PROTOCOL, CommonUnaryRequirement.DATA_SOURCE))),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, UnaryOperator<SimpleAttributeDefinitionBuilder> configurator) {
            this.definition = configurator.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(false)
                    .setRequired(true)
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

    JDBCProtocolResourceDefinition(String name, Consumer<ResourceDescriptor> descriptorConfigurator, ResourceServiceBuilderFactory<ChannelFactory> parentBuilderFactory) {
        super(pathElement(name), descriptorConfigurator.andThen(descriptor -> descriptor
                .addAttributes(Attribute.class)
                .setAddOperationTransformation(new LegacyAddOperationTransformation(operation -> !operation.hasDefined(Attribute.DATA_SOURCE.getName())))
                ), address -> new JDBCProtocolConfigurationBuilder<>(address), parentBuilderFactory);
    }
}
