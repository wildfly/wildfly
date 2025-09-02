/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.persistence.jipijapa.hibernate7.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.hibernate.internal.util.cache.InternalCache;
import org.hibernate.internal.util.cache.InternalCacheFactory;

/**
 * A factory implementation of {@link InternalCacheFactory} which creates instances of
 * {@link WildFlyCustomInternalCache}. This implementation leverages the Caffeine library
 * to enhance caching performance and manage an arbitrary approximate size for the cache.
 *
 * This class is designed to integrate with Hibernate to provide a specialized internal
 * cache implementation tailored for use in WildFly.
 *
 * @author Sanne Grinovero <sanne@hibernate.org>
 */
final class WildFlyCustomInternalCacheFactory implements InternalCacheFactory {

    @Override
    public <K, V> InternalCache<K, V> createInternalCache(int intendedApproximateSize) {
        final Cache<K, V> caffeineCache = Caffeine.newBuilder()
                .maximumSize(intendedApproximateSize)
                .build();
        return new WildFlyCustomInternalCache<>(caffeineCache);
    }

}
