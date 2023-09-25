/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.distribution;

import org.infinispan.Cache;
import org.infinispan.distribution.DistributionManager;

/**
 * A {@link Locality} implementation that delegates to either a {@link ConsistentHashLocality} or {@link SimpleLocality} depending on the cache mode.
 * Instances of this object should not be retained for longer than a single unit of work, since this object holds a final reference to the current ConsistentHash, which will become stale on topology change.
 * @author Paul Ferraro
 */
public class CacheLocality implements Locality {

    private final Locality locality;

    public CacheLocality(Cache<?, ?> cache) {
        DistributionManager dist = cache.getAdvancedCache().getDistributionManager();
        this.locality = (dist != null) ? new ConsistentHashLocality(cache, dist.getCacheTopology().getWriteConsistentHash()) : new SimpleLocality(true);
    }

    @Override
    public boolean isLocal(Object key) {
        return this.locality.isLocal(key);
    }
}
