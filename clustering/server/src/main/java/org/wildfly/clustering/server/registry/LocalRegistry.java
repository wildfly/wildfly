/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.server.registry;

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
