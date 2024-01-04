/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.lifecycle;

import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.infinispan.configuration.cache.StoreConfiguration;
import org.infinispan.eviction.impl.PassivationManager;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.factories.annotations.InfinispanModule;
import org.infinispan.lifecycle.ModuleLifecycle;
import org.infinispan.persistence.manager.PersistenceManager;

/**
 * @author Paul Ferraro
 */
@InfinispanModule(name = "wildfly", requiredModules = "core")
public class WildFlyInfinispanModuleLifecycle implements ModuleLifecycle {

    @Override
    public void cacheStarting(ComponentRegistry registry, Configuration configuration, String cacheName) {
        PersistenceConfiguration persistence = configuration.persistence();
        // If we purge passivation stores on startup, passivating entries on stop is a waste of time
        if (persistence.usingStores() && persistence.passivation()) {
            PassivationManager passivation = registry.getLocalComponent(PassivationManager.class);
            passivation.skipPassivationOnStop(persistence.stores().stream().allMatch(StoreConfiguration::purgeOnStartup));
        }
        registry.getLocalComponent(PersistenceManager.class).setClearOnStop(false);
    }
}
