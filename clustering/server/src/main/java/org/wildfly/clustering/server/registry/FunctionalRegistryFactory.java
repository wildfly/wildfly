/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
