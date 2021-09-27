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

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.EnumSet;
import java.util.function.UnaryOperator;

import org.infinispan.configuration.cache.StorageType;
import org.jboss.as.clustering.controller.AttributeTranslation;
import org.jboss.as.clustering.controller.AttributeValueTranslator;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.SimpleAliasEntry;
import org.jboss.as.clustering.controller.validation.EnumValidator;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

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
                return builder.setValidator(new EnumValidator<>(MemorySizeUnit.class));
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

    @Deprecated
    enum DeprecatedAttribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        CAPACITY("capacity", ModelType.INT, new ModelNode(1048576), InfinispanModel.VERSION_12_0_0),
        EVICTION_TYPE("eviction-type", ModelType.STRING, new ModelNode(org.infinispan.eviction.EvictionType.COUNT.name()), InfinispanModel.VERSION_13_0_0) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setValidator(new EnumValidator<>(org.infinispan.eviction.EvictionType.class));
            }
        },
        ;
        private final AttributeDefinition definition;

        DeprecatedAttribute(String name, ModelType type, ModelNode defaultValue, InfinispanModel deprecation) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setDefaultValue(defaultValue)
                    .setDeprecated(deprecation.getVersion())
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();
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

    @SuppressWarnings("deprecation")
    enum EvictionTypeTranslator implements AttributeTranslation {
        INSTANCE;

        private final AttributeValueTranslator readTranslator = new AttributeValueTranslator() {
            @Override
            public ModelNode translate(OperationContext context, ModelNode value) throws OperationFailedException {
                if (!value.isDefined()) return value;
                MemorySizeUnit unit = MemorySizeUnit.valueOf(Attribute.SIZE_UNIT.getDefinition().resolveValue(context, value).asString());
                return new ModelNode(((unit == MemorySizeUnit.ENTRIES) ? org.infinispan.eviction.EvictionType.COUNT : org.infinispan.eviction.EvictionType.MEMORY).name());
            }
        };
        private final AttributeValueTranslator writeTranslator = new AttributeValueTranslator() {
            @Override
            public ModelNode translate(OperationContext context, ModelNode value) throws OperationFailedException {
                if (!value.isDefined()) return value;
                org.infinispan.eviction.EvictionType type = org.infinispan.eviction.EvictionType.valueOf(DeprecatedAttribute.EVICTION_TYPE.getDefinition().resolveValue(context, value).asString());
                return new ModelNode(((type == org.infinispan.eviction.EvictionType.COUNT) ? MemorySizeUnit.ENTRIES : MemorySizeUnit.BYTES).name());
            }
        };

        @Override
        public AttributeValueTranslator getReadTranslator() {
            return this.readTranslator;
        }

        @Override
        public AttributeValueTranslator getWriteTranslator() {
            return this.writeTranslator;
        }

        @Override
        public org.jboss.as.clustering.controller.Attribute getTargetAttribute() {
            return Attribute.SIZE_UNIT;
        }
    }

    static class ResourceDescriptorConfigurator implements UnaryOperator<ResourceDescriptor> {
        @Override
        public ResourceDescriptor apply(ResourceDescriptor descriptor) {
            return descriptor.addAttributes(Attribute.class)
                    .addAttributeTranslation(DeprecatedAttribute.EVICTION_TYPE, EvictionTypeTranslator.INSTANCE)
                    .addIgnoredAttributes(EnumSet.complementOf(EnumSet.of(DeprecatedAttribute.EVICTION_TYPE)))
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
