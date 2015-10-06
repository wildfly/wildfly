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
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.wildfly.clustering.ee.infinispan.CacheEntryMutator;
import org.wildfly.clustering.ee.infinispan.MutableCacheEntry;
import org.wildfly.clustering.ee.infinispan.Mutator;
import org.wildfly.clustering.infinispan.spi.distribution.Key;
import org.wildfly.clustering.marshalling.jboss.InvalidSerializedFormException;
import org.wildfly.clustering.marshalling.jboss.MarshalledValue;
import org.wildfly.clustering.marshalling.jboss.Marshaller;
import org.wildfly.clustering.marshalling.jboss.MarshallingContext;
import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.infinispan.logging.InfinispanWebLogger;
import org.wildfly.clustering.web.infinispan.session.InfinispanImmutableSession;
import org.wildfly.clustering.web.infinispan.session.InfinispanSession;
import org.wildfly.clustering.web.infinispan.session.MutableSessionAccessMetaData;
import org.wildfly.clustering.web.infinispan.session.MutableSessionCreationMetaData;
import org.wildfly.clustering.web.infinispan.session.SessionAccessMetaData;
import org.wildfly.clustering.web.infinispan.session.SessionAccessMetaDataKey;
import org.wildfly.clustering.web.infinispan.session.SessionCreationMetaData;
import org.wildfly.clustering.web.infinispan.session.SessionCreationMetaDataEntry;
import org.wildfly.clustering.web.infinispan.session.SessionCreationMetaDataKey;
import org.wildfly.clustering.web.infinispan.session.SessionFactory;
import org.wildfly.clustering.web.infinispan.session.SimpleSessionAccessMetaData;
import org.wildfly.clustering.web.infinispan.session.SimpleSessionCreationMetaData;
import org.wildfly.clustering.web.infinispan.session.SimpleSessionMetaData;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionAttributes;
import org.wildfly.clustering.web.session.SessionContext;
import org.wildfly.clustering.web.session.SessionMetaData;

/**
 * {@link SessionFactory} for fine granularity sessions.
 * A given session is mapped to N+2 co-located cache entries, where N is the number of session attributes:
 * One cache entry containing the static session meta data and local context, one containing the dynamic session meta data,
 * and one cache entry per session attribute.
 * @author Paul Ferraro
 */
public class FineSessionFactory<L> implements SessionFactory<FineSessionEntry<L>, L> {

    private final Cache<SessionCreationMetaDataKey, SessionCreationMetaDataEntry<L>> creationMetaDataCache;
    private final Cache<SessionCreationMetaDataKey, SessionCreationMetaDataEntry<L>> findCreationMetaDataCache;
    private final Cache<SessionAccessMetaDataKey, SessionAccessMetaData> accessMetaDataCache;
    private final Cache<SessionAttributeKey, MarshalledValue<Object, MarshallingContext>> attributeCache;
    private final SessionContext context;
    private final Marshaller<Object, MarshalledValue<Object, MarshallingContext>, MarshallingContext> marshaller;
    private final LocalContextFactory<L> localContextFactory;
    private final Predicate<Map.Entry<SessionAttributeKey, MarshalledValue<Object, MarshallingContext>>> invalidAttribute;

    @SuppressWarnings("unchecked")
    public FineSessionFactory(Cache<? extends Key<String>, ?> cache, SessionContext context, Marshaller<Object, MarshalledValue<Object, MarshallingContext>, MarshallingContext> marshaller, LocalContextFactory<L> localContextFactory, boolean lockOnRead) {
        this.creationMetaDataCache = (Cache<SessionCreationMetaDataKey, SessionCreationMetaDataEntry<L>>) cache;
        this.findCreationMetaDataCache = lockOnRead ? this.creationMetaDataCache.getAdvancedCache().withFlags(Flag.FORCE_WRITE_LOCK) : this.creationMetaDataCache;
        this.accessMetaDataCache = (Cache<SessionAccessMetaDataKey, SessionAccessMetaData>) cache;
        this.attributeCache = (Cache<SessionAttributeKey, MarshalledValue<Object, MarshallingContext>>) cache;
        this.context = context;
        this.marshaller = marshaller;
        this.localContextFactory = localContextFactory;
        this.invalidAttribute = entry -> {
            try {
                this.marshaller.read(entry.getValue());
                return false;
            } catch (InvalidSerializedFormException e) {
                InfinispanWebLogger.ROOT_LOGGER.failedToActivateSessionAttribute(e, entry.getKey().getValue(), entry.getKey().getAttribute());
                return true;
            }
        };
    }

