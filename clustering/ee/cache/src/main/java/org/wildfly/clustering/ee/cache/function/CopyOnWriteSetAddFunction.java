/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.function;

/**
 * Function that adds an item to a set within a transactional cache.
 * @author Paul Ferraro
 * @param <V> the set element type
 */
public class CopyOnWriteSetAddFunction<V> extends SetAddFunction<V> {

    public CopyOnWriteSetAddFunction(V value) {
        super(value, new CopyOnWriteSetOperations<>());
    }
}
