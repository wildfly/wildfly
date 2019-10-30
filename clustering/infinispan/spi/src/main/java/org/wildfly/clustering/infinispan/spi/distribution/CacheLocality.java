/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.infinispan.spi.distribution;

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
        this.locality = (dist != null) ? new ConsistentHashLocality(dist.getCacheTopology()) : new SimpleLocality(true);
    }

    @Override
    public boolean isLocal(Object key) {
        return this.locality.isLocal(key);
    }
}