    @Override
    public Session<L> createSession(String id, FineSessionEntry<L> entry) {
        MutableCacheEntry<SessionCreationMetaData> creationMetaDataEntry = entry.getMutableSessionCreationMetaDataEntry();
        MutableCacheEntry<SessionAccessMetaData> accessMetaDataEntry = entry.getMutableSessionAccessMetaDataEntry();

        SessionCreationMetaData creationMetaData = new MutableSessionCreationMetaData(creationMetaDataEntry.getValue(), creationMetaDataEntry.getMutator());
        SessionAccessMetaData accessMetaData = new MutableSessionAccessMetaData(accessMetaDataEntry.getValue(), accessMetaDataEntry.getMutator());
        SessionMetaData metaData = new SimpleSessionMetaData(creationMetaData, accessMetaData);

        SessionAttributes attributes = new FineSessionAttributes<>(id, this.attributeCache, this.marshaller);
        return new InfinispanSession<>(id, metaData, attributes, entry.getLocalContext(), this.localContextFactory, this.context, this);
    }

    @Override
    public ImmutableSession createImmutableSession(String id, FineSessionEntry<L> entry) {
        SessionCreationMetaData creationMetaData = entry.getMutableSessionCreationMetaDataEntry().getValue();
        SessionAccessMetaData accessMetaData = entry.getMutableSessionAccessMetaDataEntry().getValue();
        ImmutableSessionMetaData metaData = new SimpleSessionMetaData(creationMetaData, accessMetaData);
        ImmutableSessionAttributes attributes = new FineImmutableSessionAttributes<>(id, this.attributeCache, this.marshaller);

        return new InfinispanImmutableSession(id, metaData, attributes, this.context);
    }

    @Override
    public FineSessionEntry<L> createValue(String id, Void context) {
        SessionCreationMetaDataKey creationMetaDataKey = new SessionCreationMetaDataKey(id);
        SessionCreationMetaDataEntry<L> creationMetaDataEntry = new SessionCreationMetaDataEntry<>(new SimpleSessionCreationMetaData());
        SessionCreationMetaDataEntry<L> existingCreationMetaDataEntry = this.creationMetaDataCache.getAdvancedCache().withFlags(Flag.FORCE_SYNCHRONOUS).putIfAbsent(creationMetaDataKey, creationMetaDataEntry);
        Mutator creationMetaDataMutator = Mutator.PASSIVE;
        if (existingCreationMetaDataEntry != null) {
            creationMetaDataEntry = existingCreationMetaDataEntry;
            creationMetaDataMutator = new CacheEntryMutator<>(this.creationMetaDataCache, creationMetaDataKey, creationMetaDataEntry);
        }

        SessionAccessMetaDataKey accessMetaDataKey = new SessionAccessMetaDataKey(id);
        SessionAccessMetaData accessMetaData = new SimpleSessionAccessMetaData();
        Mutator accessMetaDataMutator = Mutator.PASSIVE;
        if (existingCreationMetaDataEntry == null) {
            this.accessMetaDataCache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).put(accessMetaDataKey, accessMetaData);
        } else {
            SessionAccessMetaData existingAccessMetaData = this.accessMetaDataCache.getAdvancedCache().withFlags(Flag.FORCE_SYNCHRONOUS).putIfAbsent(accessMetaDataKey, accessMetaData);
            if (existingAccessMetaData != null) {
                accessMetaData = existingAccessMetaData;
                accessMetaDataMutator = new CacheEntryMutator<>(this.accessMetaDataCache, accessMetaDataKey, accessMetaData);
            }
        }

