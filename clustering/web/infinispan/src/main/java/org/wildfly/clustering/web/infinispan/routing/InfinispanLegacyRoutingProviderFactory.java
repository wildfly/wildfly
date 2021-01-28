/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.clustering.web.infinispan.routing;

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
import org.wildfly.clustering.infinispan.spi.ConfigurationBuilderAttributesAccessor;
import org.wildfly.clustering.infinispan.spi.DataContainerConfigurationBuilder;
import org.wildfly.clustering.web.routing.LegacyRoutingProviderFactory;
import org.wildfly.clustering.web.routing.RoutingProvider;

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
