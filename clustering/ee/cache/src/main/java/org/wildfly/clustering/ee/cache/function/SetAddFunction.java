/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.function;

import java.util.Set;

/**
 * Function that adds an item to a set.
 * @author Paul Ferraro
 * @param <V> the set element type
 */
public class SetAddFunction<V> extends CollectionAddFunction<V, Set<V>> {

    public SetAddFunction(V value, Operations<Set<V>> operations) {
        super(value, operations, operations);
    }
}