        if (existingCreationMetaDataEntry != null) {
            // Preemptively read all attributes to detect invalid session attributes
            if (this.attributeCache.getAdvancedCache().getGroup(id).entrySet().stream().filter(entry -> ((Map.Entry<?, ?>) entry).getKey() instanceof SessionAttributeKey).anyMatch(this.invalidAttribute)) {
                this.remove(id);
                return this.createValue(id, context);
            }
        }

        return new FineSessionEntry<>(new MutableCacheEntry<>(creationMetaDataEntry.getMetaData(), creationMetaDataMutator), new MutableCacheEntry<>(accessMetaData, accessMetaDataMutator), creationMetaDataEntry.getLocalContext());
    }

    @Override
    public FineSessionEntry<L> tryValue(String id) {
        return this.getValue(id, this.findCreationMetaDataCache.getAdvancedCache().withFlags(Flag.FAIL_SILENTLY));
    }

    @Override
    public FineSessionEntry<L> findValue(String id) {
        return this.getValue(id, this.findCreationMetaDataCache);
    }

    public FineSessionEntry<L> getValue(String id, Cache<SessionCreationMetaDataKey, SessionCreationMetaDataEntry<L>> creationMetaDataCache) {
        SessionCreationMetaDataKey creationMetaDataKey = new SessionCreationMetaDataKey(id);
        SessionCreationMetaDataEntry<L> creationMetaDataEntry = creationMetaDataCache.get(creationMetaDataKey);
        if (creationMetaDataEntry != null) {
            Mutator creationMetaDataMutator = new CacheEntryMutator<>(this.creationMetaDataCache, creationMetaDataKey, creationMetaDataEntry);
            SessionAccessMetaDataKey accessMetaDataKey = new SessionAccessMetaDataKey(id);
            SessionAccessMetaData accessMetaData = this.accessMetaDataCache.get(accessMetaDataKey);
            if (accessMetaData != null) {
                Mutator accessMetaDataMutator = new CacheEntryMutator<>(this.accessMetaDataCache, accessMetaDataKey, accessMetaData);
                // Preemptively read all attributes to detect invalid session attributes
                if (this.attributeCache.getAdvancedCache().getGroup(id).entrySet().stream().filter(entry -> ((Map.Entry<?, ?>) entry).getKey() instanceof SessionAttributeKey).anyMatch(this.invalidAttribute)) {
                    // Invalidate
                    this.remove(id);
                    return null;
                }

                return new FineSessionEntry<>(new MutableCacheEntry<>(creationMetaDataEntry.getMetaData(), creationMetaDataMutator), new MutableCacheEntry<>(accessMetaData, accessMetaDataMutator), creationMetaDataEntry.getLocalContext());
            }
            // Remove orphaned entries
            this.attributeCache.getAdvancedCache().withFlags(Flag.SKIP_LISTENER_NOTIFICATION).removeGroup(id);
        }
        return null;
    }

    @Override
    public void remove(final String id) {
        this.creationMetaDataCache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).remove(new SessionCreationMetaDataKey(id));
        this.accessMetaDataCache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).remove(new SessionAccessMetaDataKey(id));
        this.attributeCache.getAdvancedCache().removeGroup(id);
    }

    @Override
    public void evict(final String id) {
        try {
            this.creationMetaDataCache.evict(new SessionCreationMetaDataKey(id));
            this.accessMetaDataCache.evict(new SessionAccessMetaDataKey(id));

            Consumer<SessionAttributeKey> evictor = key -> {
                try {
                    this.attributeCache.evict(key);
                } catch (Throwable e) {
                    InfinispanWebLogger.ROOT_LOGGER.failedToPassivateSessionAttribute(e, id, key.getAttribute());
                }
            };
            this.attributeCache.getAdvancedCache().withFlags(Flag.SKIP_CACHE_LOAD).getGroup(id).keySet().stream().filter((Object key) -> key instanceof SessionAttributeKey).forEach(evictor);
        } catch (Throwable e) {
            InfinispanWebLogger.ROOT_LOGGER.failedToPassivateSession(e, id);
        }
    }
}
