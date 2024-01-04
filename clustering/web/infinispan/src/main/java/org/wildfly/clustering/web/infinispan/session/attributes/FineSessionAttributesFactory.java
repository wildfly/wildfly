/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan.session.attributes;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.infinispan.Cache;
import org.wildfly.clustering.ee.Immutability;
import org.wildfly.clustering.ee.MutatorFactory;
import org.wildfly.clustering.ee.cache.CacheProperties;
import org.wildfly.clustering.ee.infinispan.CacheComputeMutatorFactory;
import org.wildfly.clustering.infinispan.listener.ListenerRegistration;
import org.wildfly.clustering.infinispan.listener.PostActivateBlockingListener;
import org.wildfly.clustering.infinispan.listener.PostPassivateBlockingListener;
import org.wildfly.clustering.infinispan.listener.PrePassivateBlockingListener;
import org.wildfly.clustering.marshalling.spi.Marshaller;
import org.wildfly.clustering.web.cache.session.CompositeImmutableSession;
import org.wildfly.clustering.web.cache.session.attributes.SessionAttributes;
import org.wildfly.clustering.web.cache.session.attributes.SessionAttributesFactory;
import org.wildfly.clustering.web.cache.session.attributes.SimpleImmutableSessionAttributes;
import org.wildfly.clustering.web.cache.session.attributes.fine.FineSessionAttributes;
import org.wildfly.clustering.web.cache.session.attributes.fine.ImmutableSessionAttributeActivationNotifier;
import org.wildfly.clustering.web.cache.session.attributes.fine.SessionAttributeActivationNotifier;
import org.wildfly.clustering.web.cache.session.attributes.fine.SessionAttributeMapComputeFunction;
import org.wildfly.clustering.web.infinispan.logging.InfinispanWebLogger;
import org.wildfly.clustering.web.infinispan.session.metadata.SessionMetaDataKey;
import org.wildfly.clustering.web.session.HttpSessionActivationListenerProvider;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;

/**
 * {@link SessionAttributesFactory} for fine granularity sessions.
 * A given session's attributes are mapped to N+1 co-located cache entries, where N is the number of session attributes.
 * A separate cache entry stores the activate attribute names for the session.
 * @author Paul Ferraro
 */
public class FineSessionAttributesFactory<S, C, L, V> implements SessionAttributesFactory<C, Map<String, Object>> {

    private final Cache<SessionAttributesKey, Map<String, V>> cache;
    private final Cache<SessionAttributesKey, Map<String, V>> writeCache;
    private final Cache<SessionAttributesKey, Map<String, V>> silentCache;
    private final Marshaller<Object, V> marshaller;
    private final Immutability immutability;
    private final CacheProperties properties;
    private final MutatorFactory<SessionAttributesKey, Map<String, V>> mutatorFactory;
    private final HttpSessionActivationListenerProvider<S, C, L> provider;
    private final Function<String, SessionAttributeActivationNotifier> notifierFactory;
    private final ListenerRegistration evictListenerRegistration;
    private final ListenerRegistration prePassivateListenerRegistration;
    private final ListenerRegistration postActivateListenerRegistration;

    public FineSessionAttributesFactory(InfinispanSessionAttributesFactoryConfiguration<S, C, L, Object, V> configuration) {
        this.cache = configuration.getCache();
        this.writeCache = configuration.getWriteOnlyCache();
        this.silentCache = configuration.getSilentWriteCache();
        this.marshaller = configuration.getMarshaller();
        this.immutability = configuration.getImmutability();
        this.properties = configuration.getCacheProperties();
        this.mutatorFactory = new CacheComputeMutatorFactory<>(this.cache, SessionAttributeMapComputeFunction::new);
        this.provider = configuration.getHttpSessionActivationListenerProvider();
        this.notifierFactory = configuration.getActivationNotifierFactory();
        this.prePassivateListenerRegistration = !this.properties.isPersistent() ? new PrePassivateBlockingListener<>(this.cache, this::prePassivate).register(SessionAttributesKey.class) : null;
        this.postActivateListenerRegistration = !this.properties.isPersistent() ? new PostActivateBlockingListener<>(this.cache, this::postActivate).register(SessionAttributesKey.class) : null;
        this.evictListenerRegistration = new PostPassivateBlockingListener<>(configuration.getCache(), this::cascadeEvict).register(SessionMetaDataKey.class);
    }

