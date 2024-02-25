/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;

import org.infinispan.Cache;
import org.jboss.as.clustering.controller.SimpleResourceDescriptorConfigurator;
import org.jboss.as.clustering.controller.validation.IntRangeValidatorBuilder;
import org.jboss.as.clustering.controller.validation.LongRangeValidatorBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * Resource definition for a scattered-cache.
 * @author Paul Ferraro
 */
@Deprecated
public class ScatteredCacheResourceDefinition extends SegmentedCacheResourceDefinition {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    static PathElement pathElement(String name) {
        return PathElement.pathElement("scattered-cache", name);
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        BIAS_LIFESPAN("bias-lifespan", ModelType.LONG, new ModelNode(TimeUnit.MINUTES.toMillis(5))) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setValidator(new LongRangeValidatorBuilder().min(0).configure(builder).build())
                        .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
                        ;
            }
        },
        INVALIDATION_BATCH_SIZE("invalidation-batch-size", ModelType.INT, new ModelNode(128)) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setValidator(new IntRangeValidatorBuilder().min(0).configure(builder).build())
                        ;
            }
        },
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    ).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    ScatteredCacheResourceDefinition(FunctionExecutorRegistry<Cache<?, ?>> executors) {
        super(WILDCARD_PATH, new SimpleResourceDescriptorConfigurator<>(Attribute.class), new ClusteredCacheServiceHandler(ScatteredCacheServiceConfigurator::new), executors);
        this.setDeprecated(InfinispanSubsystemModel.VERSION_16_0_0.getVersion());
    }
}
