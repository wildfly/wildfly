/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.registry;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.registry.RegistryFactory;

/**
 * @author Paul Ferraro
 */
public class FunctionalRegistryFactory<K, V> implements RegistryFactory<K, V>, Runnable {

    private final AtomicReference<Map.Entry<K, V>> entry = new AtomicReference<>();
    private final BiFunction<Map.Entry<K, V>, Runnable, Registry<K, V>> factory;

    public FunctionalRegistryFactory(BiFunction<Map.Entry<K, V>, Runnable, Registry<K, V>> factory) {
        this.factory = factory;
    }

    @Override
    public void run() {
        this.entry.set(null);
    }

    @Override
    public Registry<K, V> createRegistry(Map.Entry<K, V> entry) {
        // Ensure only one registry is created at a time
        if (!this.entry.compareAndSet(null, entry)) {
            throw new IllegalStateException();
        }
        return this.factory.apply(entry, this);
    }
}
