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
package org.wildfly.clustering.web.infinispan.session.fine;

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
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionAttributes;
import org.wildfly.clustering.web.session.SessionContext;
import org.wildfly.clustering.web.session.SessionMetaData;

/**
 * {@link SessionFactory} for fine granularity sessions.
 * A given session is mapped to N+1 co-located cache entries, where N is the number of session attributes.
 * One cache entry containing the session meta data, local context, and the set of attribute names;
 * and one cache entry per session attribute.
 * @author Paul Ferraro
 */
public class FineSessionFactory<L> implements SessionFactory<MutableCacheEntry<FineSessionCacheEntry<L>>, L> {

    private final Cache<String, FineSessionCacheEntry<L>> sessionCache;
    private final Cache<SessionAttributeCacheKey, MarshalledValue<Object, MarshallingContext>> attributeCache;
    private final SessionContext context;
    private final Marshaller<Object, MarshalledValue<Object, MarshallingContext>> marshaller;
    private final LocalContextFactory<L> localContextFactory;

    public FineSessionFactory(Cache<String, FineSessionCacheEntry<L>> sessionCache, Cache<SessionAttributeCacheKey, MarshalledValue<Object, MarshallingContext>> attributeCache, SessionContext context, Marshaller<Object, MarshalledValue<Object, MarshallingContext>> marshaller, LocalContextFactory<L> localContextFactory) {
        this.sessionCache = sessionCache;
        this.attributeCache = attributeCache;
        this.context = context;
        this.marshaller = marshaller;
        this.localContextFactory = localContextFactory;
    }

    @Override
    public Session<L> createSession(String id, MutableCacheEntry<FineSessionCacheEntry<L>> entry) {
        FineSessionCacheEntry<L> sessionEntry = entry.getValue();
        SessionMetaData metaData = sessionEntry.getMetaData();
        SessionAttributes attributes = new FineSessionAttributes<>(id, this.attributeCache, this.marshaller);
        return new InfinispanSession<>(id, metaData, attributes, sessionEntry.getLocalContext(), this.localContextFactory, this.context, entry.getMutator(), this);
    }

    @Override
    public ImmutableSession createImmutableSession(String id, MutableCacheEntry<FineSessionCacheEntry<L>> entry) {
        ImmutableSessionAttributes attributes = new FineImmutableSessionAttributes<>(id, this.attributeCache, this.marshaller);
        return new InfinispanImmutableSession(id, entry.getValue().getMetaData(), attributes, this.context);
    }

    @Override
    public MutableCacheEntry<FineSessionCacheEntry<L>> findValue(String id) {
        TransactionConfiguration transaction = this.sessionCache.getCacheConfiguration().transaction();
        boolean pessimistic = transaction.transactionMode().isTransactional() && (transaction.lockingMode() == LockingMode.PESSIMISTIC);
        Cache<String, FineSessionCacheEntry<L>> cache = pessimistic ? this.sessionCache.getAdvancedCache().withFlags(Flag.FORCE_WRITE_LOCK) : this.sessionCache;
        FineSessionCacheEntry<L> value = cache.get(id);
        if (value == null) return null;
        // Preemptively read all attributes to detect invalid session attributes
        for (Map.Entry<SessionAttributeCacheKey, MarshalledValue<Object, MarshallingContext>> entry : this.attributeCache.getAdvancedCache().getGroup(id).entrySet()) {
            try {
                this.marshaller.read(entry.getValue());
            } catch (InvalidSerializedFormException e) {
                InfinispanWebLogger.ROOT_LOGGER.failedToActivateSessionAttribute(e, id, entry.getKey().getAttribute());
                this.remove(id);
                return null;
            }
        }
        return new MutableCacheEntry<>(value, new CacheEntryMutator<>(this.sessionCache, id, value));
    }

    @Override
    public MutableCacheEntry<FineSessionCacheEntry<L>> createValue(String id, Void context) {
        FineSessionCacheEntry<L> value = new FineSessionCacheEntry<>(new SimpleSessionMetaData());
        FineSessionCacheEntry<L> existing = this.sessionCache.getAdvancedCache().withFlags(Flag.FORCE_SYNCHRONOUS).putIfAbsent(id, value);
        if (existing == null) {
            return new MutableCacheEntry<>(value, Mutator.PASSIVE);
        }
        // Preemptively read all attributes to detect invalid session attributes
        for (Map.Entry<SessionAttributeCacheKey, MarshalledValue<Object, MarshallingContext>> entry : this.attributeCache.getAdvancedCache().getGroup(id).entrySet()) {
            try {
                this.marshaller.read(entry.getValue());
            } catch (InvalidSerializedFormException e) {
                InfinispanWebLogger.ROOT_LOGGER.failedToActivateSessionAttribute(e, id, entry.getKey().getAttribute());
                this.remove(id);
                return this.createValue(id, context);
            }
        }
        return new MutableCacheEntry<>(existing, new CacheEntryMutator<>(this.sessionCache, id, existing));
    }

    @Override
    public void remove(final String id) {
        this.sessionCache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).remove(id);
        this.attributeCache.getAdvancedCache().removeGroup(id);
    }

    @Override
    public void evict(final String id) {
        for (SessionAttributeCacheKey key: this.attributeCache.getAdvancedCache().getGroup(id).keySet()) {
            try {
                this.attributeCache.evict(key);
            } catch (Throwable e) {
                InfinispanWebLogger.ROOT_LOGGER.failedToPassivateSessionAttribute(e, id, key.getAttribute());
            }
        }
        try {
            this.sessionCache.evict(id);
        } catch (Throwable e) {
            InfinispanWebLogger.ROOT_LOGGER.failedToPassivateSession(e, id);
        }
    }
}
