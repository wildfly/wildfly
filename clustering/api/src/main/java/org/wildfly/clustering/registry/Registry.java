/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.clustering.registry;

import java.util.Map;

import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;

/**
 * Clustered registry abstraction that stores a unique key/value per node.
 * @author Paul Ferraro
 */
public interface Registry<K, V> extends AutoCloseable {

    interface Listener<K, V> {
        void addedEntries(Map<K, V> added);

        void updatedEntries(Map<K, V> updated);

        void removedEntries(Map<K, V> removed);
    }

    /**
     * Returns the group associated with this factory.
     * @return a group
     */
    Group getGroup();

    /**
     * Adds a listener to this registry
     * @param listener a registry listener
     */
    void addListener(Listener<K, V> listener);

    /**
     * Adds a listener from this registry
     * @param listener a registry listener
     */
    void removeListener(Listener<K, V> listener);

    /**
     * Returns all registry entries in this group
     * @return
     */
    Map<K, V> getEntries();

    /**
     * Returns the registry entry for the specified node
     * @param node a node
     * @return the node's registry entry, or null if undefined
     */
    Map.Entry<K, V> getEntry(Node node);

    /**
     * Refreshes and returns the local registry entry from the {@link RegistryEntryProvider}.
     * @return the registry entry of the local node
     */
    Map.Entry<K, V> getLocalEntry();

    /**
     * Removes our entry from the registry.
     * Once closed, the registry can no longer be accessed.
     */
    @Override
    void close();
}
