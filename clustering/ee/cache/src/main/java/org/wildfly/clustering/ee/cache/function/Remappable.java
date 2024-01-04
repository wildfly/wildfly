/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.function;

/**
 * Implemented by cache value types that support copy-on-write operations.
 * @author Paul Ferraro
 * @param <V> the cache value type
 * @param <O> the operand type
 */
public interface Remappable<V extends Remappable<V, O>, O> {

    /**
     * Returns a new instance of this object with the specified operand applied.
     * @param operand a value applied to the replacement value
     * @return a replacement value
     */
    V remap(O operand);
}
