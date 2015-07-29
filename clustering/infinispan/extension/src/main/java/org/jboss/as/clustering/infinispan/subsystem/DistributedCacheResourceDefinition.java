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

import org.jboss.as.clustering.controller.AddStepHandler;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.clustering.controller.RemoveStepHandler;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.validation.DoubleRangeValidatorBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
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

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);
    static PathElement pathElement(String name) {
        return PathElement.pathElement("distributed-cache", name);
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        CAPACITY_FACTOR("capacity-factor", ModelType.DOUBLE, new ModelNode(1.0f), new DoubleRangeValidatorBuilder().lowerBound(0).upperBound(Float.MAX_VALUE).build()),
        CONSISTENT_HASH_STRATEGY("consistent-hash-strategy", ModelType.STRING, new ModelNode(ConsistentHashStrategy.INTRA_CACHE.name()), new EnumValidator<>(ConsistentHashStrategy.class, true, true)),
        L1_LIFESPAN("l1-lifespan", ModelType.LONG, new ModelNode(600000L), new DoubleRangeValidatorBuilder().lowerBound(0).build()),
        OWNERS("owners", ModelType.INT, new ModelNode(2), new IntRangeValidator(1, true, true)),
        SEGMENTS("segments", ModelType.INT, new ModelNode(80), new IntRangeValidator(1, true, true)),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue, ParameterValidator validator) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setMeasurementUnit((type == ModelType.LONG) ? MeasurementUnit.MILLISECONDS : null)
                    .setValidator(validator)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = parent.addChildResource(WILDCARD_PATH);

        if (InfinispanModel.VERSION_3_0_0.requiresTransformation(version)) {
            builder.getAttributeBuilder()
                    .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(Attribute.CAPACITY_FACTOR.getDefinition().getDefaultValue()), Attribute.CAPACITY_FACTOR.getDefinition())
                    .addRejectCheck(RejectAttributeChecker.DEFINED, Attribute.CAPACITY_FACTOR.getDefinition())
                    .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(Attribute.CONSISTENT_HASH_STRATEGY.getDefinition().getDefaultValue()), Attribute.CONSISTENT_HASH_STRATEGY.getDefinition())
                    .addRejectCheck(RejectAttributeChecker.DEFINED, Attribute.CONSISTENT_HASH_STRATEGY.getDefinition())
                    .end();
        }

        SharedStateCacheResourceDefinition.buildTransformation(version, builder);
    }

    DistributedCacheResourceDefinition(PathManager pathManager, boolean allowRuntimeOnlyRegistration) {
        super(WILDCARD_PATH, pathManager, allowRuntimeOnlyRegistration);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        super.registerAttributes(registration);

        new ReloadRequiredWriteAttributeHandler(Attribute.class).register(registration);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registration) {
        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver()).addAttributes(Attribute.class).addAttributes(ClusteredCacheResourceDefinition.Attribute.class).addAttributes(CacheResourceDefinition.Attribute.class);
        ResourceServiceHandler handler = new DistributedCacheServiceHandler();
        new AddStepHandler(descriptor, handler).register(registration);
        new RemoveStepHandler(descriptor, handler).register(registration);
    }
}
