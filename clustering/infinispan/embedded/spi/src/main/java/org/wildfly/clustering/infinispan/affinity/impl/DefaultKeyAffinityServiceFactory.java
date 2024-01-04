/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.affinity.impl;

import java.util.function.Predicate;

import org.infinispan.Cache;
import org.infinispan.affinity.KeyAffinityService;
import org.infinispan.affinity.KeyGenerator;
import org.infinispan.remoting.transport.Address;
import org.wildfly.clustering.infinispan.affinity.KeyAffinityServiceFactory;

/**
 * Factory for a {@link KeyAffinityService} whose implementation varies depending on cache mode.
 * @author Paul Ferraro
 */
public class DefaultKeyAffinityServiceFactory implements KeyAffinityServiceFactory {

    @Override
    public <K> KeyAffinityService<K> createService(Cache<? extends K, ?> cache, KeyGenerator<K> generator, Predicate<Address> filter) {
        return cache.getCacheConfiguration().clustering().cacheMode().isClustered() ? new DefaultKeyAffinityService<>(cache, generator, filter) : new SimpleKeyAffinityService<>(generator);
    }
}
