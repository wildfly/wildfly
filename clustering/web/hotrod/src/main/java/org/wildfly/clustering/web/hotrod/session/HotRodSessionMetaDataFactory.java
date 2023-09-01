/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.hotrod.session;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.infinispan.client.hotrod.RemoteCache;
import org.wildfly.clustering.ee.Key;
import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.ee.MutatorFactory;
import org.wildfly.clustering.ee.cache.CacheProperties;
import org.wildfly.clustering.ee.hotrod.HotRodConfiguration;
import org.wildfly.clustering.ee.hotrod.RemoteCacheEntryMutator;
import org.wildfly.clustering.ee.hotrod.RemoteCacheMutatorFactory;
import org.wildfly.clustering.web.cache.session.CompositeSessionMetaData;
import org.wildfly.clustering.web.cache.session.CompositeSessionMetaDataEntry;
import org.wildfly.clustering.web.cache.session.InvalidatableSessionMetaData;
import org.wildfly.clustering.web.cache.session.MutableSessionAccessMetaData;
import org.wildfly.clustering.web.cache.session.MutableSessionCreationMetaData;
import org.wildfly.clustering.web.cache.session.SessionAccessMetaData;
import org.wildfly.clustering.web.cache.session.SessionCreationMetaData;
import org.wildfly.clustering.web.cache.session.SessionCreationMetaDataEntry;
import org.wildfly.clustering.web.cache.session.SessionMetaDataFactory;
import org.wildfly.clustering.web.cache.session.SimpleSessionAccessMetaData;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;

/**
 * Factory for creating {@link SessionMetaData} backed by a pair of {@link RemoteCache} entries.
 * @author Paul Ferraro
 * @param <C> the local context type
 */
public class HotRodSessionMetaDataFactory<C> implements SessionMetaDataFactory<CompositeSessionMetaDataEntry<C>> {

    private final RemoteCache<Key<String>, Object> cache;
    private final RemoteCache<SessionCreationMetaDataKey, SessionCreationMetaDataEntry<C>> creationMetaDataCache;
    private final RemoteCache<SessionAccessMetaDataKey, SessionAccessMetaData> accessMetaDataCache;
    private final MutatorFactory<SessionCreationMetaDataKey, SessionCreationMetaDataEntry<C>> creationMetaDataMutatorFactory;
    private final CacheProperties properties;

    public HotRodSessionMetaDataFactory(HotRodConfiguration configuration) {
        this.cache = configuration.getCache();
        this.creationMetaDataCache = configuration.getCache();
        this.creationMetaDataMutatorFactory = new RemoteCacheMutatorFactory<>(this.creationMetaDataCache);
        this.accessMetaDataCache = configuration.getCache();
        this.properties = configuration.getCacheProperties();
    }

    @Override
    public CompositeSessionMetaDataEntry<C> createValue(String id, SessionCreationMetaData creationMetaData) {
        SessionCreationMetaDataEntry<C> creationMetaDataEntry = new SessionCreationMetaDataEntry<>(creationMetaData);
        SessionAccessMetaData accessMetaData = new SimpleSessionAccessMetaData();
        this.creationMetaDataMutatorFactory.createMutator(new SessionCreationMetaDataKey(id), creationMetaDataEntry).mutate();
        this.createSessionAccessMetaDataMutator(new SessionAccessMetaDataKey(id), accessMetaData, creationMetaData).mutate();
        return new CompositeSessionMetaDataEntry<>(creationMetaDataEntry, accessMetaData);
    }

    @Override
    public CompositeSessionMetaDataEntry<C> findValue(String id) {
        SessionCreationMetaDataKey creationMetaDataKey = new SessionCreationMetaDataKey(id);
        SessionAccessMetaDataKey accessMetaDataKey = new SessionAccessMetaDataKey(id);
        // Use bulk read
        Map<Key<String>, Object> entries = this.cache.getAll(Set.of(creationMetaDataKey, accessMetaDataKey));
        @SuppressWarnings("unchecked")
        SessionCreationMetaDataEntry<C> creationMetaDataEntry = (SessionCreationMetaDataEntry<C>) entries.get(creationMetaDataKey);
        SessionAccessMetaData accessMetaData = (SessionAccessMetaData) entries.get(accessMetaDataKey);
        if ((creationMetaDataEntry != null) && (accessMetaData != null)) {
            return new CompositeSessionMetaDataEntry<>(creationMetaDataEntry, accessMetaData);
        }
        // Any orphan entry should not be removed here - this would otherwise interfere with expiration listener
        return null;
    }

    @Override
    public InvalidatableSessionMetaData createSessionMetaData(String id, CompositeSessionMetaDataEntry<C> entry) {
        boolean newSession = entry.getCreationMetaData().isNew();
        boolean requireMutator = !this.properties.isTransactional() || !newSession;

        SessionCreationMetaData creationMetaData = entry.getCreationMetaData();
        if (requireMutator) {
            SessionCreationMetaDataKey creationMetaDataKey = new SessionCreationMetaDataKey(id);
            SessionCreationMetaDataEntry<C> creationMetaDataEntry = new SessionCreationMetaDataEntry<>(creationMetaData, entry.getLocalContext());
            Mutator mutator = this.creationMetaDataMutatorFactory.createMutator(creationMetaDataKey, creationMetaDataEntry);
            creationMetaData = new MutableSessionCreationMetaData(creationMetaData, mutator);
        }

        SessionAccessMetaData accessMetaData = entry.getAccessMetaData();
        if (requireMutator) {
            SessionAccessMetaDataKey accessMetaDataKey = new SessionAccessMetaDataKey(id);
            Mutator mutator = this.createSessionAccessMetaDataMutator(accessMetaDataKey, accessMetaData, creationMetaData);
            accessMetaData = new MutableSessionAccessMetaData(entry.getAccessMetaData(), mutator);
        }

        return new CompositeSessionMetaData(creationMetaData, accessMetaData);
    }

    @Override
    public ImmutableSessionMetaData createImmutableSessionMetaData(String id, CompositeSessionMetaDataEntry<C> entry) {
        return new CompositeSessionMetaData(entry.getCreationMetaData(), entry.getAccessMetaData());
    }

    @Override
    public boolean remove(String id) {
        this.accessMetaDataCache.remove(new SessionAccessMetaDataKey(id));
        this.creationMetaDataCache.remove(new SessionCreationMetaDataKey(id));
        return true;
    }

    private Mutator createSessionAccessMetaDataMutator(SessionAccessMetaDataKey key, SessionAccessMetaData value, SessionCreationMetaData creationMetaData) {
        // Max-idle for expiration references session timeout from creation metadata entry
        return new RemoteCacheEntryMutator<>(this.accessMetaDataCache, key, value, new Function<>() {
            @Override
            public Duration apply(SessionAccessMetaData accessMetaData) {
                return creationMetaData.getTimeout();
            }
        });
    }}
