/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan.session;

import java.time.Duration;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import jakarta.transaction.SystemException;

import org.infinispan.Cache;
import org.infinispan.commons.CacheException;
import org.infinispan.context.Flag;
import org.wildfly.clustering.ee.Key;
import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.ee.MutatorFactory;
import org.wildfly.clustering.ee.cache.CacheProperties;
import org.wildfly.clustering.ee.infinispan.InfinispanConfiguration;
import org.wildfly.clustering.ee.infinispan.CacheMutatorFactory;
import org.wildfly.clustering.infinispan.listener.ListenerRegistration;
import org.wildfly.clustering.infinispan.listener.PostPassivateBlockingListener;
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
import org.wildfly.clustering.web.cache.session.SimpleSessionCreationMetaData;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;

/**
 * Abstract {@link org.wildfly.clustering.web.cache.session.SessionMetaDataFactory} implementation that stores session meta-data in 2 distinct cache entries:
 * <dl>
 * <dt>Creation meta-data</dt>
 * <dd>Meta data that is usually determined on session creation, and is rarely or never updated</dd>
 * <dt>Access meta-data</dt>
 * <dd>Meta data that is updated often, typically every request</dd>
 * </dl>
 * @author Paul Ferraro
 */
public abstract class AbstractInfinispanSessionMetaDataFactory<L> implements SessionMetaDataFactory<CompositeSessionMetaDataEntry<L>>, BiFunction<String, Set<Flag>, CompositeSessionMetaDataEntry<L>> {

    private static final Set<Flag> TRY_LOCK_FLAGS = EnumSet.of(Flag.ZERO_LOCK_ACQUISITION_TIMEOUT, Flag.FAIL_SILENTLY);

    private final Cache<Key<String>, Object> writeOnlyCache;
    private final Cache<Key<String>, Object> silentWriteCache;
    private final Cache<SessionCreationMetaDataKey, SessionCreationMetaDataEntry<L>> creationMetaDataCache;
    private final Cache<SessionCreationMetaDataKey, SessionCreationMetaDataEntry<L>> creationMetaDataTryLockCache;
    private final MutatorFactory<SessionCreationMetaDataKey, SessionCreationMetaDataEntry<L>> creationMetaDataMutatorFactory;
    private final Cache<SessionAccessMetaDataKey, SessionAccessMetaData> accessMetaDataCache;
    private final MutatorFactory<SessionAccessMetaDataKey, SessionAccessMetaData> accessMetaDataMutatorFactory;
    private final CacheProperties properties;
    private final ListenerRegistration evictListenerRegistration;

    public AbstractInfinispanSessionMetaDataFactory(InfinispanConfiguration configuration) {
        this.writeOnlyCache = configuration.getWriteOnlyCache();
        this.silentWriteCache = configuration.getSilentWriteCache();
        this.creationMetaDataTryLockCache = configuration.getTryLockCache();
        this.properties = configuration.getCacheProperties();
        this.creationMetaDataCache = configuration.getCache();
        this.creationMetaDataMutatorFactory = new CacheMutatorFactory<>(this.creationMetaDataCache, this.properties);
        this.accessMetaDataCache = configuration.getCache();
        this.accessMetaDataMutatorFactory = new CacheMutatorFactory<>(this.accessMetaDataCache, this.properties);
        this.evictListenerRegistration = new PostPassivateBlockingListener<>(this.creationMetaDataCache, this::cascadeEvict).register(SessionCreationMetaDataKey.class);
    }

    @Override
    public void close() {
        this.evictListenerRegistration.close();
    }

    @Override
    public CompositeSessionMetaDataEntry<L> createValue(String id, Duration defaultTimeout) {
        Map<Key<String>, Object> entries = new HashMap<>(3);
        SessionCreationMetaData creationMetaData = new SimpleSessionCreationMetaData();
        creationMetaData.setTimeout(defaultTimeout);
        SessionCreationMetaDataEntry<L> creationMetaDataEntry = new SessionCreationMetaDataEntry<>(creationMetaData);
        entries.put(new SessionCreationMetaDataKey(id), creationMetaDataEntry);
        SessionAccessMetaData accessMetaData = new SimpleSessionAccessMetaData();
        entries.put(new SessionAccessMetaDataKey(id), accessMetaData);
        this.writeOnlyCache.putAll(entries);
        return new CompositeSessionMetaDataEntry<>(creationMetaDataEntry, accessMetaData);
    }

    @Override
    public CompositeSessionMetaDataEntry<L> findValue(String id) {
        return this.apply(id, EnumSet.noneOf(Flag.class));
    }

    @Override
    public CompositeSessionMetaDataEntry<L> tryValue(String id) {
        return this.apply(id, TRY_LOCK_FLAGS);
    }

    @Override
    public InvalidatableSessionMetaData createSessionMetaData(String id, CompositeSessionMetaDataEntry<L> entry) {
        SessionCreationMetaDataEntry<L> creationMetaDataEntry = entry.getCreationMetaData();
        SessionCreationMetaData creationMetaData = creationMetaDataEntry.getMetaData();
        boolean newSession = creationMetaData.isNew();

        SessionCreationMetaDataKey creationMetaDataKey = new SessionCreationMetaDataKey(id);
        Mutator creationMutator = this.properties.isTransactional() && newSession ? Mutator.PASSIVE : this.creationMetaDataMutatorFactory.createMutator(creationMetaDataKey, creationMetaDataEntry);
        SessionCreationMetaData mutableCreationMetaData = new MutableSessionCreationMetaData(creationMetaData, creationMutator);

        SessionAccessMetaDataKey accessMetaDataKey = new SessionAccessMetaDataKey(id);
        Mutator accessMutator = this.properties.isTransactional() && newSession ? Mutator.PASSIVE : this.accessMetaDataMutatorFactory.createMutator(accessMetaDataKey, entry.getAccessMetaData());
        SessionAccessMetaData mutableAccessMetaData = new MutableSessionAccessMetaData(entry.getAccessMetaData(), accessMutator);

        return new CompositeSessionMetaData(mutableCreationMetaData, mutableAccessMetaData);
    }

    @Override
    public ImmutableSessionMetaData createImmutableSessionMetaData(String id, CompositeSessionMetaDataEntry<L> entry) {
        return new CompositeSessionMetaData(entry.getCreationMetaData().getMetaData(), entry.getAccessMetaData());
    }

    @Override
    public boolean remove(String id) {
        SessionCreationMetaDataKey key = new SessionCreationMetaDataKey(id);
        try {
            if (!this.properties.isLockOnWrite() || (this.creationMetaDataCache.getAdvancedCache().getTransactionManager().getTransaction() == null) || this.creationMetaDataTryLockCache.getAdvancedCache().lock(key)) {
                return delete(this.writeOnlyCache, id);
            }
            return false;
        } catch (SystemException e) {
            throw new CacheException(e);
        }
    }

    @Override
    public boolean purge(String id) {
        return delete(this.silentWriteCache, id);
    }

    private static boolean delete(Cache<Key<String>, Object> cache, String id) {
        cache.remove(new SessionAccessMetaDataKey(id));
        cache.remove(new SessionCreationMetaDataKey(id));
        return true;
    }

    void cascadeEvict(SessionCreationMetaDataKey key) {
        this.silentWriteCache.evict(new SessionAccessMetaDataKey(key.getId()));
    }
}
