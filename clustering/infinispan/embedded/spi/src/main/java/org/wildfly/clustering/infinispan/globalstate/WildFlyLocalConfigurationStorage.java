/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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
