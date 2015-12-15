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

import java.util.concurrent.atomic.AtomicReference;

import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.registry.RegistryEntryProvider;
import org.wildfly.clustering.registry.RegistryFactory;

/**
 * Non-clustered {@link RegistryFactory} implementation.
 * @author Paul Ferraro
 * @param <K> the registry key type
 * @param <V> the registry value type
 */
public class LocalRegistryFactory<K, V> implements RegistryFactory<K, V> {

    final AtomicReference<RegistryEntryProvider<K, V>> provider = new AtomicReference<>();

    private final Group group;

    public LocalRegistryFactory(Group group) {
        this.group = group;
    }

    @Override
    public Registry<K, V> createRegistry(RegistryEntryProvider<K, V> provider) {
        // Ensure only one registry is created at a time
        if (!this.provider.compareAndSet(null, provider)) {
            throw new IllegalStateException();
        }
        return new LocalRegistry<K, V>(this.group, provider) {
            @Override
            public void close() {
                super.close();
                LocalRegistryFactory.this.provider.set(null);
            }
        };
    }
}
