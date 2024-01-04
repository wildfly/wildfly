/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.function;

import java.util.Collection;
import java.util.Set;

/**
 * Function that removes an item from a set.
 * @author Paul Ferraro
 * @param <V> the set element type
 */
public class SetRemoveFunction<V> extends CollectionRemoveFunction<V, Set<V>> {

    public SetRemoveFunction(V value) {
        this(Set.of(value));
    }

    public SetRemoveFunction(Collection<V> values) {
        super(values, SetOperations.forOperand(values.iterator().next()));
    }
}
