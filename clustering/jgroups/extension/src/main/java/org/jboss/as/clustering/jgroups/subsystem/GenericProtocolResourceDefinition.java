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
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceConfiguratorFactory;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * @author Paul Ferraro
 */
public class GenericProtocolResourceDefinition extends ProtocolResourceDefinition {

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

    private static class ResourceDescriptorConfigurator implements UnaryOperator<ResourceDescriptor> {
        private final UnaryOperator<ResourceDescriptor> configurator;

        ResourceDescriptorConfigurator(UnaryOperator<ResourceDescriptor> configurator) {
            this.configurator = configurator;
        }

        @Override
        public ResourceDescriptor apply(ResourceDescriptor descriptor) {
            return this.configurator.apply(descriptor).addExtraParameters(DeprecatedAttribute.class);
        }
    }

    GenericProtocolResourceDefinition(UnaryOperator<ResourceDescriptor> configurator, ResourceServiceConfiguratorFactory parentServiceConfiguratorFactory) {
        this(WILDCARD_PATH, configurator, parentServiceConfiguratorFactory);
    }

    GenericProtocolResourceDefinition(String name, JGroupsModel deprecation, UnaryOperator<ResourceDescriptor> configurator, ResourceServiceConfiguratorFactory parentServiceConfiguratorFactory) {
        this(pathElement(name), configurator, parentServiceConfiguratorFactory);
        this.setDeprecated(deprecation.getVersion());
    }

    private GenericProtocolResourceDefinition(PathElement path, UnaryOperator<ResourceDescriptor> configurator, ResourceServiceConfiguratorFactory parentServiceConfiguratorFactory) {
        super(path, new ResourceDescriptorConfigurator(configurator), ProtocolConfigurationServiceConfigurator::new, parentServiceConfiguratorFactory);
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = super.register(parent);

        for (org.jboss.as.clustering.controller.Attribute attribute : EnumSet.allOf(DeprecatedAttribute.class)) {
            registration.registerReadOnlyAttribute(attribute.getDefinition(), null);
        }

        return registration;
    }
}
