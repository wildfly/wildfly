/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.registry;

import java.util.Map;

/**
 * Factory for creating a clustered registry.
 *
 * @param <K> the type of the registry entry key
 * @param <V> the type of the registry entry value
 * @author Paul Ferraro
 * @deprecated Replaced by {@link org.wildfly.clustering.server.registry.RegistryFactory}.
 */
@Deprecated(forRemoval = true)
public interface RegistryFactory<K, V> {

    /**
     * Creates a registry using the specified entry.
     *
     * @param entry the local registry entry
     * @return a registry
     */
    Registry<K, V> createRegistry(Map.Entry<K, V> entry);
}
