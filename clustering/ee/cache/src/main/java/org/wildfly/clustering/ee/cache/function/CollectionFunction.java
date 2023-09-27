/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.function;

import java.util.Collection;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Function that operates on a collection.
 * @author Paul Ferraro
 * @param <V> the collection element type
 * @param <C> the collection type
 */
public abstract class CollectionFunction<V, C extends Collection<V>> extends AbstractFunction<V, C> {

    public CollectionFunction(V operand, UnaryOperator<C> copier, Supplier<C> factory) {
        super(operand, copier, factory, Collection::isEmpty);
    }
}
