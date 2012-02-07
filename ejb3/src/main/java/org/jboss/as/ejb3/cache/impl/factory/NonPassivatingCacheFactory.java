/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.ejb3.cache.impl.factory;

import java.io.Serializable;
import java.util.concurrent.Executors;

import org.jboss.as.ejb3.cache.Cache;
import org.jboss.as.ejb3.cache.CacheFactory;
import org.jboss.as.ejb3.cache.Cacheable;
import org.jboss.as.ejb3.cache.PassivationManager;
import org.jboss.as.ejb3.cache.StatefulObjectFactory;
import org.jboss.as.ejb3.cache.impl.SimpleCache;
import org.jboss.as.ejb3.cache.impl.backing.NonPassivatingBackingCacheEntry;
import org.jboss.as.ejb3.cache.impl.backing.NonPassivatingBackingCacheImpl;
import org.jboss.as.ejb3.component.stateful.StatefulTimeoutInfo;
import org.jboss.as.server.ServerEnvironment;

/**
 * {@link CacheFactory} implementation that will return a non-group-aware cache that doesn't support passivation.
 *
 * @see TransactionalCache
 * @see NonPassivatingBackingCacheImpl
 *
 * @author Brian Stansberry
 * @author Paul Ferraro
 */
public class NonPassivatingCacheFactory<K extends Serializable, V extends Cacheable<K>> implements CacheFactory<K, V> {

    private final ServerEnvironment environment;

    public NonPassivatingCacheFactory(ServerEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public Cache<K, V> createCache(String beanName, StatefulObjectFactory<V> factory, PassivationManager<K, V> passivationManager, StatefulTimeoutInfo timeout) {
        NonPassivatingBackingCacheImpl<K, V> backingCache = new NonPassivatingBackingCacheImpl<K, V>(factory, Executors.defaultThreadFactory(), timeout, this.environment);
        return new SimpleCache<K, V, NonPassivatingBackingCacheEntry<K, V>>(backingCache, false);
    }
}
