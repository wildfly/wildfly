/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.affinity;

import java.util.function.Predicate;

import org.infinispan.Cache;
import org.infinispan.affinity.KeyAffinityService;
import org.infinispan.affinity.KeyGenerator;
import org.infinispan.remoting.transport.Address;

/**
 * Factory for creating a key affinity service.
 * @author Paul Ferraro
 */
public interface KeyAffinityServiceFactory {
    /**
     * Creates a key affinity service for use with the specified cache, that generates local key using the specified generator.
     * @param cache
     * @param generator
     * @return a key affinity service
     */
    @SuppressWarnings("resource")
    default <K> KeyAffinityService<K> createService(Cache<? extends K, ?> cache, KeyGenerator<K> generator) {
        return this.createService(cache, generator, cache.getCacheManager().getAddress()::equals);
    }

    /**
     * Creates a key affinity service for use with the specified cache, that generates key for members matching the specified filter, using the specified generator.
     * @param cache
     * @param generator
     * @return a key affinity service
     */
    <K> KeyAffinityService<K> createService(Cache<? extends K, ?> cache, KeyGenerator<K> generator, Predicate<Address> filter);
}
