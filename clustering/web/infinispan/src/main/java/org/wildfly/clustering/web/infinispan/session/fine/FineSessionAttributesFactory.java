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

package org.wildfly.clustering.web.infinispan.session.fine;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntriesEvicted;
import org.infinispan.notifications.cachelistener.event.CacheEntriesEvictedEvent;
import org.wildfly.clustering.ee.Immutability;
import org.wildfly.clustering.ee.MutatorFactory;
import org.wildfly.clustering.ee.cache.CacheProperties;
import org.wildfly.clustering.ee.infinispan.GroupedKey;
import org.wildfly.clustering.ee.infinispan.InfinispanMutatorFactory;
import org.wildfly.clustering.marshalling.spi.Marshaller;
import org.wildfly.clustering.web.cache.session.CompositeImmutableSession;
import org.wildfly.clustering.web.cache.session.ImmutableSessionAttributeActivationNotifier;
import org.wildfly.clustering.web.cache.session.SessionAttributeActivationNotifier;
import org.wildfly.clustering.web.cache.session.SessionAttributes;
import org.wildfly.clustering.web.cache.session.SessionAttributesFactory;
import org.wildfly.clustering.web.cache.session.fine.FineImmutableSessionAttributes;
import org.wildfly.clustering.web.cache.session.fine.FineSessionAttributes;
import org.wildfly.clustering.web.infinispan.logging.InfinispanWebLogger;
import org.wildfly.clustering.web.infinispan.session.InfinispanSessionAttributesFactoryConfiguration;
import org.wildfly.clustering.web.infinispan.session.SessionCreationMetaDataKey;
import org.wildfly.clustering.web.session.HttpSessionActivationListenerProvider;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;

/**
 * {@link SessionAttributesFactory} for fine granularity sessions.
 * A given session's attributes are mapped to N+1 co-located cache entries, where N is the number of session attributes.
 * A separate cache entry stores the activate attribute names for the session.
 * @author Paul Ferraro
 */
@Listener(sync = false)
public class FineSessionAttributesFactory<S, C, L, V> implements SessionAttributesFactory<C, AtomicReference<Map<String, UUID>>> {

    private final Cache<SessionAttributeNamesKey, Map<String, UUID>> namesCache;
    private final Cache<SessionAttributeKey, V> attributeCache;
    private final Marshaller<Object, V> marshaller;
    private final Immutability immutability;
    private final CacheProperties properties;
    private final MutatorFactory<SessionAttributeKey, V> mutatorFactory;
    private final HttpSessionActivationListenerProvider<S, C, L> provider;

    public FineSessionAttributesFactory(InfinispanSessionAttributesFactoryConfiguration<S, C, L, Object, V> configuration) {
        this.namesCache = configuration.getCache();
        this.attributeCache = configuration.getCache();
        this.marshaller = configuration.getMarshaller();
        this.immutability = configuration.getImmutability();
        this.properties = configuration.getCacheProperties();
        this.mutatorFactory = new InfinispanMutatorFactory<>(this.attributeCache, this.properties);
        this.provider = configuration.getHttpSessionActivationListenerProvider();
    }

    @Override
    public AtomicReference<Map<String, UUID>> createValue(String id, Void context) {
        return new AtomicReference<>(Collections.emptyMap());
    }

    @Override
    public AtomicReference<Map<String, UUID>> findValue(String id) {
        return this.getValue(id, true);
    }

    @Override
    public AtomicReference<Map<String, UUID>> tryValue(String id) {
        return this.getValue(id, false);
    }

