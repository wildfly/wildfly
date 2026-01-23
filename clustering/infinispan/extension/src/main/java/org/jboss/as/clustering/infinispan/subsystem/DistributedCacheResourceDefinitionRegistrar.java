/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.jboss.as.clustering.controller.validation.DoubleRangeValidator;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.AttributeDefinitionProvider;
import org.wildfly.subsystem.resource.DurationAttributeDefinition;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * Registers a resource definition for a distributed cache.
 * @author Paul Ferraro
 */
public class DistributedCacheResourceDefinitionRegistrar extends SegmentedCacheResourceDefinitionRegistrar {

    static final DurationAttributeDefinition L1_LIFESPAN = DurationAttributeDefinition.builder("l1-lifespan", ChronoUnit.MILLIS).setDefaultValue(Duration.ZERO).build();

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

    DistributedCacheResourceDefinitionRegistrar(FunctionExecutorRegistry<Cache<?, ?>> executors) {
        super(CacheResourceRegistration.DISTRIBUTED, executors);
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder)
                .addAttributes(List.of(L1_LIFESPAN))
                .provideAttributes(EnumSet.allOf(Attribute.class));
    }

    @Override
    public ServiceDependency<ConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        Duration l1Lifespan = L1_LIFESPAN.resolve(context, model);
        float capacityFactor = (float) Attribute.CAPACITY_FACTOR.resolveModelAttribute(context, model).asDouble();
        int owners = Attribute.OWNERS.resolveModelAttribute(context, model).asInt();

        return super.resolve(context, model).map(new UnaryOperator<>() {
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
