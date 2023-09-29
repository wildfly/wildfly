/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.function;

import java.util.Collection;

/**
 * Function that operates on a collection.
 * @author Paul Ferraro
 * @param <V> the collection element type
 * @param <C> the collection type
 */
public abstract class CollectionFunction<V, C extends Collection<V>> extends AbstractFunction<Collection<V>, C> {

    public CollectionFunction(Collection<V> operand, Operations<C> operations) {
        super(operand, operations.getCopier(), operations.getFactory(), operations.isEmpty());
    }

    @Override
    public int hashCode() {
        int result = 1;
        for (V value : this.getOperand()) {
            result = 31 * result + value.hashCode();
        }
        return result;
    }

    @Override
    public boolean equals(Object object) {
        if (!this.getClass().isInstance(object)) return false;
        @SuppressWarnings("unchecked")
        CollectionFunction<V, C> function = (CollectionFunction<V, C>) object;
        Collection<V> ourOperand = this.getOperand();
        Collection<V> otherOperand = function.getOperand();
        return ourOperand.size() == otherOperand.size() && ourOperand.containsAll(otherOperand);
    }
}