    private AtomicReference<Map<String, UUID>> getValue(String id, boolean purgeIfInvalid) {
        Map<String, UUID> names = this.namesCache.get(new SessionAttributeNamesKey(id));
        if (names != null) {
            for (Map.Entry<String, UUID> nameEntry : names.entrySet()) {
                V value = this.attributeCache.get(new SessionAttributeKey(id, nameEntry.getValue()));
                if (value != null) {
                    try {
                        this.marshaller.read(value);
                        continue;
                    } catch (IOException e) {
                        InfinispanWebLogger.ROOT_LOGGER.failedToActivateSessionAttribute(e, id, nameEntry.getKey());
                    }
                } else {
                    InfinispanWebLogger.ROOT_LOGGER.missingSessionAttributeCacheEntry(id, nameEntry.getKey());
                }
                if (purgeIfInvalid) {
                    this.purge(id);
                }
                return null;
            }
            return new AtomicReference<>(names);
        }
        return new AtomicReference<>(Collections.emptyMap());
    }

    @Override
    public boolean remove(String id) {
        return this.delete(id);
    }

    @Override
    public boolean purge(String id) {
        return this.delete(id, Flag.SKIP_LISTENER_NOTIFICATION);
    }

    private boolean delete(String id, Flag... flags) {
        Map<String, UUID> names = this.namesCache.getAdvancedCache().withFlags(EnumSet.of(Flag.FORCE_SYNCHRONOUS, flags)).remove(new SessionAttributeNamesKey(id));
        if (names != null) {
            for (UUID attributeId : names.values()) {
                this.attributeCache.getAdvancedCache().withFlags(EnumSet.of(Flag.IGNORE_RETURN_VALUES, flags)).remove(new SessionAttributeKey(id, attributeId));
            }
        }
        return true;
    }

    @Override
    public SessionAttributes createSessionAttributes(String id, AtomicReference<Map<String, UUID>> names, ImmutableSessionMetaData metaData, C context) {
        SessionAttributeActivationNotifier notifier = new ImmutableSessionAttributeActivationNotifier<>(this.provider, new CompositeImmutableSession(id, metaData, this.createImmutableSessionAttributes(id, names)), context);
        return new FineSessionAttributes<>(new SessionAttributeNamesKey(id), names, this.namesCache, getKeyFactory(id), this.attributeCache.getAdvancedCache().withFlags(Flag.FORCE_SYNCHRONOUS), this.marshaller, this.mutatorFactory, this.immutability, this.properties, notifier);
    }

    @Override
    public ImmutableSessionAttributes createImmutableSessionAttributes(String id, AtomicReference<Map<String, UUID>> names) {
        return new FineImmutableSessionAttributes<>(names, getKeyFactory(id), this.attributeCache, this.marshaller);
    }

    private static Function<UUID, SessionAttributeKey> getKeyFactory(String id) {
        return new Function<UUID, SessionAttributeKey>() {
            @Override
            public SessionAttributeKey apply(UUID attributeId) {
                return new SessionAttributeKey(id, attributeId);
            }
        };
    }

    @CacheEntriesEvicted
    public void evicted(CacheEntriesEvictedEvent<GroupedKey<String>, ?> event) {
        if (!event.isPre()) {
            Set<SessionAttributeNamesKey> keys = new HashSet<>();
            for (GroupedKey<String> key : event.getEntries().keySet()) {
                // Workaround for ISPN-8324
                if (key instanceof SessionCreationMetaDataKey) {
                    keys.add(new SessionAttributeNamesKey(key.getId()));
                }
            }
            if (!keys.isEmpty()) {
                Cache<SessionAttributeKey, V> cache = this.attributeCache.getAdvancedCache().withFlags(Flag.SKIP_LISTENER_NOTIFICATION);
                for (Map.Entry<SessionAttributeNamesKey, Map<String, UUID>> entry : this.namesCache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL, Flag.SKIP_CACHE_LOAD, Flag.ZERO_LOCK_ACQUISITION_TIMEOUT, Flag.FAIL_SILENTLY).getAll(keys).entrySet()) {
                    Map<String, UUID> names = entry.getValue();
                    if (names != null) {
                        String sessionId = entry.getKey().getId();
                        for (UUID attributeId : names.values()) {
                            cache.evict(new SessionAttributeKey(sessionId, attributeId));
                        }
                    }
                    this.namesCache.getAdvancedCache().withFlags(Flag.SKIP_LISTENER_NOTIFICATION).evict(entry.getKey());
                }
            }
        }
    }
}
