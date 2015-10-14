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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
 * {@link SessionFactory} for coarse granularity sessions.
 * A given session is mapped to 3 co-located cache entries, one containing the static session meta-data and local context, one the dynamic session meta-data (updated every request),
 * and the other containing a map of session attributes.
 * @author Paul Ferraro
 */
public class CoarseSessionFactory<L> implements SessionFactory<CoarseSessionEntry<L>, L> {

    private final SessionContext context;
    private final Cache<SessionCreationMetaDataKey, SessionCreationMetaDataEntry<L>> creationMetaDataCache;
    private final Cache<SessionCreationMetaDataKey, SessionCreationMetaDataEntry<L>> findCreationMetaDataCache;
    private final Cache<SessionAccessMetaDataKey, SessionAccessMetaData> accessMetaDataCache;
    private final Cache<SessionAttributesKey, MarshalledValue<Map<String, Object>, MarshallingContext>> attributesCache;
    private final Marshaller<Map<String, Object>, MarshalledValue<Map<String, Object>, MarshallingContext>, MarshallingContext> marshaller;
    private final LocalContextFactory<L> localContextFactory;
    private final boolean requireMarshallable;

    @SuppressWarnings("unchecked")
    public CoarseSessionFactory(Cache<? extends Key<String>, ?> cache, SessionContext context, Marshaller<Map<String, Object>, MarshalledValue<Map<String, Object>, MarshallingContext>, MarshallingContext> marshaller, LocalContextFactory<L> localContextFactory, boolean lockOnRead, boolean requireMarshallable) {
        this.creationMetaDataCache = (Cache<SessionCreationMetaDataKey, SessionCreationMetaDataEntry<L>>) cache;
        this.findCreationMetaDataCache = lockOnRead ? this.creationMetaDataCache.getAdvancedCache().withFlags(Flag.FORCE_WRITE_LOCK) : this.creationMetaDataCache;
        this.accessMetaDataCache = (Cache<SessionAccessMetaDataKey, SessionAccessMetaData>) cache;
        this.attributesCache = (Cache<SessionAttributesKey, MarshalledValue<Map<String, Object>, MarshallingContext>>) cache;
        this.context = context;
        this.marshaller = marshaller;
        this.localContextFactory = localContextFactory;
        this.requireMarshallable = requireMarshallable;
    }

    @Override
    public Session<L> createSession(String id, CoarseSessionEntry<L> entry) {
        MutableCacheEntry<SessionCreationMetaData> creationMetaDataEntry = entry.getMutableSessionCreationMetaDataEntry();
        MutableCacheEntry<SessionAccessMetaData> accessMetaDataEntry = entry.getMutableSessionAccessMetaDataEntry();
        MutableCacheEntry<Map<String, Object>> attributesEntry = entry.getMutableAttributesEntry();

        SessionCreationMetaData creationMetaData = new MutableSessionCreationMetaData(creationMetaDataEntry.getValue(), creationMetaDataEntry.getMutator());
        SessionAccessMetaData accessMetaData = new MutableSessionAccessMetaData(accessMetaDataEntry.getValue(), accessMetaDataEntry.getMutator());
        SessionMetaData metaData = new SimpleSessionMetaData(creationMetaData, accessMetaData);
        SessionAttributes attributes = new CoarseSessionAttributes(attributesEntry.getValue(), attributesEntry.getMutator(), this.marshaller.getContext(), this.requireMarshallable);

        return new InfinispanSession<>(id, metaData, attributes, entry.getLocalContext(), this.localContextFactory, this.context, this);
    }

    @Override
    public ImmutableSession createImmutableSession(String id, CoarseSessionEntry<L> entry) {
        MutableCacheEntry<SessionCreationMetaData> creationMetaDataEntry = entry.getMutableSessionCreationMetaDataEntry();
        MutableCacheEntry<SessionAccessMetaData> accessMetaDataEntry = entry.getMutableSessionAccessMetaDataEntry();
        MutableCacheEntry<Map<String, Object>> attributesEntry = entry.getMutableAttributesEntry();

        ImmutableSessionMetaData metaData = new SimpleSessionMetaData(creationMetaDataEntry.getValue(), accessMetaDataEntry.getValue());
        ImmutableSessionAttributes attributes = new CoarseImmutableSessionAttributes(attributesEntry.getValue());

        return new InfinispanImmutableSession(id, metaData, attributes, this.context);
    }

