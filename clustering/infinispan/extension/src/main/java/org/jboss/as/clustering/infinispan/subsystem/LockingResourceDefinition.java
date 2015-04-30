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

import org.infinispan.util.concurrent.IsolationLevel;
import org.jboss.as.clustering.controller.MetricHandler;
import org.jboss.as.clustering.controller.ReloadRequiredAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Resource description for the addressable resource /subsystem=infinispan/cache-container=X/cache=Y/locking=LOCKING
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class LockingResourceDefinition extends SimpleResourceDefinition {

    static final PathElement PATH = PathElement.pathElement(ModelKeys.LOCKING, ModelKeys.LOCKING_NAME);

    // attributes
    static final SimpleAttributeDefinition ACQUIRE_TIMEOUT = new SimpleAttributeDefinitionBuilder(ModelKeys.ACQUIRE_TIMEOUT, ModelType.LONG, true)
            .setXmlName(Attribute.ACQUIRE_TIMEOUT.getLocalName())
            .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode().set(15000L))
            .build();

    static final SimpleAttributeDefinition CONCURRENCY_LEVEL = new SimpleAttributeDefinitionBuilder(ModelKeys.CONCURRENCY_LEVEL, ModelType.INT, true)
            .setXmlName(Attribute.CONCURRENCY_LEVEL.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode().set(1000))
            .build();

    static final SimpleAttributeDefinition ISOLATION = new SimpleAttributeDefinitionBuilder(ModelKeys.ISOLATION, ModelType.STRING, true)
            .setXmlName(Attribute.ISOLATION.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new EnumValidator<>(IsolationLevel.class, true, false))
            .setDefaultValue(new ModelNode().set(IsolationLevel.REPEATABLE_READ.name()))
            .build();

    static final SimpleAttributeDefinition STRIPING = new SimpleAttributeDefinitionBuilder(ModelKeys.STRIPING, ModelType.BOOLEAN, true)
            .setXmlName(Attribute.STRIPING.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode().set(false))
            .build();

    static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { ACQUIRE_TIMEOUT, CONCURRENCY_LEVEL, ISOLATION, STRIPING };

    private final boolean allowRuntimeOnlyRegistration;

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        // Do nothing
    }

    LockingResourceDefinition(boolean allowRuntimeOnlyRegistration) {
        super(PATH, new InfinispanResourceDescriptionResolver(ModelKeys.LOCKING), new ReloadRequiredAddStepHandler(ATTRIBUTES), ReloadRequiredRemoveStepHandler.INSTANCE);
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
            new MetricHandler<>(new LockingMetricExecutor(), LockingMetric.class).register(registration);
        }
    }
}
