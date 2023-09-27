/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.globalstate;

import static org.infinispan.factories.KnownComponentNames.CACHE_DEPENDENCY_GRAPH;

import java.util.EnumSet;
import java.util.concurrent.CompletionStage;

import org.infinispan.Cache;
import org.infinispan.commons.api.CacheContainerAdmin;
import org.infinispan.configuration.ConfigurationManager;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.util.DependencyGraph;

/**
 * Custom {@link org.infinispan.globalstate.impl.VolatileLocalConfigurationStorage} that doesn't mess with the {@link org.infinispan.eviction.impl.PassivationManager} or {@link org.infinispan.persistence.manager.PersistenceManager}.
 * @author Paul Ferraro
 */
public class WildFlyLocalConfigurationStorage extends org.infinispan.globalstate.impl.VolatileLocalConfigurationStorage {

    @SuppressWarnings("deprecation")
    @Override
    public CompletionStage<Void> removeCache(String name, EnumSet<CacheContainerAdmin.AdminFlag> flags) {
        return this.blockingManager.<Void>supplyBlocking(() -> {
            GlobalComponentRegistry globalComponentRegistry = this.cacheManager.getGlobalComponentRegistry();
            Cache<?, ?> cache = this.cacheManager.getCache(name, false);
            if (cache != null) {
                cache.stop();
            }
            globalComponentRegistry.removeCache(name);
            // Remove cache configuration and remove it from the computed cache name list
            globalComponentRegistry.getComponent(ConfigurationManager.class).removeConfiguration(name);
            // Remove cache from dependency graph
            globalComponentRegistry.getComponent(DependencyGraph.class, CACHE_DEPENDENCY_GRAPH).remove(name);
            return null;
        }, name).toCompletableFuture();
    }
}