    @Override
    public void close() {
        this.evictListenerRegistration.close();
        if (this.prePassivateListenerRegistration != null) {
            this.prePassivateListenerRegistration.close();
        }
        if (this.postActivateListenerRegistration != null) {
            this.postActivateListenerRegistration.close();
        }
    }

    @Override
    public Map<String, Object> createValue(String id, Void context) {
        return new ConcurrentHashMap<>();
    }

    @Override
    public Map<String, Object> findValue(String id) {
        return this.getValue(id, true);
    }

    @Override
    public Map<String, Object> tryValue(String id) {
        return this.getValue(id, false);
    }

    private Map<String, Object> getValue(String id, boolean purgeIfInvalid) {
        Map<String, Object> attributes = this.createValue(id, null);
        Map<String, V> value = this.cache.get(new SessionAttributesKey(id));
        if (value != null) {
            for (Map.Entry<String, V> entry : value.entrySet()) {
                String attributeName = entry.getKey();
                try {
                    attributes.put(attributeName, this.marshaller.read(entry.getValue()));
                } catch (IOException e) {
                    InfinispanWebLogger.ROOT_LOGGER.failedToActivateSessionAttribute(e, id, attributeName);
                    if (purgeIfInvalid) {
                        this.purge(id);
                    }
                    return null;
                }
            }
        }
        return attributes;
    }

    @Override
    public boolean remove(String id) {
        return this.delete(this.writeCache, id);
    }

    @Override
    public boolean purge(String id) {
        return this.delete(this.silentCache, id);
    }

    private boolean delete(Cache<SessionAttributesKey, Map<String, V>> cache, String id) {
        cache.remove(new SessionAttributesKey(id));
        return true;
    }

    @Override
    public SessionAttributes createSessionAttributes(String id, Map<String, Object> attributes, ImmutableSessionMetaData metaData, C context) {
        SessionAttributeActivationNotifier notifier = this.properties.isPersistent() ? new ImmutableSessionAttributeActivationNotifier<>(this.provider, new CompositeImmutableSession(id, metaData, this.createImmutableSessionAttributes(id, attributes)), context) : null;
        return new FineSessionAttributes<>(new SessionAttributesKey(id), attributes, this.mutatorFactory, this.marshaller, this.immutability, this.properties, notifier);
    }

    @Override
    public ImmutableSessionAttributes createImmutableSessionAttributes(String id, Map<String, Object> attributes) {
        return new SimpleImmutableSessionAttributes(attributes);
    }

    private void cascadeEvict(SessionMetaDataKey key) {
        this.cache.evict(new SessionAttributesKey(key.getId()));
    }

    private void prePassivate(SessionAttributesKey key, Map<String, V> attributes) {
        this.notify(SessionAttributeActivationNotifier.PRE_PASSIVATE, key, attributes);
    }

    private void postActivate(SessionAttributesKey key, Map<String, V> attributes) {
        this.notify(SessionAttributeActivationNotifier.POST_ACTIVATE, key, attributes);
    }

    private void notify(BiConsumer<SessionAttributeActivationNotifier, Object> notification, SessionAttributesKey key, Map<String, V> attributes) {
        String sessionId = key.getId();
        for (Map.Entry<String, V> entry : attributes.entrySet()) {
            try (SessionAttributeActivationNotifier notifier = this.notifierFactory.apply(key.getId())) {
                notification.accept(notifier, this.marshaller.read(entry.getValue()));
            } catch (IOException e) {
                InfinispanWebLogger.ROOT_LOGGER.failedToActivateSessionAttribute(e, sessionId, entry.getKey());
            }
        }
    }
}
