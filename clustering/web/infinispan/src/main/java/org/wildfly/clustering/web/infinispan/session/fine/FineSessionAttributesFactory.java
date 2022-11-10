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
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.infinispan.Cache;
import org.wildfly.clustering.ee.Immutability;
import org.wildfly.clustering.ee.Key;
import org.wildfly.clustering.ee.MutatorFactory;
import org.wildfly.clustering.ee.cache.CacheProperties;
import org.wildfly.clustering.ee.infinispan.InfinispanMutatorFactory;
import org.wildfly.clustering.infinispan.listener.ListenerRegistration;
import org.wildfly.clustering.infinispan.listener.PostActivateBlockingListener;
import org.wildfly.clustering.infinispan.listener.PostPassivateBlockingListener;
import org.wildfly.clustering.infinispan.listener.PrePassivateBlockingListener;
import org.wildfly.clustering.infinispan.listener.PrePassivateNonBlockingListener;
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
public class FineSessionAttributesFactory<S, C, L, V> implements SessionAttributesFactory<C, AtomicReference<Map<String, UUID>>> {

    private final Cache<SessionAttributeNamesKey, Map<String, UUID>> namesCache;
    private final Cache<SessionAttributeKey, V> attributeCache;
    private final Cache<Key<String>, Object> writeCache;
    private final Cache<Key<String>, Object> silentCache;
    private final Marshaller<Object, V> marshaller;
    private final Immutability immutability;
    private final CacheProperties properties;
    private final MutatorFactory<SessionAttributeKey, V> mutatorFactory;
    private final HttpSessionActivationListenerProvider<S, C, L> provider;
    private final Function<String, SessionAttributeActivationNotifier> notifierFactory;
    private final Executor executor;
    private final ListenerRegistration evictListenerRegistration;
    private final ListenerRegistration evictAttributesListenerRegistration;
    private final ListenerRegistration prePassivateListenerRegistration;
    private final ListenerRegistration postActivateListenerRegistration;

    public FineSessionAttributesFactory(InfinispanSessionAttributesFactoryConfiguration<S, C, L, Object, V> configuration) {
        this.namesCache = configuration.getCache();
        this.attributeCache = configuration.getCache();
        this.writeCache = configuration.getWriteOnlyCache();
        this.silentCache = configuration.getSilentWriteCache();
        this.marshaller = configuration.getMarshaller();
        this.immutability = configuration.getImmutability();
        this.properties = configuration.getCacheProperties();
        this.mutatorFactory = new InfinispanMutatorFactory<>(this.attributeCache, this.properties);
        this.provider = configuration.getHttpSessionActivationListenerProvider();
        this.notifierFactory = configuration.getActivationNotifierFactory();
        this.executor = configuration.getBlockingManager().asExecutor(this.getClass().getName());
        this.evictListenerRegistration = new PostPassivateBlockingListener<>(configuration.getCache(), this::cascadeEvict).register(SessionCreationMetaDataKey.class);
        this.evictAttributesListenerRegistration = new PrePassivateNonBlockingListener<>(this.namesCache, this::cascadeEvictAttributes).register(SessionAttributeNamesKey.class);
        this.prePassivateListenerRegistration = !this.properties.isPersistent() ? new PrePassivateBlockingListener<>(this.attributeCache, this::prePassivate).register(SessionAttributeKey.class) : null;
        this.postActivateListenerRegistration = !this.properties.isPersistent() ? new PostActivateBlockingListener<>(this.attributeCache, this::postActivate).register(SessionAttributeKey.class) : null;
    }

