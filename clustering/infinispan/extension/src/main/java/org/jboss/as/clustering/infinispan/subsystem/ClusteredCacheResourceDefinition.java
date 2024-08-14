/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.ClusteredCacheResourceDefinition.Attribute.REMOTE_TIMEOUT;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.server.util.MapEntry;
import org.wildfly.subsystem.resource.capability.ResourceCapabilityReference;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * Base class for cache resources which require common cache attributes and clustered cache attributes.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class ClusteredCacheResourceDefinition extends CacheResourceDefinition {

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        REMOTE_TIMEOUT("remote-timeout", ModelType.LONG, new ModelNode(TimeUnit.MILLISECONDS.toMillis(17500))) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setMeasurementUnit(MeasurementUnit.MILLISECONDS);
            }
        },
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    private static class ResourceDescriptorConfigurator implements UnaryOperator<ResourceDescriptor> {
        private final UnaryOperator<ResourceDescriptor> configurator;

        ResourceDescriptorConfigurator(UnaryOperator<ResourceDescriptor> configurator) {
            this.configurator = configurator;
        }

        @Override
        public ResourceDescriptor apply(ResourceDescriptor descriptor) {
            return this.configurator.apply(descriptor)
                    .addAttributes(Attribute.class)
                    .addResourceCapabilityReference(ResourceCapabilityReference.builder(CACHE_CONFIGURATION_CAPABILITY, JGroupsTransportResourceDefinition.SERVICE_DESCRIPTOR).withRequirementNameResolver(UnaryCapabilityNameResolver.PARENT).build())
                    ;
        }
    }

    ClusteredCacheResourceDefinition(PathElement path, UnaryOperator<ResourceDescriptor> configurator, CacheMode mode, FunctionExecutorRegistry<Cache<?, ?>> executors) {
        super(path, new ResourceDescriptorConfigurator(configurator), mode, executors);
    }

    @Override
    public MapEntry<Consumer<ConfigurationBuilder>, Stream<Consumer<RequirementServiceBuilder<?>>>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        long remoteTimeout = REMOTE_TIMEOUT.resolveModelAttribute(context, model).asLong();

        return super.resolve(context, model).map(consumer -> consumer.andThen(new Consumer<>() {
            @Override
            public void accept(ConfigurationBuilder builder) {
                builder.clustering().remoteTimeout(remoteTimeout, TimeUnit.MILLISECONDS);
            }
        }), Function.identity());
    }
}
