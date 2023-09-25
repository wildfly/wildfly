/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.routing.infinispan;

import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ClusteringConfiguration;
import org.infinispan.configuration.cache.ClusteringConfigurationBuilder;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.StateTransferConfiguration;
import org.infinispan.configuration.cache.StateTransferConfigurationBuilder;
import org.infinispan.configuration.cache.StorageType;
import org.infinispan.eviction.EvictionStrategy;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.infinispan.configuration.ConfigurationBuilderAttributesAccessor;
import org.wildfly.clustering.infinispan.container.DataContainerConfigurationBuilder;
import org.wildfly.clustering.web.service.routing.LegacyRoutingProviderFactory;
import org.wildfly.clustering.web.service.routing.RoutingProvider;

/**
 * Legacy affinity provider using hard coded values from WF14 and earlier.
 * @author Paul Ferraro
 */
@Deprecated
@MetaInfServices(LegacyRoutingProviderFactory.class)
public class InfinispanLegacyRoutingProviderFactory implements LegacyRoutingProviderFactory, InfinispanRoutingConfiguration {

    @Override
    public RoutingProvider createRoutingProvider() {
        return new InfinispanRoutingProvider(this);
    }

    @Override
    public String getContainerName() {
        return "web";
    }

    @Override
    public String getCacheName() {
        return null;
    }

    @Override
    public void accept(ConfigurationBuilder builder) {
        ClusteringConfigurationBuilder clustering = builder.clustering();
        CacheMode mode = clustering.cacheMode();
        clustering.cacheMode(mode.needsStateTransfer() ? CacheMode.REPL_SYNC : CacheMode.LOCAL);
        clustering.l1().disable();
        // Workaround for ISPN-8722
        AttributeSet attributes = ConfigurationBuilderAttributesAccessor.INSTANCE.apply(clustering);
        attributes.attribute(ClusteringConfiguration.BIAS_ACQUISITION).reset();
        attributes.attribute(ClusteringConfiguration.BIAS_LIFESPAN).reset();
        attributes.attribute(ClusteringConfiguration.INVALIDATION_BATCH_SIZE).reset();
        // Ensure we use the default data container
        builder.addModule(DataContainerConfigurationBuilder.class).evictable(null);
        // Disable expiration
        builder.expiration().lifespan(-1).maxIdle(-1);
        // Disable eviction
        builder.memory().storage(StorageType.HEAP).maxCount(-1).whenFull(EvictionStrategy.NONE);
        builder.persistence().clearStores();
        StateTransferConfigurationBuilder stateTransfer = clustering.stateTransfer().fetchInMemoryState(mode.needsStateTransfer());
        attributes = ConfigurationBuilderAttributesAccessor.INSTANCE.apply(stateTransfer);
        attributes.attribute(StateTransferConfiguration.AWAIT_INITIAL_TRANSFER).reset();
        attributes.attribute(StateTransferConfiguration.TIMEOUT).reset();
    }
}
