/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.EnumSet;
import java.util.function.UnaryOperator;

import org.infinispan.configuration.cache.StorageType;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.EnumValidator;

/**
 * Definition for the /memory=object resource.
 * @author Paul Ferraro
 */
public class HeapMemoryResourceDefinition extends MemoryResourceDefinition {

    static final PathElement PATH = pathElement("heap");

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        SIZE_UNIT(SharedAttribute.SIZE_UNIT) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setValidator(new EnumValidator<>(MemorySizeUnit.class, EnumSet.of(MemorySizeUnit.ENTRIES)));
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
            return descriptor.addAttributes(Attribute.class);
        }
    }

    HeapMemoryResourceDefinition() {
        super(StorageType.HEAP, PATH, new ResourceDescriptorConfigurator(), Attribute.SIZE_UNIT.getDefinition());
    }
}
