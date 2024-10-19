/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.ConnectionPoolConfiguration;
import org.infinispan.client.hotrod.configuration.ConnectionPoolConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.ExhaustedAction;
import org.jboss.as.clustering.controller.DurationAttributeDefinition;
import org.jboss.as.clustering.controller.EnumAttributeDefinition;
import org.jboss.as.clustering.infinispan.subsystem.ComponentResourceDescription;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.resource.AttributeDefinitionProvider;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Describes a connection pool component.
 * @author Paul Ferraro
 */
public enum ConnectionPoolResourceDescription implements RemoteCacheContainerComponentResourceDescription<ConnectionPoolConfiguration, ConnectionPoolConfigurationBuilder> {
    INSTANCE;

    private final PathElement path = ComponentResourceDescription.pathElement("connection-pool");
    private final UnaryServiceDescriptor<ConnectionPoolConfiguration> descriptor = RemoteCacheContainerComponentResourceDescription.createServiceDescriptor(this.path, ConnectionPoolConfiguration.class);
    private final RuntimeCapability<Void> capability = RuntimeCapability.Builder.of(this.descriptor).setDynamicNameMapper(UnaryCapabilityNameResolver.PARENT).build();

    public static final EnumAttributeDefinition<ExhaustedAction> EXHAUSTED_ACTION = new EnumAttributeDefinition.Builder<>("exhausted-action", ExhaustedAction.WAIT).build();
    public static final DurationAttributeDefinition MIN_EVICTABLE_IDLE = new DurationAttributeDefinition.Builder("min-evictable-idle-time", ChronoUnit.MILLIS).setDefaultValue(Duration.ofMinutes(30)).build();
    public static final DurationAttributeDefinition MAX_WAIT = new DurationAttributeDefinition.Builder("max-wait", ChronoUnit.MILLIS).setRequired(false).build();

    public enum Attribute implements AttributeDefinitionProvider {
        MAX_ACTIVE("max-active", ModelType.INT, null),
        MIN_IDLE("min-idle", ModelType.INT, new ModelNode(1)),
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
        public AttributeDefinition get() {
            return this.definition;
        }
    }

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    @Override
    public UnaryServiceDescriptor<ConnectionPoolConfiguration> getServiceDescriptor() {
        return this.descriptor;
    }

    @Override
    public RuntimeCapability<Void> getCapability() {
        return this.capability;
    }

    @Override
    public Stream<AttributeDefinition> getAttributes() {
        return Stream.concat(Stream.of(EXHAUSTED_ACTION, MIN_EVICTABLE_IDLE, MAX_WAIT), ResourceDescriptor.stream(EnumSet.allOf(Attribute.class)));
    }

    @Override
    public ServiceDependency<ConnectionPoolConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        ExhaustedAction exhaustedAction = EXHAUSTED_ACTION.resolve(context, model);
        int maxActive = Attribute.MAX_ACTIVE.resolveModelAttribute(context, model).asInt(-1);
        Duration maxWait = MAX_WAIT.resolve(context, model);
        Duration minEvictableIdle = MIN_EVICTABLE_IDLE.resolve(context, model);
        int minIdle = ConnectionPoolResourceDescription.Attribute.MIN_IDLE.resolveModelAttribute(context, model).asInt();

        return ServiceDependency.from(new Supplier<>() {
            @Override
            public ConnectionPoolConfigurationBuilder get() {
                return new ConfigurationBuilder().connectionPool()
                        .exhaustedAction(exhaustedAction)
                        .maxActive(maxActive)
                        .maxWait((maxWait != null) ? maxWait.toMillis() : -1L)
                        .minEvictableIdleTime(minEvictableIdle.toMillis())
                        .minIdle(minIdle)
                        ;
            }
        });
    }
}
