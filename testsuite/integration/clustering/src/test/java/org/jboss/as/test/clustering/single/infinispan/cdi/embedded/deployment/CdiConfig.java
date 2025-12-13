/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.single.infinispan.cdi.embedded.deployment;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;

import org.infinispan.cdi.embedded.ConfigureCache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.StorageType;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
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
        // Disable metrics - otherwise, we would need to ensure Micrometer available is available to deployment.
        return new DefaultCacheManager(new GlobalConfigurationBuilder().metrics().gauges(false).histograms(false).build());
    }

    public void stopEmbeddedCacheManager(@Disposes EmbeddedCacheManager embeddedCacheManager) {
        embeddedCacheManager.stop();
    }

}
