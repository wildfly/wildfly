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

package org.jboss.as.ejb3.cache.impl.backing.clustering;

import java.io.Serializable;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.jboss.as.ejb3.cache.Cacheable;
import org.jboss.as.ejb3.cache.spi.BackingCacheEntryStoreSourceService;

/**
 * @author paul
 *
 */
public class ClusteredBackingCacheEntryStoreSourceService<K extends Serializable, V extends Cacheable<K>, G extends Serializable> extends BackingCacheEntryStoreSourceService<K, V, G, ClusteredBackingCacheEntryStoreSource<K, V, G>> {
    /**
     * @param name
     */
    public ClusteredBackingCacheEntryStoreSourceService(String name) {
        super(name, ClusteredBackingCacheEntryStoreSourceService.<K, V, G>load());
    }

    private static <K extends Serializable, V extends Cacheable<K>, G extends Serializable> ClusteredBackingCacheEntryStoreSource<K, V, G> load() {
        for (ClusteredBackingCacheEntryStoreSource<K, V, G> source: ServiceLoader.load(ClusteredBackingCacheEntryStoreSource.class, ClusteredBackingCacheEntryStoreSourceService.class.getClassLoader())) {
            return source;
        }
        throw new ServiceConfigurationError(ClusteredBackingCacheEntryStoreSource.class.getName());
    }
}
