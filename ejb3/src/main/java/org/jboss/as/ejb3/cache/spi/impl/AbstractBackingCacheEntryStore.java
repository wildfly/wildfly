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

package org.jboss.as.ejb3.cache.spi.impl;

import java.io.Serializable;

import org.jboss.as.ejb3.cache.Cacheable;
import org.jboss.as.ejb3.cache.spi.BackingCacheEntry;
import org.jboss.as.ejb3.cache.spi.BackingCacheEntryStore;
import org.jboss.as.ejb3.cache.spi.BackingCacheEntryStoreConfig;
import org.jboss.as.ejb3.component.stateful.StatefulTimeoutInfo;

/**
 * @author Paul Ferraro
 *
 */
public abstract class AbstractBackingCacheEntryStore<K extends Serializable, V extends Cacheable<K>, E extends BackingCacheEntry<K, V>>  implements BackingCacheEntryStore<K, V, E> {
    private final BackingCacheEntryStoreConfig config;
    private final StatefulTimeoutInfo timeout;

    protected AbstractBackingCacheEntryStore(StatefulTimeoutInfo timeout, BackingCacheEntryStoreConfig config) {
        this.timeout = timeout;
        this.config = config;
    }

    @Override
    public BackingCacheEntryStoreConfig getConfig() {
        return this.config;
    }

    @Override
    public StatefulTimeoutInfo getTimeout() {
        return this.timeout;
    }
}
