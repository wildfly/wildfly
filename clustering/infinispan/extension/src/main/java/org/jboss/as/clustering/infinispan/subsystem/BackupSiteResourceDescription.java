/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.infinispan.configuration.cache.BackupConfiguration;
import org.infinispan.configuration.cache.BackupConfiguration.BackupStrategy;
import org.infinispan.configuration.cache.BackupConfigurationBuilder;
import org.infinispan.configuration.cache.BackupFailurePolicy;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.jboss.as.clustering.controller.DurationAttributeDefinition;
import org.jboss.as.clustering.controller.EnumAttributeDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.TernaryCapabilityNameResolver;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.service.descriptor.TernaryServiceDescriptor;
import org.wildfly.subsystem.resource.AttributeDefinitionProvider;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Describes a backup site resource.
 * @author Paul Ferraro
 */
public enum BackupSiteResourceDescription implements ComponentResourceDescription<BackupConfiguration, BackupConfigurationBuilder> {
    INSTANCE;

    static PathElement pathElement(String name) {
        return PathElement.pathElement("backup", name);
    }

    private final PathElement path = pathElement(PathElement.WILDCARD_VALUE);
    private final TernaryServiceDescriptor<BackupConfiguration> descriptor = TernaryServiceDescriptor.of(String.join(".", InfinispanServiceDescriptor.CACHE_CONFIGURATION.getName(), this.path.getKey()), BackupConfiguration.class);
    private final RuntimeCapability<Void> capability = RuntimeCapability.Builder.of(this.descriptor).setDynamicNameMapper(TernaryCapabilityNameResolver.GREATGRANDPARENT_GRANDPARENT_CHILD).setAllowMultipleRegistrations(true).build();

    static final EnumAttributeDefinition<BackupFailurePolicy> FAILURE_POLICY = new EnumAttributeDefinition.Builder<>("failure-policy", BackupFailurePolicy.WARN).build();
    static final EnumAttributeDefinition<BackupStrategy> STRATEGY = new EnumAttributeDefinition.Builder<>("strategy", BackupStrategy.ASYNC).build();
    static final DurationAttributeDefinition TIMEOUT = new DurationAttributeDefinition.Builder("timeout", ChronoUnit.MILLIS).setDefaultValue(Duration.ofSeconds(10)).build();

    enum TakeOfflineAttribute implements AttributeDefinitionProvider {
        AFTER_FAILURES("after-failures", ModelType.INT, new ModelNode(1)),
        MIN_WAIT("min-wait", ModelType.LONG, ModelNode.ZERO_LONG),
        ;
        private final AttributeDefinition definition;

        TakeOfflineAttribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAttributeGroup("take-offline")
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setMeasurementUnit((type == ModelType.LONG) ? MeasurementUnit.MILLISECONDS : null)
                    .build();
        }

        @Override
        public AttributeDefinition get() {
            return this.definition;
        }
    }

    enum DeprecatedAttribute implements AttributeDefinitionProvider {
        ENABLED("enabled", ModelType.BOOLEAN, ModelNode.TRUE, InfinispanSubsystemModel.VERSION_16_0_0),
        ;
        private final AttributeDefinition definition;

        DeprecatedAttribute(String name, ModelType type, ModelNode defaultValue, InfinispanSubsystemModel deprecation) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setDefaultValue(defaultValue)
                    .setDeprecated(deprecation.getVersion())
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
    public Stream<AttributeDefinition> getAttributes() {
        return Stream.concat(Stream.of(FAILURE_POLICY, STRATEGY, TIMEOUT), Stream.concat(ResourceDescriptor.stream(EnumSet.allOf(TakeOfflineAttribute.class)), ResourceDescriptor.stream(EnumSet.allOf(DeprecatedAttribute.class))));
    }

    @Override
    public TernaryServiceDescriptor<BackupConfiguration> getServiceDescriptor() {
        return this.descriptor;
    }

    @Override
    public RuntimeCapability<Void> getCapability() {
        return this.capability;
    }

    @Override
    public ServiceDependency<BackupConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        String siteName = context.getCurrentAddressValue();
        BackupFailurePolicy failurePolicy = FAILURE_POLICY.resolve(context, model);
        BackupStrategy strategy = STRATEGY.resolve(context, model);
        Duration timeout = TIMEOUT.resolve(context, model);
        int afterFailures = TakeOfflineAttribute.AFTER_FAILURES.resolveModelAttribute(context, model).asInt();
        long minWait = TakeOfflineAttribute.MIN_WAIT.resolveModelAttribute(context, model).asLong();
        return ServiceDependency.from(new Supplier<>() {
            @Override
            public BackupConfigurationBuilder get() {
                return new ConfigurationBuilder().sites().addBackup()
                        .site(siteName)
                        .backupFailurePolicy(failurePolicy)
                        .replicationTimeout(timeout.toMillis())
                        .strategy(strategy)
                        .takeOffline()
                            .afterFailures(afterFailures)
                            .minTimeToWait(minWait)
                        .backup();
            }
        });
    }
}
