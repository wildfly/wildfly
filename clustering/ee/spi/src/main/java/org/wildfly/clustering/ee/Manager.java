/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Strategy for managing the creation and destruction of objects.
 * @author Paul Ferraro
 */
public interface Manager<K, V> extends BiFunction<K, Function<Runnable, V>, V> {
    /**
     * Returns the value associated with the specified key, creating it from the specified factory, if necessary.
     * @param key the key of the managed value
     * @param factory a factory for creating the value from a close task
     * @return a managed value
     */
    @Override
    V apply(K key, Function<Runnable, V> factory);
}
