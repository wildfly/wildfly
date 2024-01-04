/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.SimpleResourceDescriptorConfigurator;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess.Flag;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.infinispan.client.service.InfinispanClientRequirement;

/**
 * @author Paul Ferraro
 */
public class HotRodSessionManagementResourceDefinition extends SessionManagementResourceDefinition {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);
    static PathElement pathElement(String name) {
        return PathElement.pathElement("hotrod-session-management", name);
    }

    public enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        REMOTE_CACHE_CONTAINER("remote-cache-container", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setAllowExpression(false)
                        .setRequired(true)
                        .setCapabilityReference(new CapabilityReference(Capability.SESSION_MANAGEMENT_PROVIDER, InfinispanClientRequirement.REMOTE_CONTAINER))
                        ;
            }
        },
        CACHE_CONFIGURATION("cache-configuration", ModelType.STRING),
        EXPIRATION_THREAD_POOL_SIZE("expiration-thread-pool-size", ModelType.INT) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setDefaultValue(new ModelNode(16));
            }
        },
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setFlags(Flag.RESTART_RESOURCE_SERVICES)
                ).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }

        @Override
        public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
            return builder;
        }
    }

    HotRodSessionManagementResourceDefinition() {
        super(WILDCARD_PATH, new SimpleResourceDescriptorConfigurator<>(Attribute.class), HotRodSessionManagementServiceConfigurator::new);
    }
}
