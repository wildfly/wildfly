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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleListAttributeDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Base class for store resources which require common store attributes only.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class StoreResourceDefinition extends SimpleResourceDefinition {

    // attributes
    static final SimpleAttributeDefinition FETCH_STATE = new SimpleAttributeDefinitionBuilder(ModelKeys.FETCH_STATE, ModelType.BOOLEAN, true)
            .setXmlName(Attribute.FETCH_STATE.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode().set(true))
            .build();

    static final SimpleAttributeDefinition PASSIVATION = new SimpleAttributeDefinitionBuilder(ModelKeys.PASSIVATION, ModelType.BOOLEAN, true)
            .setXmlName(Attribute.PASSIVATION.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode().set(true))
            .build();

    static final SimpleAttributeDefinition PRELOAD = new SimpleAttributeDefinitionBuilder(ModelKeys.PRELOAD, ModelType.BOOLEAN, true)
            .setXmlName(Attribute.PRELOAD.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode().set(false))
            .build();

    static final SimpleAttributeDefinition PURGE = new SimpleAttributeDefinitionBuilder(ModelKeys.PURGE, ModelType.BOOLEAN, true)
            .setXmlName(Attribute.PURGE.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode().set(true))
            .build();

    static final SimpleAttributeDefinition SHARED = new SimpleAttributeDefinitionBuilder(ModelKeys.SHARED, ModelType.BOOLEAN, true)
            .setXmlName(Attribute.SHARED.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode().set(false))
            .build();

    static final SimpleAttributeDefinition SINGLETON = new SimpleAttributeDefinitionBuilder(ModelKeys.SINGLETON, ModelType.BOOLEAN, true)
            .setXmlName(Attribute.SINGLETON.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode().set(false))
            .build();

    // used to pass in a list of properties to the store add command
    static final AttributeDefinition PROPERTY = new SimpleAttributeDefinition(ModelKeys.PROPERTY, ModelType.PROPERTY, true);
    static final SimpleListAttributeDefinition PROPERTIES = SimpleListAttributeDefinition.Builder.of(ModelKeys.PROPERTIES, PROPERTY)
            .setAllowNull(true)
            .build();

    static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] {
            SHARED, PRELOAD, PASSIVATION, FETCH_STATE, PURGE, SINGLETON
    };
    static final AttributeDefinition[] PARAMETERS = new AttributeDefinition[] {
            SHARED, PRELOAD, PASSIVATION, FETCH_STATE, PURGE, SINGLETON, PROPERTIES
    };

    // operations
    private static final OperationDefinition CACHE_STORE_ADD_DEFINITION = new SimpleOperationDefinitionBuilder(ADD, InfinispanExtension.getResourceDescriptionResolver(ModelKeys.STORE))
            .setParameters(PARAMETERS)
            .build();

    private final boolean allowRuntimeOnlyRegistration;

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder builder) {
        if (InfinispanModel.VERSION_1_4_0.requiresTransformation(version)) {
            builder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, FETCH_STATE, PASSIVATION, PRELOAD, PURGE, SHARED, SINGLETON);
        }

        StorePropertyResourceDefinition.buildTransformation(version, builder);
        StoreWriteBehindResourceDefinition.buildTransformation(version, builder);
    }

    StoreResourceDefinition(StoreType type, boolean allowRuntimeOnlyRegistration) {
        super(type.pathElement(), type.getResourceDescriptionResolver(), type.getAddHandler(), type.getRemoveHandler());
        this.allowRuntimeOnlyRegistration = allowRuntimeOnlyRegistration;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        // check that we don't need a special handler here?
        final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attr : ATTRIBUTES) {
            registration.registerReadWriteAttribute(attr, null, writeHandler);
        }

        if (this.allowRuntimeOnlyRegistration) {
            OperationStepHandler handler = new StoreMetricsHandler();
            for (StoreMetric metric: StoreMetric.values()) {
                registration.registerMetric(metric.getDefinition(), handler);
            }
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration registration) {
        // child resources
        registration.registerSubModel(new StoreWriteBehindResourceDefinition());
        registration.registerSubModel(new StorePropertyResourceDefinition());
    }

    // override the add operation to provide a custom definition (for the optional PROPERTIES parameter to add())
    @Override
    protected void registerAddOperation(final ManagementResourceRegistration registration, final OperationStepHandler handler, OperationEntry.Flag... flags) {
        registration.registerOperationHandler(CACHE_STORE_ADD_DEFINITION, handler);
    }
}
