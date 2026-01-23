/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Supplier;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.BackupConfiguration;
import org.infinispan.configuration.cache.BackupConfiguration.BackupStrategy;
import org.infinispan.configuration.cache.BackupConfigurationBuilder;
import org.infinispan.configuration.cache.BackupFailurePolicy;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.jboss.as.clustering.controller.EnumAttributeDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.TernaryCapabilityNameResolver;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.service.descriptor.TernaryServiceDescriptor;
import org.wildfly.subsystem.resource.DurationAttributeDefinition;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.executor.RuntimeOperationStepHandler;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * Definition of a backup site resource.
 *
 * @author Paul Ferraro
 */
public class BackupSiteResourceDefinitionRegistrar extends ConfigurationResourceDefinitionRegistrar<BackupConfiguration, BackupConfigurationBuilder> {
    static final ResourceRegistration REGISTRATION = ResourceRegistration.of(PathElement.pathElement("backup"));
    static final TernaryServiceDescriptor<BackupConfiguration> SERVICE_DESCRIPTOR = TernaryServiceDescriptor.of(String.join(".", InfinispanServiceDescriptor.CACHE_CONFIGURATION.getName(), REGISTRATION.getPathElement().getKey()), BackupConfiguration.class);
    private static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(SERVICE_DESCRIPTOR).setDynamicNameMapper(TernaryCapabilityNameResolver.GREATGRANDPARENT_GRANDPARENT_CHILD).setAllowMultipleRegistrations(true).build();

    static final EnumAttributeDefinition<BackupFailurePolicy> FAILURE_POLICY = new EnumAttributeDefinition.Builder<>("failure-policy", BackupFailurePolicy.WARN).build();
    static final EnumAttributeDefinition<BackupStrategy> STRATEGY = new EnumAttributeDefinition.Builder<>("strategy", BackupStrategy.ASYNC).build();
    static final DurationAttributeDefinition TIMEOUT = DurationAttributeDefinition.builder("timeout", ChronoUnit.MILLIS).setDefaultValue(Duration.ofSeconds(10)).build();
    static final DurationAttributeDefinition MIN_WAIT = DurationAttributeDefinition.builder("min-wait", ChronoUnit.MILLIS)
            .setDefaultValue(Duration.ZERO)
            .build();
    static final AttributeDefinition AFTER_FAILURES = new SimpleAttributeDefinitionBuilder("after-failures", ModelType.INT)
            .setAllowExpression(true)
            .setRequired(false)
            .setDefaultValue(new ModelNode(1))
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();
    static final AttributeDefinition ENABLED = new SimpleAttributeDefinitionBuilder("enabled", ModelType.BOOLEAN)
            .setAllowExpression(true)
            .setRequired(false)
            .setDefaultValue(ModelNode.TRUE)
            .setDeprecated(InfinispanSubsystemModel.VERSION_16_0_0.getVersion())
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    private final ResourceOperationRuntimeHandler parentRuntimeHandler;
    private final FunctionExecutorRegistry<Cache<?, ?>> executors;

    BackupSiteResourceDefinitionRegistrar(ResourceOperationRuntimeHandler parentRuntimeHandler, FunctionExecutorRegistry<Cache<?, ?>> executors) {
        super(new Configurator<>() {
            @Override
            public ResourceRegistration getResourceRegistration() {
                return REGISTRATION;
            }

            @Override
            public RuntimeCapability<Void> getCapability() {
                return CAPABILITY;
            }
        });
        this.parentRuntimeHandler = parentRuntimeHandler;
        this.executors = executors;
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder)
                .addAttributes(List.of(FAILURE_POLICY, STRATEGY, TIMEOUT, MIN_WAIT, AFTER_FAILURES))
                .addModelOnlyAttributes(List.of(ENABLED))
                ;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ManagementResourceRegistration registration = super.register(parent, context);

        if (context.isRuntimeOnlyRegistrationValid()) {
            new RuntimeOperationStepHandler<>(new BackupSiteOperationExecutor(this.executors), BackupSiteOperation.class).register(registration);
        }

        return registration;
    }

    @Override
    public ResourceOperationRuntimeHandler get() {
        return ResourceOperationRuntimeHandler.combine(super.get(), ResourceOperationRuntimeHandler.restartParent(this.parentRuntimeHandler));
    }

    @Override
    public ServiceDependency<BackupConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        String siteName = context.getCurrentAddressValue();
        BackupFailurePolicy failurePolicy = FAILURE_POLICY.resolve(context, model);
        BackupStrategy strategy = STRATEGY.resolve(context, model);
        Duration timeout = TIMEOUT.resolve(context, model);
        int afterFailures = AFTER_FAILURES.resolveModelAttribute(context, model).asInt();
        Duration minWait = MIN_WAIT.resolve(context, model);
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
                            .minTimeToWait(minWait.toMillis())
                        .backup();
            }
        });
    }
}
