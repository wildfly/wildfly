/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.EnumSet;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.infinispan.configuration.cache.ConfigurationBuilder;
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
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * @author Paul Ferraro
 */
public interface SegmentedCacheResourceDescription extends SharedStateCacheResourceDescription {

    enum Attribute implements AttributeDefinitionProvider {
        SEGMENTS("segments", ModelType.INT, new ModelNode(256), IntRangeValidator.POSITIVE),
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

    @Override
    default Stream<AttributeDefinition> getAttributes() {
        return Stream.concat(SharedStateCacheResourceDescription.super.getAttributes(), ResourceDescriptor.stream(EnumSet.allOf(Attribute.class)));
    }

    @Override
    default ServiceDependency<ConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        int segments = Attribute.SEGMENTS.resolveModelAttribute(context, model).asInt();
        return SharedStateCacheResourceDescription.super.resolve(context, model).map(new UnaryOperator<>() {
            @Override
            public ConfigurationBuilder apply(ConfigurationBuilder builder) {
                builder.clustering().hash().numSegments(segments);
                return builder;
            }
        });
    }
}
