/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.infinispan.invoker;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.remoting.transport.Address;

/**
 * Indicates that the value represented by this object has changed and needs to be replicated.
 * @author Paul Ferraro
 */
public interface Mutator {
    /**
     * Ensure that this object replicates.
     */
    void mutate();

    /**
     * Reusable mutation operation.
     */
    class MutateOperation<K, V> implements CacheInvoker.Operation<K, V, V> {
        private final K key;
        private final V value;

        public MutateOperation(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public V invoke(Cache<K, V> cache) {
            return this.getCache(cache).replace(this.key, this.value);
        }

        private Cache<K, V> getCache(Cache<K, V> cache) {
            DistributionManager dist = cache.getAdvancedCache().getDistributionManager();
            if (dist != null) {
                Address localAddress = cache.getCacheManager().getAddress();
                Address address = dist.getPrimaryLocation(this.key);
                if (!localAddress.equals(address)) {
                    // If we don't own this cache entry, replicate synchronously since
                    // subsequent requests will likely be directed to the owning node
                    return cache.getAdvancedCache().withFlags(Flag.FORCE_SYNCHRONOUS);
                }
            }
            return cache;
        }
    }

    /**
     * Trivial {@link Mutator} implementation that does nothing.
     * New cache entries, in particular, don't require mutation.
     */
    Mutator PASSIVE = new Mutator() {
        @Override
        public void mutate() {
            // Do nothing
        }
    };
}
