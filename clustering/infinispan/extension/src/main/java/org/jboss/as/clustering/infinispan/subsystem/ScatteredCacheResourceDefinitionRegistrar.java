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
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * @author Paul Ferraro
 *
 */
public class ScatteredCacheResourceDefinitionRegistrar extends SegmentedCacheResourceDefinitionRegistrar {

    static final ResourceRegistration REGISTRATION = ResourceRegistration.of(PathElement.pathElement("scattered-cache"));

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
        super(new Configurator() {
            @Override
            public CacheResourceRegistration getResourceRegistration() {
                return CacheResourceRegistration.SCATTERED;
            }

            @Override
            public InfinispanSubsystemModel getDeprecation() {
                return InfinispanSubsystemModel.VERSION_16_0_0;
            }

            @Override
            public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
                return Configurator.super.apply(builder).addAttributes(List.of(BIAS_LIFESPAN, INVALIDATION_BATCH_SIZE));
            }
        }, executors);
    }
}
