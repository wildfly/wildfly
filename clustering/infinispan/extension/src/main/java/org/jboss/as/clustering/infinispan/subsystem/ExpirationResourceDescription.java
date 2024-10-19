/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.ExpirationConfiguration;
import org.infinispan.configuration.cache.ExpirationConfigurationBuilder;
import org.jboss.as.clustering.controller.DurationAttributeDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.dmr.ModelNode;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.subsystem.resource.AttributeDefinitionProvider;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers an expiration cache component resource definition.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @author Paul Ferraro
 */
public enum ExpirationResourceDescription implements CacheComponentResourceDescription<ExpirationConfiguration, ExpirationConfigurationBuilder> {
    INSTANCE;

    private final PathElement path = ComponentResourceDescription.pathElement("expiration");
    private final BinaryServiceDescriptor<ExpirationConfiguration> descriptor = CacheComponentResourceDescription.createServiceDescriptor(this.path, ExpirationConfiguration.class);
    private final RuntimeCapability<Void> capability = RuntimeCapability.Builder.of(this.descriptor).setDynamicNameMapper(BinaryCapabilityNameResolver.GRANDPARENT_PARENT).setAllowMultipleRegistrations(true).build();

    enum Attribute implements AttributeDefinitionProvider, ResourceModelResolver<Duration> {
        INTERVAL("interval", Duration.ofMinutes(1)),
        LIFESPAN("lifespan", null),
        MAX_IDLE("max-idle", null),
        ;
        private final DurationAttributeDefinition definition;

        Attribute(String name, Duration defaultValue) {
            this.definition = new DurationAttributeDefinition.Builder(name, ChronoUnit.MILLIS)
                    .setRequired(false)
                    .setDefaultValue(defaultValue)
                    .build();
        }

        @Override
        public AttributeDefinition get() {
            return this.definition;
        }

        @Override
        public Duration resolve(OperationContext context, ModelNode model) throws OperationFailedException {
            return this.definition.resolve(context, model);
        }
    }

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    @Override
    public BinaryServiceDescriptor<ExpirationConfiguration> getServiceDescriptor() {
        return this.descriptor;
    }

    @Override
    public RuntimeCapability<Void> getCapability() {
        return this.capability;
    }

    @Override
    public Stream<AttributeDefinition> getAttributes() {
        return ResourceDescriptor.stream(EnumSet.allOf(Attribute.class));
    }

    @Override
    public ServiceDependency<ExpirationConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        Duration interval = Attribute.INTERVAL.resolve(context, model);
        Duration lifespan = Attribute.LIFESPAN.resolve(context, model);
        Duration maxIdle = Attribute.MAX_IDLE.resolve(context, model);
        return ServiceDependency.from(new Supplier<>() {
            @Override
            public ExpirationConfigurationBuilder get() {
                return new ConfigurationBuilder().expiration()
                        .lifespan((lifespan != null) ? lifespan.toMillis() : -1, TimeUnit.MILLISECONDS)
                        .maxIdle((maxIdle != null) ? maxIdle.toMillis() : -1, TimeUnit.MILLISECONDS)
                        .reaperEnabled(!interval.isZero())
                        .wakeUpInterval(interval.toMillis(), TimeUnit.MILLISECONDS);
            }
        });
    }
}
