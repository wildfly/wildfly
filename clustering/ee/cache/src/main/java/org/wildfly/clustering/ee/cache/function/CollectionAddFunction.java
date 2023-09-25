/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.function;

import java.util.Collection;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Function that adds an item to a collection.
 * @author Paul Ferraro
 * @param <V> the collection element type
 * @param <C> the collection type
 */
public class CollectionAddFunction<V, C extends Collection<V>> extends CollectionFunction<V, C> {

    public CollectionAddFunction(V value, UnaryOperator<C> copier, Supplier<C> factory) {
        super(value, copier, factory);
    }

    @Override
    public void accept(C collection, V value) {
        collection.add(value);
    }
}
