/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess.Flag;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.infinispan.service.InfinispanCacheRequirement;
import org.wildfly.clustering.infinispan.service.InfinispanDefaultCacheRequirement;

/**
 * Definition of the /subsystem=distributable-web/infinispan-session-management=* resource.
 * @author Paul Ferraro
 */
public class InfinispanSessionManagementResourceDefinition extends SessionManagementResourceDefinition {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);
    static PathElement pathElement(String name) {
        return PathElement.pathElement("infinispan-session-management", name);
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        CACHE_CONTAINER("cache-container", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setAllowExpression(false)
                        .setRequired(true)
                        .setCapabilityReference(new CapabilityReference(Capability.SESSION_MANAGEMENT_PROVIDER, InfinispanDefaultCacheRequirement.CONFIGURATION))
                        ;
            }
        },
        CACHE("cache", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setAllowExpression(false)
                        .setCapabilityReference(new CapabilityReference(Capability.SESSION_MANAGEMENT_PROVIDER, InfinispanCacheRequirement.CONFIGURATION, CACHE_CONTAINER))
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

    private static final UnaryOperator<ResourceDescriptor> CONFIGURATOR = new UnaryOperator<>() {
        @Override
        public ResourceDescriptor apply(ResourceDescriptor descriptor) {
            return descriptor.addAttributes(Attribute.class)
                    .addRequiredSingletonChildren(PrimaryOwnerAffinityResourceDefinition.PATH)
                    ;
        }
    };

    InfinispanSessionManagementResourceDefinition() {
        super(WILDCARD_PATH, CONFIGURATOR, InfinispanSessionManagementServiceConfigurator::new);
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = super.register(parent);

        new PrimaryOwnerAffinityResourceDefinition().register(registration);
        new RankedAffinityResourceDefinition().register(registration);

        return registration;
    }
}
