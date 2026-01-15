/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.remote;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ClusteringConfigurationBuilder;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.StorageType;
import org.infinispan.eviction.EvictionStrategy;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.cache.infinispan.embedded.container.DataContainerConfigurationBuilder;
import org.wildfly.clustering.ejb.remote.EjbClientServicesProvider;
import org.wildfly.clustering.ejb.remote.LegacyEjbClientServicesProviderFactory;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;

import java.util.function.UnaryOperator;

/**
 * Factory for creating legacy version of the InfinispanClientMappingsRegistryProvider
 *
 * @author Richard Achmatowicz
 */
@Deprecated
@MetaInfServices(LegacyEjbClientServicesProviderFactory.class)
public class LegacyInfinispanEjbClientServicesProviderFactory implements LegacyEjbClientServicesProviderFactory, UnaryOperator<ConfigurationBuilder> {

    @Override
    public EjbClientServicesProvider createEjbClientServicesProvider(String clusterName) {
        return new InfinispanEjbClientServicesProvider(BinaryServiceConfiguration.of(clusterName, null), this);
    }

    @Override
    public ConfigurationBuilder apply(ConfigurationBuilder builder) {
        ClusteringConfigurationBuilder clustering = builder.clustering();
        CacheMode mode = clustering.cacheMode();
        clustering.cacheMode(mode.needsStateTransfer() ? CacheMode.REPL_SYNC : CacheMode.LOCAL);
        clustering.l1().disable();
        // Ensure we use the default data container
        builder.addModule(DataContainerConfigurationBuilder.class);
        // Disable expiration
        builder.expiration().lifespan(-1).maxIdle(-1);
        // Disable eviction
        builder.memory().storage(StorageType.HEAP).maxCount(-1).whenFull(EvictionStrategy.NONE);
        builder.persistence().clearStores();
        clustering.stateTransfer().fetchInMemoryState(mode.needsStateTransfer()).awaitInitialTransfer(mode.needsStateTransfer());
        return builder;
    }
}
