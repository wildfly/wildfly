/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.TransactionConfiguration;
import org.infinispan.configuration.cache.TransactionConfigurationBuilder;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.tm.EmbeddedTransactionManager;
import org.jboss.as.clustering.controller.DurationAttributeDefinition;
import org.jboss.as.clustering.controller.EnumAttributeDefinition;
import org.jboss.as.clustering.infinispan.tx.TransactionManagerProvider;
import org.jboss.as.clustering.infinispan.tx.TransactionSynchronizationRegistryProvider;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.dmr.ModelNode;
import org.jboss.tm.XAResourceRecoveryRegistry;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.service.descriptor.NullaryServiceDescriptor;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.capability.ResourceCapabilityReference;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.transaction.client.ContextTransactionManager;

import jakarta.transaction.TransactionSynchronizationRegistry;

/**
 * Registers a resource definition for the transaction component of a cache.
 * @author Paul Ferraro
 */
public class TransactionResourceDefinitionRegistrar extends ConfigurationResourceDefinitionRegistrar<TransactionConfiguration, TransactionConfigurationBuilder> {

    static final BinaryServiceDescriptor<TransactionConfiguration> SERVICE_DESCRIPTOR = BinaryServiceDescriptorFactory.createServiceDescriptor(ComponentResourceRegistration.TRANSACTION, TransactionConfiguration.class);
    private static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(SERVICE_DESCRIPTOR).setDynamicNameMapper(BinaryCapabilityNameResolver.GRANDPARENT_PARENT).setAllowMultipleRegistrations(true).build();

    static final EnumAttributeDefinition<LockingMode> LOCKING = new EnumAttributeDefinition.Builder<>("locking", LockingMode.PESSIMISTIC).build();
    static final EnumAttributeDefinition<TransactionMode> MODE = new EnumAttributeDefinition.Builder<>("mode", TransactionMode.NONE).build();
    static final DurationAttributeDefinition STOP_TIMEOUT = new DurationAttributeDefinition.Builder("stop-timeout", ChronoUnit.MILLIS).setDefaultValue(Duration.ofSeconds(10)).build();
    static final DurationAttributeDefinition COMPLETE_TIMEOUT = new DurationAttributeDefinition.Builder("complete-timeout", ChronoUnit.MILLIS).setDefaultValue(Duration.ofMinutes(1)).build();

    static final NullaryServiceDescriptor<Void> LOCAL_TRANSACTION_PROVIDER = NullaryServiceDescriptor.of("org.wildfly.transactions.global-default-local-provider", Void.class);
    static final NullaryServiceDescriptor<TransactionSynchronizationRegistry> TRANSACTION_SYNCHRONIZATION_REGISTRY = NullaryServiceDescriptor.of("org.wildfly.transactions.transaction-synchronization-registry", TransactionSynchronizationRegistry.class);

    private static final ResourceCapabilityReference<Void> LOCAL_TRANSACTION_PROVIDER_REFERENCE = ResourceCapabilityReference.builder(CAPABILITY, LOCAL_TRANSACTION_PROVIDER).when(MODE, new TransactionModeFilter(EnumSet.complementOf(EnumSet.of(TransactionMode.NONE, TransactionMode.BATCH)))).build();
    private static final ResourceCapabilityReference<TransactionSynchronizationRegistry> TRANSACTION_SYNCHRONIZATION_REGISTRY_REFERENCE = ResourceCapabilityReference.builder(CAPABILITY, TRANSACTION_SYNCHRONIZATION_REGISTRY).when(MODE, new TransactionModeFilter(TransactionMode.NON_XA)).build();
    private static final ResourceCapabilityReference<XAResourceRecoveryRegistry> XA_RESOURCE_RECOVERY_REGISTRY_REFERENCE = ResourceCapabilityReference.builder(CAPABILITY, XAResourceRecoveryServiceInstallerFactory.XA_RESOURCE_RECOVERY_REGISTRY).when(MODE, new TransactionModeFilter(TransactionMode.FULL_XA)).build();

    private static class TransactionModeFilter implements Predicate<TransactionMode> {
        private final Set<TransactionMode> modes;

        TransactionModeFilter(TransactionMode mode) {
            this(EnumSet.of(mode));
        }

        TransactionModeFilter(Set<TransactionMode> modes) {
            this.modes = modes;
        }

        @Override
        public boolean test(TransactionMode value) {
            return this.modes.contains(value);
        }
    }

    TransactionResourceDefinitionRegistrar() {
        super(new Configurator() {
            @Override
            public ResourceRegistration getResourceRegistration() {
                return ComponentResourceRegistration.TRANSACTION;
            }

            @Override
            public RuntimeCapability<Void> getCapability() {
                return CAPABILITY;
            }

            @Override
            public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
                return Configurator.super.apply(builder)
                        .addAttributes(List.of(LOCKING, MODE, STOP_TIMEOUT, COMPLETE_TIMEOUT))
                        .addResourceCapabilityReferences(List.of(LOCAL_TRANSACTION_PROVIDER_REFERENCE, TRANSACTION_SYNCHRONIZATION_REGISTRY_REFERENCE, XA_RESOURCE_RECOVERY_REGISTRY_REFERENCE))
                        ;
            }
        });
    }

    @Override
    public ServiceDependency<TransactionConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        TransactionMode mode = MODE.resolve(context, model);
        LockingMode locking = LOCKING.resolve(context, model);
        Duration stopTimeout = STOP_TIMEOUT.resolve(context, model);
        Duration transactionTimeout = COMPLETE_TIMEOUT.resolve(context, model);
        ServiceDependency<Void> localTransactionProvider = LOCAL_TRANSACTION_PROVIDER_REFERENCE.resolve(context, model);
        ServiceDependency<TransactionSynchronizationRegistry> tsr = TRANSACTION_SYNCHRONIZATION_REGISTRY_REFERENCE.resolve(context, model);
        ServiceDependency<XAResourceRecoveryRegistry> registry = XA_RESOURCE_RECOVERY_REGISTRY_REFERENCE.resolve(context, model);
        return new ServiceDependency<>() {
            @Override
            public void accept(RequirementServiceBuilder<?> builder) {
                localTransactionProvider.accept(builder);
                tsr.accept(builder);
                registry.accept(builder);
            }

            @Override
            public TransactionConfigurationBuilder get() {
                TransactionConfigurationBuilder builder = new ConfigurationBuilder().transaction()
                        .lockingMode(locking)
                        .cacheStopTimeout(stopTimeout.toMillis(), TimeUnit.MILLISECONDS)
                        .completedTxTimeout(transactionTimeout.toMillis())
                        .transactionMode((mode == TransactionMode.NONE) ? org.infinispan.transaction.TransactionMode.NON_TRANSACTIONAL : org.infinispan.transaction.TransactionMode.TRANSACTIONAL)
                        .useSynchronization(mode == TransactionMode.NON_XA)
                        .recovery().enabled(mode == TransactionMode.FULL_XA).transaction()
                        ;
                if (mode != TransactionMode.NONE) {
                    builder.transactionManagerLookup(new TransactionManagerProvider((mode == TransactionMode.BATCH) ? EmbeddedTransactionManager.getInstance() : ContextTransactionManager.getInstance()));
                }
                if (tsr.isPresent()) {
                    builder.transactionSynchronizationRegistryLookup(new TransactionSynchronizationRegistryProvider(tsr.get()));
                }
                return builder;
            }
        };
    }
}
