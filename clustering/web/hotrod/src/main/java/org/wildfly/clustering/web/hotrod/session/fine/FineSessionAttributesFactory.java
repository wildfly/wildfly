/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.hotrod.session.fine;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.wildfly.clustering.ee.Immutability;
import org.wildfly.clustering.ee.cache.CacheProperties;
import org.wildfly.clustering.marshalling.spi.Marshaller;
import org.wildfly.clustering.web.cache.session.CompositeImmutableSession;
import org.wildfly.clustering.web.cache.session.ImmutableSessionAttributeActivationNotifier;
import org.wildfly.clustering.web.cache.session.SessionAttributeActivationNotifier;
import org.wildfly.clustering.web.cache.session.SessionAttributes;
import org.wildfly.clustering.web.cache.session.SessionAttributesFactory;
import org.wildfly.clustering.web.cache.session.fine.FineImmutableSessionAttributes;
import org.wildfly.clustering.web.cache.session.fine.FineSessionAttributes;
import org.wildfly.clustering.web.hotrod.logging.Logger;
import org.wildfly.clustering.web.hotrod.session.HotRodSessionAttributesFactoryConfiguration;
import org.wildfly.clustering.web.session.HttpSessionActivationListenerProvider;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;

/**
 * {@link SessionAttributesFactory} for fine granularity sessions.
 * A given session's attributes are mapped to N+1 co-located cache entries, where N is the number of session attributes.
 * A separate cache entry stores the activate attribute names for the session.
 * @author Paul Ferraro
 */
public class FineSessionAttributesFactory<S, C, L, V> implements SessionAttributesFactory<C, Map.Entry<Map<String, UUID>, Map<UUID, Object>>> {

    private final RemoteCache<SessionAttributeNamesKey, Map<String, UUID>> namesCache;
    private final RemoteCache<SessionAttributeKey, V> attributeCache;
    private final Marshaller<Object, V> marshaller;
    private final Immutability immutability;
    private final CacheProperties properties;
    private final HttpSessionActivationListenerProvider<S, C, L> provider;

    public FineSessionAttributesFactory(HotRodSessionAttributesFactoryConfiguration<S, C, L, Object, V> configuration) {
        this.namesCache = configuration.getCache();
        this.attributeCache = configuration.getCache();
        this.marshaller = configuration.getMarshaller();
        this.immutability = configuration.getImmutability();
        this.properties = configuration.getCacheProperties();
        this.provider = configuration.getHttpSessionActivationListenerProvider();
    }

    @Override
    public Map.Entry<Map<String, UUID>, Map<UUID, Object>> createValue(String id, Void context) {
        return new AbstractMap.SimpleImmutableEntry<>(new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
    }

    @Override
    public Map.Entry<Map<String, UUID>, Map<UUID, Object>> findValue(String id) {
        return this.getValue(id, true);
    }

    @Override
    public Map.Entry<Map<String, UUID>, Map<UUID, Object>> tryValue(String id) {
        return this.getValue(id, false);
    }

    private Map.Entry<Map<String, UUID>, Map<UUID, Object>> getValue(String id, boolean purgeIfInvalid) {
        Map<String, UUID> names = this.namesCache.get(new SessionAttributeNamesKey(id));
        if (names == null) {
            return this.createValue(id, null);
        }
        // Read attribute entries via bulk read
        Map<SessionAttributeKey, String> keys = new HashMap<>();
        for (Map.Entry<String, UUID> entry : names.entrySet()) {
            keys.put(new SessionAttributeKey(id, entry.getValue()), entry.getKey());
        }
        Map<SessionAttributeKey, V> values = this.attributeCache.getAll(keys.keySet());
        // Validate attributes
        Map<UUID, Object> attributes = new ConcurrentHashMap<>();
        for (Map.Entry<SessionAttributeKey, String> entry : keys.entrySet()) {
            SessionAttributeKey key = entry.getKey();
            V value = values.get(entry.getKey());
            if (value != null) {
                try {
                    attributes.put(key.getAttributeId(), this.marshaller.read(value));
                    continue;
                } catch (IOException e) {
                    Logger.ROOT_LOGGER.failedToActivateSessionAttribute(e, id, entry.getValue());
                }
            } else {
                Logger.ROOT_LOGGER.missingSessionAttributeCacheEntry(id, entry.getValue());
            }
            if (purgeIfInvalid) {
                this.purge(id);
            }
            return null;
        }
        return new AbstractMap.SimpleImmutableEntry<>(new ConcurrentHashMap<>(names), attributes);
    }

    @Override
    public boolean remove(String id) {
        SessionAttributeNamesKey key = new SessionAttributeNamesKey(id);
        Map<String, UUID> names = this.namesCache.withFlags(Flag.FORCE_RETURN_VALUE).remove(key);
        if (names != null) {
            for (UUID attributeId : names.values()) {
                this.attributeCache.remove(new SessionAttributeKey(id, attributeId));
            }
        }
        return true;
    }

    @Override
    public SessionAttributes createSessionAttributes(String id, Map.Entry<Map<String, UUID>, Map<UUID, Object>> entry, ImmutableSessionMetaData metaData, C context) {
        SessionAttributeActivationNotifier notifier = new ImmutableSessionAttributeActivationNotifier<>(this.provider, new CompositeImmutableSession(id, metaData, this.createImmutableSessionAttributes(id, entry)), context);
        return new FineSessionAttributes<>(this.namesCache, new SessionAttributeNamesKey(id), entry.getKey(), this.attributeCache, getKeyFactory(id), entry.getValue(), this.marshaller, this.immutability, this.properties, notifier);
    }

    @Override
    public ImmutableSessionAttributes createImmutableSessionAttributes(String id, Map.Entry<Map<String, UUID>, Map<UUID, Object>> entry) {
        return new FineImmutableSessionAttributes(entry.getKey(), entry.getValue());
    }

    private static Function<UUID, SessionAttributeKey> getKeyFactory(String id) {
        return new Function<>() {
            @Override
            public SessionAttributeKey apply(UUID attributeId) {
                return new SessionAttributeKey(id, attributeId);
            }
        };
    }
}
