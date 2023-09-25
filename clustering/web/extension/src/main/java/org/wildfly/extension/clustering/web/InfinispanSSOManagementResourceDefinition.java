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
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.infinispan.service.InfinispanCacheRequirement;
import org.wildfly.clustering.infinispan.service.InfinispanDefaultCacheRequirement;

/**
 * Definition of the /subsystem=distributable-web/infinispan-single-sign-on-management=* resource.
 * @author Paul Ferraro
 */
public class InfinispanSSOManagementResourceDefinition extends SSOManagementResourceDefinition {

    static final PathElement WILDCARD_PATH = PathElement.pathElement("infinispan-single-sign-on-management");

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        CACHE_CONTAINER("cache-container", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setAllowExpression(false)
                        .setRequired(true)
                        .setCapabilityReference(new CapabilityReference(Capability.SSO_MANAGEMENT_PROVIDER, InfinispanDefaultCacheRequirement.CONFIGURATION))
                        ;
            }
        },
        CACHE("cache", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setAllowExpression(false)
                        .setCapabilityReference(new CapabilityReference(Capability.SSO_MANAGEMENT_PROVIDER, InfinispanCacheRequirement.CONFIGURATION, CACHE_CONTAINER))
                        ;
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
    }

    InfinispanSSOManagementResourceDefinition() {
        super(WILDCARD_PATH, new SimpleResourceDescriptorConfigurator<>(Attribute.class), InfinispanSSOManagementServiceConfigurator::new);
    }
}
