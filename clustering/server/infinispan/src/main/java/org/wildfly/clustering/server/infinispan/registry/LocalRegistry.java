/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.server.infinispan.registry;

import java.util.Collections;
import java.util.Map;

import org.wildfly.clustering.Registration;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.registry.RegistryListener;

/**
 * Non-clustered {@link Registry} implementation.
 * @author Paul Ferraro
 * @param <K> key type
 * @param <V> value type
 */
public class LocalRegistry<K, V> implements Registry<K, V> {

    private final Group group;
    private final Runnable closeTask;
    private volatile Map.Entry<K, V> entry;

    public LocalRegistry(Group group, Map.Entry<K, V> entry, Runnable closeTask) {
        this.group = group;
        this.closeTask = closeTask;
        this.entry = entry;
    }

    @Override
    public Group getGroup() {
        return this.group;
    }

    @Override
    public Registration register(RegistryListener<K, V> object) {
        // if there are no remote nodes, any registered listener would never get triggered
        return () -> {};
    }

    @Override
    public Map<K, V> getEntries() {
        Map.Entry<K, V> entry = this.entry;
        return (entry != null) ? Collections.singletonMap(entry.getKey(), entry.getValue()) : Collections.emptyMap();
    }

    @Override
    public Map.Entry<K, V> getEntry(Node node) {
        return this.entry;
    }

    @Override
    public void close() {
        this.entry = null;
        this.closeTask.run();
    }
}
