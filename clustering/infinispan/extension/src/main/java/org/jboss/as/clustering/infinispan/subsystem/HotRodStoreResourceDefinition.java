/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.SimpleResourceDescriptorConfigurator;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.infinispan.client.service.InfinispanClientRequirement;

/**
 * Resource description for the addressable resource:
 *
 * /subsystem=infinispan/cache-container=X/cache=Y/store=hotrod
 *
 * @author Radoslav Husar
 */
public class HotRodStoreResourceDefinition extends StoreResourceDefinition {

    static final PathElement PATH = pathElement("hotrod");

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        CACHE_CONFIGURATION("cache-configuration", ModelType.STRING, null),
        REMOTE_CACHE_CONTAINER("remote-cache-container", ModelType.STRING, new CapabilityReference(Capability.PERSISTENCE, InfinispanClientRequirement.REMOTE_CONTAINER)),
        ;

        private final AttributeDefinition definition;

        Attribute(String attributeName, ModelType type, CapabilityReference capabilityReference) {
            this.definition = new SimpleAttributeDefinitionBuilder(attributeName, type)
                    .setAllowExpression(capabilityReference == null)
                    .setRequired(capabilityReference != null)
                    .setCapabilityReference(capabilityReference)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    HotRodStoreResourceDefinition() {
        super(PATH, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(PATH, WILDCARD_PATH), new SimpleResourceDescriptorConfigurator<>(Attribute.class), HotRodStoreServiceConfigurator::new);
    }
}
