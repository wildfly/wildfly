/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.infinispan.Cache;
import org.jboss.as.clustering.controller.DurationAttributeDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * Registers a resource definition for a scattered cache.
 * @author Paul Ferraro
 */
public class ScatteredCacheResourceDefinitionRegistrar extends SegmentedCacheResourceDefinitionRegistrar {

    static final DurationAttributeDefinition BIAS_LIFESPAN = new DurationAttributeDefinition.Builder("bias-lifespan", ChronoUnit.MILLIS)
            .setDefaultValue(Duration.ofMinutes(5))
            .build();
    static final AttributeDefinition INVALIDATION_BATCH_SIZE = new SimpleAttributeDefinitionBuilder("invalidation-batch-size", ModelType.INT)
            .setAllowExpression(true)
            .setRequired(false)
            .setDefaultValue(new ModelNode(128))
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setValidator(new IntRangeValidator(0))
            .build();

    ScatteredCacheResourceDefinitionRegistrar(FunctionExecutorRegistry<Cache<?, ?>> executors) {
        super(CacheResourceRegistration.SCATTERED, executors);
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder)
                .addAttributes(List.of(BIAS_LIFESPAN, INVALIDATION_BATCH_SIZE))
                ;
    }
}
