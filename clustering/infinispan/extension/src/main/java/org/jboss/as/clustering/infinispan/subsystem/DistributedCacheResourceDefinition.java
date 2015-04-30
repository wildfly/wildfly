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

import org.jboss.as.clustering.controller.validation.DoubleRangeValidatorBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Resource description for the addressable resource /subsystem=infinispan/cache-container=X/distributed-cache=*
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @author Radoslav Husar
 */
public class DistributedCacheResourceDefinition extends SharedStateCacheResourceDefinition {

    // attributes
    static final SimpleAttributeDefinition L1_LIFESPAN = new SimpleAttributeDefinitionBuilder(ModelKeys.L1_LIFESPAN, ModelType.LONG, true)
            .setXmlName(Attribute.L1_LIFESPAN.getLocalName())
            .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode(600000L))
            .build();

    static final SimpleAttributeDefinition OWNERS = new SimpleAttributeDefinitionBuilder(ModelKeys.OWNERS, ModelType.INT, true)
            .setXmlName(Attribute.OWNERS.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode(2))
            .setValidator(new IntRangeValidator(1, true, true))
            .build();

    static final SimpleAttributeDefinition SEGMENTS = new SimpleAttributeDefinitionBuilder(ModelKeys.SEGMENTS, ModelType.INT, true)
            .setXmlName(Attribute.SEGMENTS.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode(80)) // Recommended value is 10 * max_cluster_size.
            .setValidator(new IntRangeValidator(1, true, true))
            .build();

    static final SimpleAttributeDefinition CAPACITY_FACTOR = new SimpleAttributeDefinitionBuilder(ModelKeys.CAPACITY_FACTOR, ModelType.DOUBLE, true)
            .setXmlName(Attribute.CAPACITY_FACTOR.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode(1.0f))
            .setValidator(new DoubleRangeValidatorBuilder().lowerBound(0).upperBound(Float.MAX_VALUE).build())
            .build();

    static final SimpleAttributeDefinition CONSISTENT_HASH_STRATEGY = new SimpleAttributeDefinitionBuilder(ModelKeys.CONSISTENT_HASH_STRATEGY, ModelType.STRING, true)
            .setXmlName(Attribute.CONSISTENT_HASH_STRATEGY.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode(ConsistentHashStrategy.DEFAULT.name()))
            .setValidator(new EnumValidator<>(ConsistentHashStrategy.class, true, true))
            .build();

    static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { OWNERS, SEGMENTS, L1_LIFESPAN, CAPACITY_FACTOR, CONSISTENT_HASH_STRATEGY };

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = parent.addChildResource(CacheType.DISTRIBUTED.pathElement());

        if (InfinispanModel.VERSION_3_0_0.requiresTransformation(version)) {
            builder.getAttributeBuilder()
                    .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(CAPACITY_FACTOR.getDefaultValue()), CAPACITY_FACTOR)
                    .addRejectCheck(RejectAttributeChecker.DEFINED, CAPACITY_FACTOR)
                    .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(CONSISTENT_HASH_STRATEGY.getDefaultValue()), CONSISTENT_HASH_STRATEGY)
                    .addRejectCheck(RejectAttributeChecker.DEFINED, CONSISTENT_HASH_STRATEGY)
                    .end();
        }

        SharedStateCacheResourceDefinition.buildTransformation(version, builder);
    }

    DistributedCacheResourceDefinition(PathManager pathManager, boolean allowRuntimeOnlyRegistration) {
        super(CacheType.DISTRIBUTED, pathManager, allowRuntimeOnlyRegistration);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        super.registerAttributes(registration);

        // check that we don't need a special handler here?
        final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attr : ATTRIBUTES) {
            registration.registerReadWriteAttribute(attr, null, writeHandler);
        }
    }
}
