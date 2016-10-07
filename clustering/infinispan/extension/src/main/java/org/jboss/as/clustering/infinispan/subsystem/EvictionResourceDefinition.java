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

import org.infinispan.eviction.EvictionStrategy;
import org.jboss.as.clustering.controller.AddStepHandler;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.MetricHandler;
import org.jboss.as.clustering.controller.RemoveStepHandler;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleAliasEntry;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
import org.jboss.as.clustering.controller.transform.RequiredChildResourceDiscardPolicy;
import org.jboss.as.clustering.controller.validation.EnumValidatorBuilder;
import org.jboss.as.clustering.controller.validation.ParameterValidatorBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Resource description for the addressable resource and its alias:
 *
 * /subsystem=infinispan/cache-container=X/cache=Y/component=eviction
 * /subsystem=infinispan/cache-container=X/cache=Y/eviction=EVICTION
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class EvictionResourceDefinition extends ComponentResourceDefinition {

    static final PathElement PATH = pathElement("eviction");
    static final PathElement LEGACY_PATH = PathElement.pathElement(PATH.getValue(), "EVICTION");

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        STRATEGY("strategy", ModelType.STRING, new ModelNode(EvictionStrategy.NONE.name()), new EnumValidatorBuilder<>(EvictionStrategy.class)),
        MAX_ENTRIES("max-entries", ModelType.LONG, new ModelNode(-1L)),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = createBuilder(name, type, defaultValue).build();
        }

        Attribute(String name, ModelType type, ModelNode defaultValue, ParameterValidatorBuilder validator) {
            SimpleAttributeDefinitionBuilder builder = createBuilder(name, type, defaultValue);
            this.definition = builder.setValidator(validator.configure(builder).build()).build();
        }

        private static SimpleAttributeDefinitionBuilder createBuilder(String name, ModelType type, ModelNode defaultValue) {
            return new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    ;
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    private final boolean allowRuntimeOnlyRegistration;

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = InfinispanModel.VERSION_4_0_0.requiresTransformation(version) ? parent.addChildRedirection(PATH, LEGACY_PATH, RequiredChildResourceDiscardPolicy.NEVER) : parent.addChildResource(PATH);

        if (InfinispanModel.VERSION_4_0_0.requiresTransformation(version)) {
            builder.getAttributeBuilder().setValueConverter(new AttributeConverter.DefaultAttributeConverter() {
                @Override
                protected void convertAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
                    if (attributeValue.isDefined()) {
                        attributeValue.set(attributeValue.asInt());
                    }
                }
            }, Attribute.MAX_ENTRIES.getDefinition());
        }
    }

    EvictionResourceDefinition(boolean allowRuntimeOnlyRegistration) {
        super(PATH);
        this.allowRuntimeOnlyRegistration = allowRuntimeOnlyRegistration;
    }

    @Override
    public void register(ManagementResourceRegistration parentRegistration) {
        ManagementResourceRegistration registration = parentRegistration.registerSubModel(this);
        parentRegistration.registerAlias(LEGACY_PATH, new SimpleAliasEntry(registration));

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver()).addAttributes(Attribute.class);
        ResourceServiceHandler handler = new SimpleResourceServiceHandler<>(address -> new EvictionBuilder(address.getParent()));
        new AddStepHandler(descriptor, handler).register(registration);
        new RemoveStepHandler(descriptor, handler).register(registration);

        if (this.allowRuntimeOnlyRegistration) {
            new MetricHandler<>(new EvictionMetricExecutor(), EvictionMetric.class).register(registration);
        }
    }
}
