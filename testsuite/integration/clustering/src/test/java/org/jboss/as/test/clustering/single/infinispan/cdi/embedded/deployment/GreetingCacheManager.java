/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.single.infinispan.cdi.embedded.deployment;


import java.util.Collection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.infinispan.Cache;

/**
 * The greeting cache manager. This manager is used to collect information on the greeting cache and to clear its content if needed.
 * Adopted and adapted from Infinispan testsuite.
 *
 * @author Radoslav Husar
 * @author Kevin Pollet &lt;pollet.kevin@gmail.com&gt; (C) 2011
 * @since 27
 */
@Named
@ApplicationScoped
public class GreetingCacheManager {

    @Inject
    @GreetingCache
    private Cache<String, String> cache;

    public String getCacheName() {
        return cache.getName();
    }

    public int getNumberOfEntries() {
        return cache.size();
    }

    public long getMemorySize() {
        return cache.getCacheConfiguration().memory().maxCount();
    }

    public long getExpirationLifespan() {
        return cache.getCacheConfiguration().expiration().lifespan();
    }

    public String[] getCachedValues() {
        Collection<String> cachedValues = cache.values();
        return cachedValues.toArray(new String[cachedValues.size()]);
    }

    // JCache: @CacheRemoveAll(cacheName = "greeting-cache")
    public void clearCache() {
        cache.clear();
    }

    public void cacheResult(String name, String greeting) {
        cache.put(name, greeting);
    }
}
