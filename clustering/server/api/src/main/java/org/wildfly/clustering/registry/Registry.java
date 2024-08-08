/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.registry;

import java.util.Map;

import org.wildfly.clustering.Registrar;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;

/**
 * Clustered registry abstraction that stores a unique key/value per node.
 *
 * @param <K> the type of the registry entry key
 * @param <V> the type of the registry entry value
 * @author Paul Ferraro
 * @deprecated Replaced by {@link org.wildfly.clustering.server.registry.Registry}.
 */
@Deprecated(forRemoval = true)
public interface Registry<K, V> extends Registrar<RegistryListener<K, V>>, AutoCloseable {

    /**
     * Returns the group associated with this factory.
     *
     * @return a group
     */
    Group getGroup();

    /**
     * Returns all registry entries in this group.
     *
     * @return a map for entries
     */
    Map<K, V> getEntries();

    /**
     * Returns the registry entry for the specified node.
     *
     * @param node a node
     * @return the node's registry entry, or null if undefined
     */
    Map.Entry<K, V> getEntry(Node node);

    /**
     * Removes our entry from the registry.
     * Once closed, the registry can no longer be accessed.
     */
    @Override
    void close();
}
