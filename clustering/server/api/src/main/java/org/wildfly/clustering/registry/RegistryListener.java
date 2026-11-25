/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.registry;

import java.util.Map;

/**
 * Listener for added, updated and removed entries.
 * @author Paul Ferraro
 * @param <K> the registration key
 * @param <V> the registration value
 * @deprecated Replaced by {@link org.wildfly.clustering.server.registry.RegistryListener}.
 */
@Deprecated(forRemoval = true)
public interface RegistryListener<K, V> {
    /**
     * Called when new entries have been added.
     *
     * @param added a map of entries that have been added
     */
    void addedEntries(Map<K, V> added);

    /**
     * Called when existing entries have been updated.
     *
     * @param updated a map of entries that have been updated
     */
    void updatedEntries(Map<K, V> updated);

    /**
     * Called when entries have been removed.
     *
     * @param removed a map of entries that have been removed
     */
    void removedEntries(Map<K, V> removed);
}
