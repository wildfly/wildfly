/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.function.UnaryOperator;

import org.infinispan.configuration.cache.StorageType;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.SimpleAliasEntry;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.EnumValidator;

/**
 * @author Paul Ferraro
 */
public class OffHeapMemoryResourceDefinition extends MemoryResourceDefinition {

    static final PathElement PATH = pathElement("off-heap");
    static final PathElement BINARY_PATH = pathElement("binary");

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        SIZE_UNIT(SharedAttribute.SIZE_UNIT) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setValidator(EnumValidator.create(MemorySizeUnit.class));
            }
        },
        ;
        private final AttributeDefinition definition;

        Attribute(org.jboss.as.clustering.controller.Attribute basis) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder((SimpleAttributeDefinition) basis.getDefinition())).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static class ResourceDescriptorConfigurator implements UnaryOperator<ResourceDescriptor> {
        @Override
        public ResourceDescriptor apply(ResourceDescriptor descriptor) {
            return descriptor.addAttributes(Attribute.class)
                    ;
        }
    }

    OffHeapMemoryResourceDefinition() {
        super(StorageType.OFF_HEAP, PATH, new ResourceDescriptorConfigurator(), Attribute.SIZE_UNIT);
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = super.register(parent);

        parent.registerAlias(BINARY_PATH, new SimpleAliasEntry(registration));

        return registration;
    }
}
