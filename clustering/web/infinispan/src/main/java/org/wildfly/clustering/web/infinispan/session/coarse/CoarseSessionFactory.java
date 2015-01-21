/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.web.infinispan.session.coarse;

import java.util.HashMap;
import java.util.Map;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.TransactionConfiguration;
import org.infinispan.context.Flag;
import org.infinispan.transaction.LockingMode;
import org.wildfly.clustering.ee.infinispan.CacheEntryMutator;
import org.wildfly.clustering.ee.infinispan.MutableCacheEntry;
import org.wildfly.clustering.ee.infinispan.Mutator;
import org.wildfly.clustering.marshalling.InvalidSerializedFormException;
import org.wildfly.clustering.marshalling.MarshalledValue;
import org.wildfly.clustering.marshalling.Marshaller;
import org.wildfly.clustering.marshalling.MarshallingContext;
import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.infinispan.logging.InfinispanWebLogger;
import org.wildfly.clustering.web.infinispan.session.InfinispanImmutableSession;
import org.wildfly.clustering.web.infinispan.session.InfinispanSession;
import org.wildfly.clustering.web.infinispan.session.SessionFactory;
import org.wildfly.clustering.web.infinispan.session.SimpleSessionMetaData;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionAttributes;
import org.wildfly.clustering.web.session.SessionContext;
import org.wildfly.clustering.web.session.SessionMetaData;

/**
 * {@link SessionFactory} for coarse granularity sessions.
 * A given session is mapped to 2 co-located cache entries, one containing the session meta data and local context (updated every request)
 * and the other containing the map of session attributes.
 * @author Paul Ferraro
 */
public class CoarseSessionFactory<L> implements SessionFactory<CoarseSessionEntry<L>, L> {

    private final SessionContext context;
    private final Cache<String, CoarseSessionCacheEntry<L>> sessionCache;
    private final Cache<SessionAttributesCacheKey, MarshalledValue<Map<String, Object>, MarshallingContext>> attributesCache;
    private final Marshaller<Map<String, Object>, MarshalledValue<Map<String, Object>, MarshallingContext>> marshaller;
    private final LocalContextFactory<L> localContextFactory;

    public CoarseSessionFactory(Cache<String, CoarseSessionCacheEntry<L>> sessionCache, Cache<SessionAttributesCacheKey, MarshalledValue<Map<String, Object>, MarshallingContext>> attributesCache, SessionContext context, Marshaller<Map<String, Object>, MarshalledValue<Map<String, Object>, MarshallingContext>> marshaller, LocalContextFactory<L> localContextFactory) {
        this.sessionCache = sessionCache;
        this.attributesCache = attributesCache;
        this.context = context;
        this.marshaller = marshaller;
        this.localContextFactory = localContextFactory;
    }

    @Override
    public Session<L> createSession(String id, CoarseSessionEntry<L> entry) {
        MutableCacheEntry<CoarseSessionCacheEntry<L>> sessionEntry = entry.getMutableSessionEntry();
        MutableCacheEntry<Map<String, Object>> attributesEntry = entry.getMutableAttributesEntry();
        SessionMetaData metaData = sessionEntry.getValue().getMetaData();
        SessionAttributes attributes = new CoarseSessionAttributes(attributesEntry.getValue(), attributesEntry.getMutator());
        return new InfinispanSession<>(id, metaData, attributes, sessionEntry.getValue().getLocalContext(), this.localContextFactory, this.context, sessionEntry.getMutator(), this);
    }

    @Override
    public ImmutableSession createImmutableSession(String id, CoarseSessionEntry<L> entry) {
        MutableCacheEntry<CoarseSessionCacheEntry<L>> sessionEntry = entry.getMutableSessionEntry();
        MutableCacheEntry<Map<String, Object>> attributesEntry = entry.getMutableAttributesEntry();
        ImmutableSessionMetaData metaData = sessionEntry.getValue().getMetaData();
        ImmutableSessionAttributes attributes = new CoarseImmutableSessionAttributes(attributesEntry.getValue());
        return new InfinispanImmutableSession(id, metaData, attributes, this.context);
    }

