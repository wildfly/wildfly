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
