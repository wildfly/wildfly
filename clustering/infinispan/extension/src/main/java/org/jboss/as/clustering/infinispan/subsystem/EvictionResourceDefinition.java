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
import org.jboss.as.clustering.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.clustering.controller.RemoveStepHandler;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleAliasEntry;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Resource description for the addressable resource /subsystem=infinispan/cache-container=X/cache=Y/eviction=EVICTION
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class EvictionResourceDefinition extends ComponentResourceDefinition {

    static final PathElement PATH = pathElement("eviction");
    static final PathElement LEGACY_PATH = PathElement.pathElement(PATH.getValue(), "EVICTION");

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        MAX_ENTRIES("max-entries", ModelType.LONG, new ModelNode(-1L), null),
        STRATEGY("strategy", ModelType.STRING, new ModelNode(EvictionStrategy.NONE.name()), new EnumValidator<>(EvictionStrategy.class, true, false)),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue, ParameterValidator validator) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setValidator(validator)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    private final boolean allowRuntimeOnlyRegistration;

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        if (InfinispanModel.VERSION_4_0_0.requiresTransformation(version)) {
            parent.addChildRedirection(PATH, LEGACY_PATH);
        }
    }

    EvictionResourceDefinition(boolean allowRuntimeOnlyRegistration) {
        super(PATH);
        this.allowRuntimeOnlyRegistration = allowRuntimeOnlyRegistration;
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registration) {
        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver()).addAttributes(Attribute.class);
        ResourceServiceHandler handler = new SimpleResourceServiceHandler<>(new EvictionBuilderFactory());
        new AddStepHandler(descriptor, handler).register(registration);
        new RemoveStepHandler(descriptor, handler).register(registration);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        new ReloadRequiredWriteAttributeHandler(Attribute.class).register(registration);

        if (this.allowRuntimeOnlyRegistration) {
            new MetricHandler<>(new EvictionMetricExecutor(), EvictionMetric.class).register(registration);
        }
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        registration.registerAlias(LEGACY_PATH, new SimpleAliasEntry(registration.registerSubModel(this)));
    }
}
