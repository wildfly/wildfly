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

import java.util.EnumSet;
import java.util.function.Consumer;

import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceBuilderFactory;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelType;
import org.jgroups.stack.Protocol;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;

/**
 * @author Paul Ferraro
 */
public class GenericProtocolResourceDefinition<P extends Protocol> extends ProtocolResourceDefinition<P> {

    public static PathElement pathElement(String name) {
        return ProtocolResourceDefinition.pathElement(String.join(".", org.jgroups.conf.ProtocolConfiguration.protocol_prefix, name));
    }

    @Deprecated
    enum DeprecatedAttribute implements org.jboss.as.clustering.controller.Attribute {
        SOCKET_BINDING("socket-binding", ModelType.STRING, JGroupsModel.VERSION_5_0_0), // socket-binding is now a required attribute of SocketBindingProtocolResourceDefinition
        ;
        private final AttributeDefinition definition;

        DeprecatedAttribute(String name, ModelType type, JGroupsModel deprecation) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setRequired(false)
                    .setDeprecated(deprecation.getVersion())
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    GenericProtocolResourceDefinition(Consumer<ResourceDescriptor> descriptorConfigurator, ResourceServiceBuilderFactory<ChannelFactory> parentBuilderFactory) {
        this(WILDCARD_PATH, descriptorConfigurator, parentBuilderFactory);
    }

    GenericProtocolResourceDefinition(String name, JGroupsModel deprecation, Consumer<ResourceDescriptor> descriptorConfigurator, ResourceServiceBuilderFactory<ChannelFactory> parentBuilderFactory) {
        this(pathElement(name), descriptorConfigurator, parentBuilderFactory);
        this.setDeprecated(deprecation.getVersion());
    }

    private GenericProtocolResourceDefinition(PathElement path, Consumer<ResourceDescriptor> descriptorConfigurator, ResourceServiceBuilderFactory<ChannelFactory> parentBuilderFactory) {
        super(path, descriptorConfigurator.andThen(descriptor -> descriptor
                .addExtraParameters(DeprecatedAttribute.class)
                ), ProtocolConfigurationBuilder::new, parentBuilderFactory, (parent, registration) -> {
                    for (org.jboss.as.clustering.controller.Attribute attribute : EnumSet.allOf(DeprecatedAttribute.class)) {
                        registration.registerReadOnlyAttribute(attribute.getDefinition(), null);
                    }
                });
    }
}
