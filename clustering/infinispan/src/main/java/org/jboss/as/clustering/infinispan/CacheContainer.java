package org.jboss.as.clustering.infinispan;

import org.infinispan.manager.EmbeddedCacheManager;

public interface CacheContainer extends EmbeddedCacheManager {
    /**
     * Cache name alias for the default cache of a cache container.
     */
    String DEFAULT_CACHE_ALIAS = "default";

    /**
     * Returns the name of the default cache.
     * @return a cache name
     */
    String getDefaultCacheName();
}