    @Override
    public void close() {
        this.evictListenerRegistration.close();
        this.evictAttributesListenerRegistration.close();
        if (this.prePassivateListenerRegistration != null) {
            this.prePassivateListenerRegistration.close();
        }
        if (this.postActivateListenerRegistration != null) {
            this.postActivateListenerRegistration.close();
        }
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
            // Validate all attributes
            Map<SessionAttributeKey, String> attributes = new TreeMap<>();
            for (Map.Entry<String, UUID> entry : names.entrySet()) {
                attributes.put(new SessionAttributeKey(id, entry.getValue()), entry.getKey());
            }
            Map<SessionAttributeKey, V> entries = this.attributeCache.getAdvancedCache().getAll(attributes.keySet());
            for (Map.Entry<SessionAttributeKey, String> attribute : attributes.entrySet()) {
                V value = entries.get(attribute.getKey());
                if (value != null) {
                    try {
                        this.marshaller.read(value);
                        continue;
                    } catch (IOException e) {
                        InfinispanWebLogger.ROOT_LOGGER.failedToActivateSessionAttribute(e, id, attribute.getValue());
                    }
                } else {
                    InfinispanWebLogger.ROOT_LOGGER.missingSessionAttributeCacheEntry(id, attribute.getValue());
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
        return this.delete(this.writeCache, id);
    }

    @Override
    public boolean purge(String id) {
        return this.delete(this.silentCache, id);
    }

    private boolean delete(Cache<Key<String>, Object> cache, String id) {
        SessionAttributeNamesKey key = new SessionAttributeNamesKey(id);
        Map<String, UUID> names = this.namesCache.get(key);
        if (names != null) {
            for (UUID attributeId : names.values()) {
                cache.remove(new SessionAttributeKey(id, attributeId));
            }
            cache.remove(key);
        }
        return true;
    }

    @Override
    public SessionAttributes createSessionAttributes(String id, AtomicReference<Map<String, UUID>> names, ImmutableSessionMetaData metaData, C context) {
        SessionAttributeActivationNotifier notifier = new ImmutableSessionAttributeActivationNotifier<>(this.provider, new CompositeImmutableSession(id, metaData, this.createImmutableSessionAttributes(id, names)), context);
        return new FineSessionAttributes<>(new SessionAttributeNamesKey(id), names, this.namesCache, getKeyFactory(id), this.attributeCache, this.marshaller, this.mutatorFactory, this.immutability, this.properties, notifier);
    }

    @Override
    public ImmutableSessionAttributes createImmutableSessionAttributes(String id, AtomicReference<Map<String, UUID>> names) {
        return new FineImmutableSessionAttributes<>(names, getKeyFactory(id), this.attributeCache, this.marshaller);
    }

    private static Function<UUID, SessionAttributeKey> getKeyFactory(String id) {
        return new Function<>() {
            @Override
            public SessionAttributeKey apply(UUID attributeId) {
                return new SessionAttributeKey(id, attributeId);
            }
        };
    }

    private void cascadeEvict(SessionCreationMetaDataKey key) {
        this.namesCache.evict(new SessionAttributeNamesKey(key.getId()));
    }

    private void cascadeEvictAttributes(SessionAttributeNamesKey key, Map<String, UUID> value) {
        String sessionId = key.getId();
        for (UUID attributeId : value.values()) {
            this.executor.execute(() -> this.attributeCache.evict(new SessionAttributeKey(sessionId, attributeId)));
        }
    }

    private void prePassivate(SessionAttributeKey key, V value) {
        this.notify(SessionAttributeActivationNotifier.PRE_PASSIVATE, key, value);
    }

    private void postActivate(SessionAttributeKey key, V value) {
        this.notify(SessionAttributeActivationNotifier.POST_ACTIVATE, key, value);
    }

    private void notify(BiConsumer<SessionAttributeActivationNotifier, Object> notification, SessionAttributeKey key, V value) {
        String sessionId = key.getId();
        try (SessionAttributeActivationNotifier notifier = this.notifierFactory.apply(key.getId())) {
            notification.accept(notifier, this.marshaller.read(value));
        } catch (IOException e) {
            InfinispanWebLogger.ROOT_LOGGER.failedToActivateSessionAttribute(e, sessionId, key.getAttributeId().toString());
        }
    }
}
