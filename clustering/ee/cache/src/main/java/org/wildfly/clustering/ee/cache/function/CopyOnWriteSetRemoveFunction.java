/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.function;

/**
 * Function that removes an item from a set within a transactional cache.
 * @author Paul Ferraro
 * @param <V> the set element type
 * @deprecated Superseded by {@link SetRemoveFunction}.
 */
@Deprecated(forRemoval = true)
public class CopyOnWriteSetRemoveFunction<V> extends SetRemoveFunction<V> {

    public CopyOnWriteSetRemoveFunction(V value) {
        super(value);
    }

    public V getValue() {
        return this.getOperand().iterator().next();
    }
}