    @Override
    public CoarseSessionEntry<L> createValue(String id, Void context) {
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

        SessionAttributesKey attributesKey = new SessionAttributesKey(id);
        Map<String, Object> attributes = new ConcurrentHashMap<>();
        MarshalledValue<Map<String, Object>, MarshallingContext> attributesValue = this.marshaller.write(attributes);
        Mutator attributesMutator = Mutator.PASSIVE;
        if (existingCreationMetaDataEntry == null) {
            this.attributesCache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).put(attributesKey, attributesValue);
        } else {
            MarshalledValue<Map<String, Object>, MarshallingContext> existingAttributesValue = this.attributesCache.getAdvancedCache().withFlags(Flag.FORCE_SYNCHRONOUS).putIfAbsent(attributesKey, attributesValue);
            if (existingAttributesValue != null) {
                try {
                    attributes = this.marshaller.read(existingAttributesValue);
                    attributesMutator = new CacheEntryMutator<>(this.attributesCache, attributesKey, existingAttributesValue);
                } catch (InvalidSerializedFormException e) {
                    InfinispanWebLogger.ROOT_LOGGER.failedToActivateSession(e, id);
                    // Invalidate
                    this.remove(id);
                    return this.createValue(id, context);
                }
            }
        }

        return new CoarseSessionEntry<>(new MutableCacheEntry<>(creationMetaDataEntry.getMetaData(), creationMetaDataMutator), new MutableCacheEntry<>(accessMetaData, accessMetaDataMutator), new MutableCacheEntry<>(attributes, attributesMutator), creationMetaDataEntry.getLocalContext());
    }

    @Override
    public CoarseSessionEntry<L> tryValue(String id) {
        return this.getValue(id, this.findCreationMetaDataCache.getAdvancedCache().withFlags(Flag.FAIL_SILENTLY));
    }

    @Override
    public CoarseSessionEntry<L> findValue(String id) {
        return this.getValue(id, this.findCreationMetaDataCache);
    }

    private CoarseSessionEntry<L> getValue(String id, Cache<SessionCreationMetaDataKey, SessionCreationMetaDataEntry<L>> creationMetaDataCache) {
        SessionCreationMetaDataKey creationMetaDataKey = new SessionCreationMetaDataKey(id);
        SessionCreationMetaDataEntry<L> creationMetaDataEntry = creationMetaDataCache.get(creationMetaDataKey);
        if (creationMetaDataEntry != null) {
            SessionAccessMetaDataKey accessMetaDataKey = new SessionAccessMetaDataKey(id);
            SessionAccessMetaData accessMetaData = this.accessMetaDataCache.get(accessMetaDataKey);
            if (accessMetaData != null) {
                SessionAttributesKey attributesKey = new SessionAttributesKey(id);
                MarshalledValue<Map<String, Object>, MarshallingContext> attributesValue = this.attributesCache.get(attributesKey);
                if (attributesValue != null) {
                    try {
                        Map<String, Object> attributes = this.marshaller.read(attributesValue);
                        Mutator creationMetaDataMutator = new CacheEntryMutator<>(this.creationMetaDataCache, creationMetaDataKey, creationMetaDataEntry);
                        Mutator accessMetaDataMutator = new CacheEntryMutator<>(this.accessMetaDataCache, accessMetaDataKey, accessMetaData);
                        Mutator attributesMutator = new CacheEntryMutator<>(this.attributesCache, attributesKey, attributesValue);
                        return new CoarseSessionEntry<>(new MutableCacheEntry<>(creationMetaDataEntry.getMetaData(), creationMetaDataMutator), new MutableCacheEntry<>(accessMetaData, accessMetaDataMutator), new MutableCacheEntry<>(attributes, attributesMutator), creationMetaDataEntry.getLocalContext());
                    } catch (InvalidSerializedFormException e) {
                        InfinispanWebLogger.ROOT_LOGGER.failedToActivateSession(e, id);
                        // Invalidate
                        this.remove(id);
                        return null;
                    }
                }
                // Purge orphaned entry
                this.accessMetaDataCache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).remove(accessMetaDataKey);
            }
            // Purge orphaned entry, making sure not to trigger cache listener
            this.creationMetaDataCache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES, Flag.SKIP_LISTENER_NOTIFICATION).remove(creationMetaDataKey);
        }
        return null;
    }

    @Override
    public void remove(String id) {
        this.creationMetaDataCache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).remove(new SessionCreationMetaDataKey(id));
        this.accessMetaDataCache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).remove(new SessionAccessMetaDataKey(id));
        this.attributesCache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).remove(new SessionAttributesKey(id));
    }

    @Override
    public void evict(String id) {
        try {
            this.creationMetaDataCache.evict(new SessionCreationMetaDataKey(id));
            this.accessMetaDataCache.evict(new SessionAccessMetaDataKey(id));
            this.attributesCache.evict(new SessionAttributesKey(id));
        } catch (Throwable e) {
            InfinispanWebLogger.ROOT_LOGGER.failedToPassivateSession(e, id);
        }
    }
}
