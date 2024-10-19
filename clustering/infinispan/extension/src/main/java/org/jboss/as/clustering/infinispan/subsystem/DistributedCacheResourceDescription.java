/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.jboss.as.clustering.controller.DurationAttributeDefinition;
import org.jboss.as.clustering.controller.validation.DoubleRangeValidator;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.AttributeDefinitionProvider;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Describes a distributed cache resource.
 * @author Paul Ferraro
 */
public enum DistributedCacheResourceDescription implements SegmentedCacheResourceDescription {
    INSTANCE;

    static PathElement pathElement(String name) {
        return PathElement.pathElement("distributed-cache", name);
    }

    enum Attribute implements AttributeDefinitionProvider {
        CAPACITY_FACTOR("capacity-factor", ModelType.DOUBLE, new ModelNode(1.0f), DoubleRangeValidator.NON_NEGATIVE_FLOAT),
        OWNERS("owners", ModelType.INT, new ModelNode(2), new IntRangeValidator(1)),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue, ParameterValidator validator) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setDefaultValue(defaultValue)
                    .setValidator(validator)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition get() {
            return this.definition;
        }
    }

    private final PathElement path = pathElement(PathElement.WILDCARD_VALUE);

    static final DurationAttributeDefinition L1_LIFESPAN = new DurationAttributeDefinition.Builder("l1-lifespan", ChronoUnit.MILLIS).setDefaultValue(Duration.ZERO).build();

    @Override
    public CacheMode getCacheMode() {
        return CacheMode.DIST_SYNC;
    }

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    @Override
    public Stream<AttributeDefinition> getAttributes() {
        return Stream.concat(Stream.concat(ResourceDescriptor.stream(EnumSet.allOf(Attribute.class)), Stream.of(L1_LIFESPAN)), SegmentedCacheResourceDescription.super.getAttributes());
    }

    @Override
    public ServiceDependency<ConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        Duration l1Lifespan = L1_LIFESPAN.resolve(context, model);
        float capacityFactor = (float) Attribute.CAPACITY_FACTOR.resolveModelAttribute(context, model).asDouble();
        int owners = Attribute.OWNERS.resolveModelAttribute(context, model).asInt();

        return SegmentedCacheResourceDescription.super.resolve(context, model).map(new UnaryOperator<>() {
            @Override
            public ConfigurationBuilder apply(ConfigurationBuilder builder) {
                builder.clustering().hash()
                        .capacityFactor(capacityFactor)
                        .numOwners(owners)
                        .l1().enabled(!l1Lifespan.isZero()).lifespan(l1Lifespan.toMillis(), TimeUnit.MILLISECONDS)
                        ;
                return builder;
            }
        });
    }
}
