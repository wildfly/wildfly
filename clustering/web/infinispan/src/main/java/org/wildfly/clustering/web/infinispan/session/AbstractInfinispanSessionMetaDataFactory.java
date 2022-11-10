/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.infinispan.session;

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
import org.wildfly.clustering.ee.infinispan.InfinispanMutatorFactory;
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
        this.creationMetaDataMutatorFactory = new InfinispanMutatorFactory<>(this.creationMetaDataCache, this.properties);
        this.accessMetaDataCache = configuration.getCache();
        this.accessMetaDataMutatorFactory = new InfinispanMutatorFactory<>(this.accessMetaDataCache, this.properties);
        this.evictListenerRegistration = new PostPassivateBlockingListener<>(this.creationMetaDataCache, this::cascadeEvict).register(SessionCreationMetaDataKey.class);
    }

    @Override
    public void close() {
        this.evictListenerRegistration.close();
    }

    @Override
    public CompositeSessionMetaDataEntry<L> createValue(String id, Void context) {
        Map<Key<String>, Object> entries = new HashMap<>(3);
        SessionCreationMetaDataEntry<L> creationMetaDataEntry = new SessionCreationMetaDataEntry<>(new SimpleSessionCreationMetaData());
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
        boolean newSession = entry.getCreationMetaData().isNew();

        SessionCreationMetaDataKey creationMetaDataKey = new SessionCreationMetaDataKey(id);
        Mutator creationMutator = this.properties.isTransactional() && newSession ? Mutator.PASSIVE : this.creationMetaDataMutatorFactory.createMutator(creationMetaDataKey, new SessionCreationMetaDataEntry<>(entry.getCreationMetaData(), entry.getLocalContext()));
        SessionCreationMetaData creationMetaData = new MutableSessionCreationMetaData(entry.getCreationMetaData(), creationMutator);

        SessionAccessMetaDataKey accessMetaDataKey = new SessionAccessMetaDataKey(id);
        Mutator accessMutator = this.properties.isTransactional() && newSession ? Mutator.PASSIVE : this.accessMetaDataMutatorFactory.createMutator(accessMetaDataKey, entry.getAccessMetaData());
        SessionAccessMetaData accessMetaData = new MutableSessionAccessMetaData(entry.getAccessMetaData(), accessMutator);

        return new CompositeSessionMetaData(creationMetaData, accessMetaData);
    }

    @Override
    public ImmutableSessionMetaData createImmutableSessionMetaData(String id, CompositeSessionMetaDataEntry<L> entry) {
        return new CompositeSessionMetaData(entry.getCreationMetaData(), entry.getAccessMetaData());
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
