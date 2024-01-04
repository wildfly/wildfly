/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.Set;
import java.util.function.UnaryOperator;

import org.infinispan.persistence.jdbc.common.DatabaseType;
import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelType;

/**
 * Base class for store resources which require common store attributes and JDBC store attributes
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class JDBCStoreResourceDefinition extends StoreResourceDefinition {

    static final PathElement PATH = pathElement("jdbc");

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        DATA_SOURCE("data-source", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setRequired(true)
                        .setCapabilityReference(new CapabilityReference(Capability.PERSISTENCE, CommonUnaryRequirement.DATA_SOURCE))
                        ;
            }
        },
        DIALECT("dialect", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setAllowExpression(true)
                        .setRequired(false)
                        .setValidator(EnumValidator.create(DatabaseType.class))
                        ;
            }
        },
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES))
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static final Set<PathElement> REQUIRED_CHILDREN = Set.of(StringTableResourceDefinition.PATH);

    static class ResourceDescriptorConfigurator implements UnaryOperator<ResourceDescriptor> {

        @Override
        public ResourceDescriptor apply(ResourceDescriptor descriptor) {
            return descriptor.addAttributes(Attribute.class)
                    .addRequiredChildren(REQUIRED_CHILDREN)
                    ;
        }
    }

    JDBCStoreResourceDefinition() {
        super(PATH, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(PATH, WILDCARD_PATH), new ResourceDescriptorConfigurator(), JDBCStoreServiceConfigurator::new);
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = super.register(parent);

        new StringTableResourceDefinition().register(registration);

        return registration;
    }
}
