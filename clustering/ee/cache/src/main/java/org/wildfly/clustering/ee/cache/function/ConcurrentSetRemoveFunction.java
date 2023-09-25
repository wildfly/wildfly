/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.function;

/**
 * Function that removes an item from a set within a non-transactional cache.
 * @author Paul Ferraro
 * @param <V> the set element type
 */
public class ConcurrentSetRemoveFunction<V> extends SetRemoveFunction<V> {

    public ConcurrentSetRemoveFunction(V value) {
        super(value, new ConcurrentSetOperations<>());
    }
}
