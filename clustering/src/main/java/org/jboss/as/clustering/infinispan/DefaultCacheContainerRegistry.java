/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import org.infinispan.manager.CacheContainer;

/**
 * @author Paul Ferraro
 */
public class DefaultCacheContainerRegistry implements CacheContainerRegistry {
    private final AtomicReference<String> defaultContainer = new AtomicReference<String>();
    private final ConcurrentMap<String, RegistryEntry> entries = new ConcurrentHashMap<String, RegistryEntry>();

    /**
     * {@inheritDoc}
     * @see org.jboss.as.clustering.infinispan.CacheContainerRegistry#getCacheContainerNames()
     */
    @Override
    public Set<String> getCacheContainerNames() {
        Set<String> names = new HashSet<String>(this.entries.keySet());
        for (RegistryEntry entry: this.entries.values()) {
            names.removeAll(entry.getAliases());
        }
        return names;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.clustering.infinispan.CacheContainerRegistry#getCacheContainer(java.lang.String)
     */
    @Override
    public CacheContainer getCacheContainer(String name) {
        RegistryEntry entry = this.entries.get(name);
        if (entry == null) {
            throw new IllegalArgumentException(String.format("No cache container named %s found.", name));
        }
        return entry.getContainer();
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.clustering.infinispan.CacheContainerRegistry#addCacheContainer(java.lang.String, org.infinispan.manager.CacheContainer)
     */
    @Override
    public boolean addCacheContainer(String name, Set<String> aliases, CacheContainer container) {
        this.defaultContainer.compareAndSet(null, name);
        RegistryEntry entry = new RegistryEntry(aliases, container);
        boolean added = this.entries.putIfAbsent(name, entry) == null;
        if (added) {
            for (String alias: aliases) {
                if (this.entries.putIfAbsent(alias, entry) != null) {

                }
            }
        }
        return added;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.clustering.infinispan.CacheContainerRegistry#removeCacheContainer(java.lang.String)
     */
    @Override
    public boolean removeCacheContainer(String name) {
        RegistryEntry entry = this.entries.remove(name);
        boolean removed = (entry != null);
        if (removed) {
            for (String alias: entry.getAliases()) {
                this.entries.remove(alias);
            }
        }
        return removed;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.clustering.infinispan.CacheContainerRegistry#getDefaultCacheContainerName()
     */
    @Override
    public String getDefaultCacheContainerName() {
        return this.defaultContainer.get();
    }

    public void setDefaultCacheContainerName(String container) {
        this.defaultContainer.set(container);
    }

    private static class RegistryEntry {
       private final Set<String> aliases;
       private final CacheContainer container;

       RegistryEntry(Set<String> aliases, CacheContainer container) {
          this.container = container;
          this.aliases = aliases;
       }

       public CacheContainer getContainer() {
          return this.container;
       }

       public Set<String> getAliases() {
          return this.aliases;
       }
    }
}
