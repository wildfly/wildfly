/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.clustering.registry;

import java.util.Map;

/**
 * Listener for added, updated and removed entries.
 * @author Paul Ferraro
 * @param <K> the registration key
 * @param <V> the registration value
 */
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
