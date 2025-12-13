/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.List;
import java.util.function.UnaryOperator;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * Registers a resource definition for a segmented cache configuration.
 * @author Paul Ferraro
 */
public class SegmentedCacheResourceDefinitionRegistrar extends SharedStateCacheResourceDefinitionRegistrar {

    static final AttributeDefinition SEGMENTS = new SimpleAttributeDefinitionBuilder("segments", ModelType.INT)
            .setAllowExpression(true)
            .setRequired(false)
            .setDefaultValue(new ModelNode(128))
            .setValidator(IntRangeValidator.POSITIVE)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    SegmentedCacheResourceDefinitionRegistrar(CacheResourceRegistration registration, FunctionExecutorRegistry<Cache<?, ?>> executors) {
        super(registration, executors);
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder).addAttributes(List.of(SEGMENTS));
    }

    @Override
    public ServiceDependency<ConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        int segments = SEGMENTS.resolveModelAttribute(context, model).asInt();
        return super.resolve(context, model).map(new UnaryOperator<>() {
            @Override
            public ConfigurationBuilder apply(ConfigurationBuilder builder) {
                builder.clustering().hash().numSegments(segments);
                return builder;
            }
        });
    }
}
