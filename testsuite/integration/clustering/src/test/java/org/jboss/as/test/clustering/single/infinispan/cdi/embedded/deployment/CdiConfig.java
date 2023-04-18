/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.single.infinispan.cdi.embedded.deployment;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;

import org.infinispan.cdi.embedded.ConfigureCache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.StorageType;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

/**
 * This is the CDI configuration class.
 * <p>
 * Adopted and adapted from Infinispan testsuite.
 *
 * @author Radoslav Husar
 * @author Kevin Pollet &lt;pollet.kevin@gmail.com&gt; (C) 2011
 * @author Galder Zamarreño
 * @since 27
 */
public class CdiConfig {

    /**
     * <p>This producer defines the greeting cache configuration.</p>
     *
     * <p>This cache will have:
     * <ul>
     *    <li>a maximum of 4 entries</li>
     *    <li>use the strategy LRU for eviction</li>
     * </ul>
     * </p>
     *
     * @return the greeting cache configuration.
     */
    @GreetingCache
    @ConfigureCache("greeting-cache")
    @Produces
    public Configuration greetingCache() {
        return new ConfigurationBuilder().memory().storage(StorageType.HEAP).maxCount(128).build();
    }

    /**
     * <p>This producer overrides the default cache configuration used by the default cache manager.</p>
     *
     * <p>The default cache configuration defines that a cache entry will have a lifespan of 60_000 ms.</p>
     */
    @Produces
    public Configuration defaultCacheConfiguration() {
        return new ConfigurationBuilder()
                .expiration().lifespan(60_000L)
                .build();
    }

    @Produces
    @ApplicationScoped
    public EmbeddedCacheManager defaultEmbeddedCacheManager() {
        return new DefaultCacheManager();
    }

    public void stopEmbeddedCacheManager(@Disposes EmbeddedCacheManager embeddedCacheManager) {
        embeddedCacheManager.stop();
    }

}
