/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan.session.metadata;

import java.time.Duration;

import org.infinispan.Cache;
import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.ee.cache.CacheProperties;
import org.wildfly.clustering.ee.infinispan.CacheEntryComputeMutator;
import org.wildfly.clustering.ee.infinispan.InfinispanConfiguration;
import org.wildfly.clustering.web.cache.session.metadata.InvalidatableSessionMetaData;
import org.wildfly.clustering.web.cache.session.metadata.SessionMetaDataFactory;
import org.wildfly.clustering.web.cache.session.metadata.coarse.ContextualSessionMetaDataEntry;
import org.wildfly.clustering.web.cache.session.metadata.coarse.DefaultImmutableSessionMetaData;
import org.wildfly.clustering.web.cache.session.metadata.coarse.DefaultSessionMetaData;
import org.wildfly.clustering.web.cache.session.metadata.coarse.DefaultSessionMetaDataEntry;
import org.wildfly.clustering.web.cache.session.metadata.coarse.MutableSessionMetaDataEntry;
import org.wildfly.clustering.web.cache.session.metadata.coarse.MutableSessionMetaDataOffsetValues;
import org.wildfly.clustering.web.cache.session.metadata.coarse.SessionMetaDataEntryFunction;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;

/**
 * @author Paul Ferraro
 */
public class InfinispanSessionMetaDataFactory<L> implements SessionMetaDataFactory<ContextualSessionMetaDataEntry<L>> {

    private final Cache<SessionMetaDataKey, ContextualSessionMetaDataEntry<L>> cache;
    private final Cache<SessionMetaDataKey, ContextualSessionMetaDataEntry<L>> readForUpdateCache;
    private final Cache<SessionMetaDataKey, ContextualSessionMetaDataEntry<L>> tryReadForUpdateCache;
    private final Cache<SessionMetaDataKey, ContextualSessionMetaDataEntry<L>> writeOnlyCache;
    private final Cache<SessionMetaDataKey, ContextualSessionMetaDataEntry<L>> silentWriteCache;
    private final CacheProperties properties;

    public InfinispanSessionMetaDataFactory(InfinispanConfiguration configuration) {
        this.cache = configuration.getCache();
        this.readForUpdateCache = configuration.getReadForUpdateCache();
        this.tryReadForUpdateCache = configuration.getTryReadForUpdateCache();
        this.writeOnlyCache = configuration.getWriteOnlyCache();
        this.silentWriteCache = configuration.getSilentWriteCache();
        this.properties = configuration.getCacheProperties();
    }

    @Override
    public ContextualSessionMetaDataEntry<L> createValue(String id, Duration defaultTimeout) {
        DefaultSessionMetaDataEntry<L> entry = new DefaultSessionMetaDataEntry<>();
        entry.setTimeout(defaultTimeout);
        this.writeOnlyCache.put(new SessionMetaDataKey(id), entry);
        return entry;
    }

    @Override
    public ContextualSessionMetaDataEntry<L> findValue(String id) {
        return this.readForUpdateCache.get(new SessionMetaDataKey(id));
    }

    @Override
    public ContextualSessionMetaDataEntry<L> tryValue(String id) {
        return this.tryReadForUpdateCache.get(new SessionMetaDataKey(id));
    }

    @Override
    public boolean remove(String id) {
        this.writeOnlyCache.remove(new SessionMetaDataKey(id));
        return true;
    }

    @Override
    public boolean purge(String id) {
        this.silentWriteCache.remove(new SessionMetaDataKey(id));
        return true;
    }

    @Override
    public InvalidatableSessionMetaData createSessionMetaData(String id, ContextualSessionMetaDataEntry<L> entry) {
        MutableSessionMetaDataOffsetValues delta = this.properties.isTransactional() && entry.isNew() ? null : MutableSessionMetaDataOffsetValues.from(entry);
        Mutator mutator = (delta != null) ? new CacheEntryComputeMutator<>(this.cache, new SessionMetaDataKey(id), new SessionMetaDataEntryFunction<>(delta)) : Mutator.PASSIVE;
        return new DefaultSessionMetaData((delta != null) ? new MutableSessionMetaDataEntry(entry, delta) : entry, mutator);
    }

    @Override
    public ImmutableSessionMetaData createImmutableSessionMetaData(String id, ContextualSessionMetaDataEntry<L> entry) {
        return new DefaultImmutableSessionMetaData(entry);
    }
}
