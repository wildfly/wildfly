/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.infinispan.spi;

import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.infinispan.configuration.cache.StoreConfiguration;
import org.infinispan.eviction.impl.PassivationManager;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.factories.annotations.InfinispanModule;
import org.infinispan.lifecycle.ModuleLifecycle;

/**
 * @author Paul Ferraro
 */
@InfinispanModule(name = "wildfly", requiredModules = "core")
public class WildFlyInfinispanModuleLifecycle implements ModuleLifecycle {

    @Override
    public void cacheStarting(ComponentRegistry registry, Configuration configuration, String cacheName) {
        PersistenceConfiguration persistence = configuration.persistence();
        // If we purge passivation stores on startup, passivating entries on stop is a waste of time
        if (persistence.usingStores() && persistence.passivation() && persistence.stores().stream().allMatch(StoreConfiguration::purgeOnStartup)) {
            PassivationManager passivation = registry.getLocalComponent(PassivationManager.class);
            passivation.skipPassivationOnStop(true);
        }
    }
}
