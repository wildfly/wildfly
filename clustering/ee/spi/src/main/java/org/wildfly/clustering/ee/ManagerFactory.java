/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee;

import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * @author Paul Ferraro
 */
public interface ManagerFactory<K, V> extends BiFunction<Consumer<V>, Consumer<V>, Manager<K, V>> {
    /**
     * Creates a manager using the specified creation and close tasks.
     * @param createTask a task to run on a newly created value
     * @param closeTask a task to run on a value to be closed/dereferenced.
     */
    @Override
    Manager<K, V> apply(Consumer<V> createTask, Consumer<V> closeTask);
}
