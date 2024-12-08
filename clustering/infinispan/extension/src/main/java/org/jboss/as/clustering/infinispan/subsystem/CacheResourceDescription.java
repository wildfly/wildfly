/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.stream.Stream;

import org.infinispan.commons.CacheException;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.ExpirationConfiguration;
import org.infinispan.configuration.cache.LockingConfiguration;
import org.infinispan.configuration.cache.MemoryConfiguration;
import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.infinispan.configuration.cache.TransactionConfiguration;
import org.infinispan.configuration.cache.TransactionConfigurationBuilder;
import org.infinispan.distribution.ch.impl.AffinityPartitioner;
import org.infinispan.transaction.tm.EmbeddedTransactionManager;
import org.jboss.as.clustering.controller.ModuleListAttributeDefinition;
import org.jboss.as.clustering.controller.StatisticsEnabledAttributeDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Describes a cache resource.
 * @author Paul Ferraro
 */
public interface CacheResourceDescription<P> extends ResourceCapabilityDescription<Configuration>, ResourceModelResolver<ServiceDependency<ConfigurationBuilder>> {

    RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(InfinispanServiceDescriptor.CACHE_CONFIGURATION).setAllowMultipleRegistrations(true).build();

    StatisticsEnabledAttributeDefinition STATISTICS_ENABLED = new StatisticsEnabledAttributeDefinition.Builder().build();
    ModuleListAttributeDefinition MODULES = new ModuleListAttributeDefinition.Builder().setRequired(false).build();

    @Override
    default BinaryServiceDescriptor<Configuration> getServiceDescriptor() {
        return InfinispanServiceDescriptor.CACHE_CONFIGURATION;
    }

    @Override
    default RuntimeCapability<Void> getCapability() {
        return RuntimeCapability.Builder.of(this.getServiceDescriptor()).setAllowMultipleRegistrations(true).build();
    }

    @Override
    default Stream<AttributeDefinition> getAttributes() {
        return Stream.of(STATISTICS_ENABLED, MODULES);
    }

    Class<P> getProviderClass();

    CacheMode getCacheMode();

    default InfinispanSubsystemModel getDeprecation() {
        return null;
    }

    @Override
    default ServiceDependency<ConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        BinaryServiceConfiguration config = BinaryServiceConfiguration.of(address.getParent().getLastElement().getValue(), address.getLastElement().getValue());

        CacheMode mode = this.getCacheMode();
        boolean statisticsEnabled = STATISTICS_ENABLED.resolve(context, model);
        ServiceDependency<ExpirationConfiguration> expiration = config.getServiceDependency(ExpirationResourceDescription.INSTANCE.getServiceDescriptor());
        ServiceDependency<MemoryConfiguration> memory = config.getServiceDependency(MemoryResourceDescription.SERVICE_DESCRIPTOR);
        ServiceDependency<LockingConfiguration> locking = config.getServiceDependency(LockingResourceDescription.INSTANCE.getServiceDescriptor());
        ServiceDependency<PersistenceConfiguration> persistence = config.getServiceDependency(PersistenceResourceDescription.SERVICE_DESCRIPTOR);
        ServiceDependency<TransactionConfiguration> transaction = config.getServiceDependency(TransactionResourceDescription.INSTANCE.getServiceDescriptor());

        return new ServiceDependency<>() {
            @Override
            public void accept(RequirementServiceBuilder<?> builder) {
                memory.accept(builder);
                expiration.accept(builder);
                locking.accept(builder);
                transaction.accept(builder);
                persistence.accept(builder);
            }

            @Override
            public ConfigurationBuilder get() {
                ConfigurationBuilder builder = new ConfigurationBuilder();

                builder.clustering().cacheMode(mode).hash().keyPartitioner(new AffinityPartitioner());
                builder.memory().read(memory.get());
                builder.expiration().read(expiration.get());
                builder.locking().read(locking.get());
                builder.persistence().read(persistence.get());
                builder.transaction().read(transaction.get());
                builder.statistics().enabled(statisticsEnabled);

                try {
                    // Configure invocation batching based on transaction configuration
                    TransactionConfigurationBuilder tx = builder.transaction();
                    builder.invocationBatching().enable(tx.transactionMode().isTransactional() && (tx.transactionManagerLookup().getTransactionManager() == EmbeddedTransactionManager.getInstance()));
                } catch (Exception e) {
                    throw new CacheException(e);
                }
                return builder;
            }
        };
    }
}
