/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import org.infinispan.configuration.cache.CacheMode;
import org.jboss.as.clustering.controller.DurationAttributeDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Describes a scattered-cache resource.
 * @author Paul Ferraro
 */
@Deprecated
public enum ScatteredCacheResourceDescription implements SegmentedCacheResourceDescription {
    INSTANCE;

    static PathElement pathElement(String name) {
        return PathElement.pathElement("scattered-cache", name);
    }

    private final PathElement path = pathElement(PathElement.WILDCARD_VALUE);
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

    @Override
    public Stream<AttributeDefinition> getAttributes() {
        return Stream.concat(SegmentedCacheResourceDescription.super.getAttributes(), Stream.of(BIAS_LIFESPAN, INVALIDATION_BATCH_SIZE));
    }

    @Override
    public CacheMode getCacheMode() {
        return CacheMode.DIST_SYNC;
    }

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    @Override
    public InfinispanSubsystemModel getDeprecation() {
        return InfinispanSubsystemModel.VERSION_16_0_0;
    }
}
