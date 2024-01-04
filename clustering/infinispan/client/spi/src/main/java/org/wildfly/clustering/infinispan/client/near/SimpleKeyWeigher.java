/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.client.near;

import java.util.function.Predicate;

import com.github.benmanes.caffeine.cache.Weigher;

/**
 * Weigher that only considers keys passing a given predicate.
 * @author Paul Ferraro
 */
public class SimpleKeyWeigher implements Weigher<Object, Object> {
    private final Predicate<Object> evictable;

    public SimpleKeyWeigher(Predicate<Object> evictable) {
        this.evictable = evictable;
    }

    @Override
    public int weigh(Object key, Object value) {
        return this.evictable.test(key) ? 1 : 0;
    }
}
