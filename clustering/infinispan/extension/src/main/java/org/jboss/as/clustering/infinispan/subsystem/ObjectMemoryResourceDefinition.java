/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
import org.infinispan.eviction.EvictionStrategy;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.SimpleAliasEntry;
import org.jboss.as.clustering.controller.transform.RequiredChildResourceDiscardPolicy;
import org.jboss.as.clustering.controller.transform.SimpleAttributeConverter;
import org.jboss.as.clustering.controller.validation.EnumValidator;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Definition for the /memory=object resource.
 * @author Paul Ferraro
 */
public class ObjectMemoryResourceDefinition extends MemoryResourceDefinition {

    static final PathElement PATH = pathElement("object");
    static final PathElement EVICTION_PATH = ComponentResourceDefinition.pathElement("eviction");
    static final PathElement LEGACY_PATH = PathElement.pathElement(EVICTION_PATH.getValue(), "EVICTION");

    enum DeprecatedAttribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        STRATEGY("strategy", ModelType.STRING, new ModelNode(EvictionStrategy.NONE.name())) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setValidator(new EnumValidator<>(EvictionStrategy.class, EnumSet.complementOf(EnumSet.of(EvictionStrategy.MANUAL))));
            }
        },
        MAX_ENTRIES("max-entries", ModelType.LONG, new ModelNode(-1L)),
        ;
        private final AttributeDefinition definition;

        DeprecatedAttribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setDeprecated(InfinispanModel.VERSION_6_0_0.getVersion())
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

    @SuppressWarnings("deprecation")
    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = InfinispanModel.VERSION_4_0_0.requiresTransformation(version) ? parent.addChildRedirection(PATH, LEGACY_PATH, RequiredChildResourceDiscardPolicy.NEVER) : InfinispanModel.VERSION_6_0_0.requiresTransformation(version) ? parent.addChildRedirection(PATH, EVICTION_PATH, RequiredChildResourceDiscardPolicy.NEVER) : parent.addChildResource(PATH);

        if (InfinispanModel.VERSION_6_0_0.requiresTransformation(version)) {
            builder.getAttributeBuilder()
                .addRename(Attribute.SIZE.getDefinition(), DeprecatedAttribute.MAX_ENTRIES.getName())
                .setValueConverter(new SimpleAttributeConverter((address, name, value, model, context) -> {
                    // Set legacy eviction strategy to NONE if size is negative, otherwise set to LRU
                    if (model.hasDefined(Attribute.SIZE.getName()) && (model.get(Attribute.SIZE.getName()).asLong() < 0)) {
                        value.set(EvictionStrategy.NONE.name());
                    } else {
                        value.set(EvictionStrategy.LRU.name());
                    }
                }), DeprecatedAttribute.STRATEGY.getDefinition())
                .end();
        }

        if (InfinispanModel.VERSION_4_0_0.requiresTransformation(version)) {
            builder.getAttributeBuilder().setValueConverter(new SimpleAttributeConverter((address, name, value, model, context) -> {
                if (value.isDefined()) {
                    value.set(value.asInt());
                }
            }), Attribute.SIZE.getDefinition(), DeprecatedAttribute.MAX_ENTRIES.getDefinition());
        }
    }

    static class ResourceDescriptorConfigurator implements UnaryOperator<ResourceDescriptor> {
        @Override
        public ResourceDescriptor apply(ResourceDescriptor descriptor) {
            return descriptor.addAttributes(EnumSet.complementOf(EnumSet.of(DeprecatedAttribute.MAX_ENTRIES)))
                    .addAlias(DeprecatedAttribute.MAX_ENTRIES, Attribute.SIZE)
                    ;
        }
    }

    ObjectMemoryResourceDefinition() {
        super(StorageType.OBJECT, PATH, new ResourceDescriptorConfigurator());
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = super.register(parent);

        parent.registerAlias(EVICTION_PATH, new SimpleAliasEntry(registration));
        parent.registerAlias(LEGACY_PATH, new SimpleAliasEntry(registration));

        return registration;
    }
}
