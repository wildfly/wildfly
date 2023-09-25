/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.function;

import java.util.Collection;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Function that removes an item from a collection.
 * @author Paul Ferraro
 * @param <V> the collection element type
 * @param <C> the collection type
 */
public class CollectionRemoveFunction<V, C extends Collection<V>> extends CollectionFunction<V, C> {

    public CollectionRemoveFunction(V value, UnaryOperator<C> copier, Supplier<C> factory) {
        super(value, copier, factory);
    }

    @Override
    public void accept(C collection, V value) {
        collection.remove(value);
    }
}
