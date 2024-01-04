/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.clustering.controller.ResourceServiceConfiguratorFactory;
import org.jboss.as.clustering.controller.SimpleResourceDescriptorConfigurator;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.jgroups.spi.RelayConfiguration;

/**
 * Resource definition for /subsystem=jgroups/stack=X/relay=RELAY
 *
 * @author Paul Ferraro
 */
public class RelayResourceDefinition extends AbstractProtocolResourceDefinition {

    static final PathElement PATH = pathElement(RelayConfiguration.PROTOCOL_NAME);
    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    public static PathElement pathElement(String name) {
        return PathElement.pathElement("relay", name);
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        SITE("site", ModelType.STRING),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    private final ResourceServiceConfiguratorFactory serviceConfiguratorFactory;

    RelayResourceDefinition(ResourceServiceConfiguratorFactory parentServiceConfiguratorFactory) {
        this(RelayConfigurationServiceConfigurator::new, parentServiceConfiguratorFactory);
    }

    private RelayResourceDefinition(ResourceServiceConfiguratorFactory serviceConfiguratorFactory, ResourceServiceConfiguratorFactory parentServiceConfiguratorFactory) {
        super(new Parameters(PATH, JGroupsExtension.SUBSYSTEM_RESOLVER.createChildResolver(WILDCARD_PATH, ProtocolResourceDefinition.WILDCARD_PATH)), new SimpleResourceDescriptorConfigurator<>(Attribute.class), serviceConfiguratorFactory, parentServiceConfiguratorFactory);
        this.serviceConfiguratorFactory = serviceConfiguratorFactory;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = super.register(parent);

        new RemoteSiteResourceDefinition(this.serviceConfiguratorFactory).register(registration);

        return registration;
    }
}
