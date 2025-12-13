/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.routing.infinispan;

import java.util.function.UnaryOperator;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ClusteringConfigurationBuilder;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.StorageType;
import org.infinispan.eviction.EvictionStrategy;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.cache.infinispan.embedded.container.DataContainerConfigurationBuilder;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.web.service.routing.LegacyRoutingProviderFactory;
import org.wildfly.clustering.web.service.routing.RoutingProvider;

/**
 * Legacy affinity provider using hard coded values from WF14 and earlier.
 * @author Paul Ferraro
 */
@Deprecated
@MetaInfServices(LegacyRoutingProviderFactory.class)
public class InfinispanLegacyRoutingProviderFactory implements LegacyRoutingProviderFactory, BinaryServiceConfiguration, UnaryOperator<ConfigurationBuilder> {

    @Override
    public RoutingProvider createRoutingProvider() {
        return new InfinispanRoutingProvider(this, this);
    }

    @Override
    public String getParentName() {
        return "web";
    }

    @Override
    public String getChildName() {
        return null;
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
        clustering.stateTransfer().fetchInMemoryState(mode.needsStateTransfer()).awaitInitialTransfer(true);
        return builder;
    }
}
