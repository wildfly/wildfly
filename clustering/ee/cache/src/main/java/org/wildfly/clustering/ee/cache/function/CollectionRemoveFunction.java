/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.function;

import java.util.Collection;

/**
 * Function that removes an item from a collection.
 * @author Paul Ferraro
 * @param <V> the collection element type
 * @param <C> the collection type
 */
public class CollectionRemoveFunction<V, C extends Collection<V>> extends CollectionFunction<V, C> {

    public CollectionRemoveFunction(Collection<V> operand, Operations<C> operations) {
        super(operand, operations);
    }

    @Override
    public void accept(C collection, Collection<V> operand) {
        collection.removeAll(operand);
    }
}