    @Override
    public CoarseSessionEntry<L> createValue(String id, Void context) {
        CoarseSessionCacheEntry<L> entry = new CoarseSessionCacheEntry<>(new SimpleSessionMetaData());
        CoarseSessionCacheEntry<L> existingEntry = this.sessionCache.getAdvancedCache().withFlags(Flag.FORCE_SYNCHRONOUS).putIfAbsent(id, entry);
        SessionAttributesCacheKey key = new SessionAttributesCacheKey(id);
        Map<String, Object> attributes = new HashMap<>();
        MutableCacheEntry<Map<String, Object>> attributesEntry = new MutableCacheEntry<>(attributes, Mutator.PASSIVE);
        MarshalledValue<Map<String, Object>, MarshallingContext> value = this.marshaller.write(attributes);
        if (existingEntry == null) {
            this.attributesCache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).put(key, value);
            MutableCacheEntry<CoarseSessionCacheEntry<L>> sessionEntry = new MutableCacheEntry<>(entry, Mutator.PASSIVE);
            return new CoarseSessionEntry<>(sessionEntry, attributesEntry);
        }
        MutableCacheEntry<CoarseSessionCacheEntry<L>> sessionEntry = new MutableCacheEntry<>(existingEntry, new CacheEntryMutator<>(this.sessionCache, id, existingEntry));
        MarshalledValue<Map<String, Object>, MarshallingContext> existingValue = this.attributesCache.getAdvancedCache().withFlags(Flag.FORCE_SYNCHRONOUS).putIfAbsent(key, value);
        if (existingValue != null) {
            try {
                Map<String, Object> existingAttributes = this.marshaller.read(existingValue);
                MutableCacheEntry<Map<String, Object>> existingAttributesEntry = new MutableCacheEntry<>(existingAttributes, new CacheEntryMutator<>(this.attributesCache, key, existingValue));
                return new CoarseSessionEntry<>(sessionEntry, existingAttributesEntry);
            } catch (InvalidSerializedFormException e) {
                InfinispanWebLogger.ROOT_LOGGER.failedToActivateSession(e, id);
                new CacheEntryMutator<>(this.attributesCache, key, value).mutate();
            }
        }
        return new CoarseSessionEntry<>(sessionEntry, attributesEntry);
    }

    @Override
    public CoarseSessionEntry<L> findValue(String id) {
        TransactionConfiguration transaction = this.sessionCache.getCacheConfiguration().transaction();
        boolean pessimistic = transaction.transactionMode().isTransactional() && (transaction.lockingMode() == LockingMode.PESSIMISTIC);
        Cache<String, CoarseSessionCacheEntry<L>> cache = pessimistic ? this.sessionCache.getAdvancedCache().withFlags(Flag.FORCE_WRITE_LOCK) : this.sessionCache;
        CoarseSessionCacheEntry<L> entry = cache.get(id);
        if (entry != null) {
            MutableCacheEntry<CoarseSessionCacheEntry<L>> sessionEntry = new MutableCacheEntry<>(entry, new CacheEntryMutator<>(this.sessionCache, id, entry));
            SessionAttributesCacheKey key = new SessionAttributesCacheKey(id);
            MarshalledValue<Map<String, Object>, MarshallingContext> value = this.attributesCache.get(key);
            if (value != null) {
                try {
                    Map<String, Object> attributes = this.marshaller.read(value);
                    return new CoarseSessionEntry<>(sessionEntry, new MutableCacheEntry<>(attributes, new CacheEntryMutator<>(this.attributesCache, key, value)));
                } catch (InvalidSerializedFormException e) {
                    InfinispanWebLogger.ROOT_LOGGER.failedToActivateSession(e, id);
                }
            }
            this.remove(id);
        }
        return null;
    }

    @Override
    public void remove(String id) {
        this.sessionCache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).remove(id);
        this.attributesCache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).remove(new SessionAttributesCacheKey(id));
    }

    @Override
    public void evict(String id) {
        try {
            this.sessionCache.evict(id);
            this.attributesCache.evict(new SessionAttributesCacheKey(id));
        } catch (Throwable e) {
            InfinispanWebLogger.ROOT_LOGGER.failedToPassivateSession(e, id);
        }
    }
}
