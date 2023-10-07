/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.hotrod.session.metadata;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.wildfly.clustering.ee.Key;
import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.ee.cache.offset.OffsetValue;
import org.wildfly.clustering.ee.hotrod.HotRodConfiguration;
import org.wildfly.clustering.ee.hotrod.RemoteCacheEntryComputeMutator;
import org.wildfly.clustering.ee.hotrod.RemoteCacheEntryMutator;
import org.wildfly.clustering.web.cache.session.metadata.InvalidatableSessionMetaData;
import org.wildfly.clustering.web.cache.session.metadata.SessionMetaDataFactory;
import org.wildfly.clustering.web.cache.session.metadata.fine.CompositeImmutableSessionMetaData;
import org.wildfly.clustering.web.cache.session.metadata.fine.CompositeSessionMetaData;
import org.wildfly.clustering.web.cache.session.metadata.fine.SessionCreationMetaDataEntry;
import org.wildfly.clustering.web.cache.session.metadata.fine.SessionCreationMetaDataEntryFunction;
import org.wildfly.clustering.web.cache.session.metadata.fine.SessionMetaDataEntry;
import org.wildfly.clustering.web.cache.session.metadata.fine.MutableSessionAccessMetaData;
import org.wildfly.clustering.web.cache.session.metadata.fine.MutableSessionAccessMetaDataOffsetValues;
import org.wildfly.clustering.web.cache.session.metadata.fine.MutableSessionCreationMetaData;
import org.wildfly.clustering.web.cache.session.metadata.fine.SessionAccessMetaData;
import org.wildfly.clustering.web.cache.session.metadata.fine.SessionAccessMetaDataEntry;
import org.wildfly.clustering.web.cache.session.metadata.fine.SessionAccessMetaDataEntryFunction;
import org.wildfly.clustering.web.cache.session.metadata.fine.SessionCreationMetaData;
import org.wildfly.clustering.web.cache.session.metadata.fine.DefaultSessionCreationMetaDataEntry;
import org.wildfly.clustering.web.cache.session.metadata.fine.DefaultSessionMetaDataEntry;
import org.wildfly.clustering.web.cache.session.metadata.fine.DefaultSessionAccessMetaDataEntry;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;
import org.wildfly.clustering.web.session.SessionMetaData;

/**
 * Factory for creating {@link SessionMetaData} backed by a pair of {@link RemoteCache} entries.
 * @author Paul Ferraro
 * @param <C> the local context type
 */
public class HotRodSessionMetaDataFactory<C> implements SessionMetaDataFactory<SessionMetaDataEntry<C>> {

    private final RemoteCache<Key<String>, Object> cache;
    private final Flag[] ignoreReturnFlags;
    private final RemoteCache<SessionCreationMetaDataKey, SessionCreationMetaDataEntry<C>> creationMetaDataCache;
    private final RemoteCache<SessionAccessMetaDataKey, SessionAccessMetaDataEntry> accessMetaDataCache;

    public HotRodSessionMetaDataFactory(HotRodConfiguration configuration) {
        this.cache = configuration.getCache();
        this.ignoreReturnFlags = configuration.getIgnoreReturnFlags();
        this.creationMetaDataCache = configuration.getCache();
        this.accessMetaDataCache = configuration.getCache();
    }

    @Override
    public SessionMetaDataEntry<C> createValue(String id, Duration defaultTimeout) {
        SessionCreationMetaDataEntry<C> creationMetaData = new DefaultSessionCreationMetaDataEntry<>();
        creationMetaData.setTimeout(defaultTimeout);
        SessionAccessMetaDataEntry accessMetaData = new DefaultSessionAccessMetaDataEntry();
        this.creationMetaDataCache.withFlags(this.ignoreReturnFlags).put(new SessionCreationMetaDataKey(id), creationMetaData);
        new RemoteCacheEntryMutator<>(this.accessMetaDataCache, this.ignoreReturnFlags, new SessionAccessMetaDataKey(id), accessMetaData, creationMetaData::getTimeout).mutate();
        return new DefaultSessionMetaDataEntry<>(creationMetaData, accessMetaData);
    }

    @Override
    public SessionMetaDataEntry<C> findValue(String id) {
        SessionCreationMetaDataKey creationMetaDataKey = new SessionCreationMetaDataKey(id);
        SessionAccessMetaDataKey accessMetaDataKey = new SessionAccessMetaDataKey(id);
        // Use bulk read
        Map<Key<String>, Object> entries = this.cache.getAll(Set.of(creationMetaDataKey, accessMetaDataKey));
        @SuppressWarnings("unchecked")
        SessionCreationMetaDataEntry<C> creationMetaData = (SessionCreationMetaDataEntry<C>) entries.get(creationMetaDataKey);
        SessionAccessMetaDataEntry accessMetaData = (SessionAccessMetaDataEntry) entries.get(accessMetaDataKey);
        // Any orphan entry should not be removed here - this would otherwise interfere with expiration listener
        return (creationMetaData != null) && (accessMetaData != null) ? new DefaultSessionMetaDataEntry<>(creationMetaData, accessMetaData) : null;
    }

    @Override
    public InvalidatableSessionMetaData createSessionMetaData(String id, SessionMetaDataEntry<C> entry) {
        OffsetValue<Duration> timeoutOffset = OffsetValue.from(entry.getCreationMetaDataEntry().getTimeout());
        SessionCreationMetaData creationMetaData = new MutableSessionCreationMetaData(entry.getCreationMetaDataEntry(), timeoutOffset);

        MutableSessionAccessMetaDataOffsetValues values = MutableSessionAccessMetaDataOffsetValues.from(entry.getAccessMetaDataEntry());
        SessionAccessMetaData accessMetaData = new MutableSessionAccessMetaData(entry.getAccessMetaDataEntry(), values);

        Mutator creationMetaDataMutator = new RemoteCacheEntryComputeMutator<>(this.creationMetaDataCache, this.ignoreReturnFlags, new SessionCreationMetaDataKey(id), new SessionCreationMetaDataEntryFunction<>(timeoutOffset));
        Mutator accessMetaDataMutator = new RemoteCacheEntryComputeMutator<>(this.accessMetaDataCache, this.ignoreReturnFlags, new SessionAccessMetaDataKey(id), new SessionAccessMetaDataEntryFunction(values), creationMetaData::getTimeout);
        Mutator mutator = new Mutator() {
            @Override
            public void mutate() {
                if (!timeoutOffset.getOffset().isZero()) {
                    creationMetaDataMutator.mutate();
                }
                accessMetaDataMutator.mutate();
            }
        };

        return new CompositeSessionMetaData(creationMetaData, accessMetaData, mutator);
    }

    @Override
    public ImmutableSessionMetaData createImmutableSessionMetaData(String id, SessionMetaDataEntry<C> entry) {
        return new CompositeImmutableSessionMetaData(entry.getCreationMetaDataEntry(), entry.getAccessMetaDataEntry());
    }

    @Override
    public boolean remove(String id) {
        this.accessMetaDataCache.withFlags(this.ignoreReturnFlags).remove(new SessionAccessMetaDataKey(id));
        this.creationMetaDataCache.withFlags(this.ignoreReturnFlags).remove(new SessionCreationMetaDataKey(id));
        return true;
    }
}
