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
import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.ee.MutatorFactory;
import org.wildfly.clustering.ee.cache.CacheProperties;
import org.wildfly.clustering.ee.infinispan.CacheMutatorFactory;
import org.wildfly.clustering.infinispan.listener.ListenerRegistration;
import org.wildfly.clustering.infinispan.listener.PostActivateBlockingListener;
import org.wildfly.clustering.infinispan.listener.PostPassivateBlockingListener;
import org.wildfly.clustering.infinispan.listener.PrePassivateBlockingListener;
import org.wildfly.clustering.marshalling.spi.Marshaller;
import org.wildfly.clustering.web.cache.session.CompositeImmutableSession;
import org.wildfly.clustering.web.cache.session.attributes.SessionAttributes;
import org.wildfly.clustering.web.cache.session.attributes.SessionAttributesFactory;
import org.wildfly.clustering.web.cache.session.attributes.SimpleImmutableSessionAttributes;
import org.wildfly.clustering.web.cache.session.attributes.coarse.CoarseSessionAttributes;
import org.wildfly.clustering.web.cache.session.attributes.coarse.ImmutableSessionActivationNotifier;
import org.wildfly.clustering.web.cache.session.attributes.coarse.SessionActivationNotifier;
import org.wildfly.clustering.web.cache.session.attributes.fine.SessionAttributeActivationNotifier;
import org.wildfly.clustering.web.infinispan.logging.InfinispanWebLogger;
import org.wildfly.clustering.web.infinispan.session.metadata.SessionMetaDataKey;
import org.wildfly.clustering.web.session.HttpSessionActivationListenerProvider;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;

/**
 * {@link SessionAttributesFactory} for coarse granularity sessions, where all session attributes are stored in a single cache entry.
 * @author Paul Ferraro
 */
public class CoarseSessionAttributesFactory<S, C, L, V> implements SessionAttributesFactory<C, Map<String, Object>> {

    private final Cache<SessionAttributesKey, V> cache;
    private final Cache<SessionAttributesKey, V> writeCache;
    private final Cache<SessionAttributesKey, V> silentCache;
    private final Marshaller<Map<String, Object>, V> marshaller;
    private final CacheProperties properties;
    private final Immutability immutability;
    private final MutatorFactory<SessionAttributesKey, V> mutatorFactory;
    private final HttpSessionActivationListenerProvider<S, C, L> provider;
    private final Function<String, SessionAttributeActivationNotifier> notifierFactory;
    private final ListenerRegistration evictListenerRegistration;
    private final ListenerRegistration prePassivateListenerRegistration;
    private final ListenerRegistration postActivateListenerRegistration;

    public CoarseSessionAttributesFactory(InfinispanSessionAttributesFactoryConfiguration<S, C, L, Map<String, Object>, V> configuration) {
        this.cache = configuration.getCache();
        this.writeCache = configuration.getWriteOnlyCache();
        this.silentCache = configuration.getSilentWriteCache();
        this.marshaller = configuration.getMarshaller();
        this.immutability = configuration.getImmutability();
        this.properties = configuration.getCacheProperties();
        this.mutatorFactory = new CacheMutatorFactory<>(this.cache, this.properties);
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
        Map<String, Object> attributes = new ConcurrentHashMap<>();
        try {
            V value = this.marshaller.write(attributes);
            this.writeCache.put(new SessionAttributesKey(id), value);
            return attributes;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
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
        V value = this.cache.get(new SessionAttributesKey(id));
        if (value != null) {
            try {
                return this.marshaller.read(value);
            } catch (IOException e) {
                InfinispanWebLogger.ROOT_LOGGER.failedToActivateSession(e, id);
                if (purgeIfInvalid) {
                    this.purge(id);
                }
            }
        }
        return null;
    }

    @Override
    public boolean remove(String id) {
        return this.delete(this.writeCache, id);
    }

    @Override
    public boolean purge(String id) {
        return this.delete(this.silentCache, id);
    }

    private boolean delete(Cache<SessionAttributesKey, V> cache, String id) {
        cache.remove(new SessionAttributesKey(id));
        return true;
    }

    @Override
    public SessionAttributes createSessionAttributes(String id, Map<String, Object> attributes, ImmutableSessionMetaData metaData, C context) {
        try {
            Mutator mutator = (this.properties.isTransactional() && metaData.isNew()) ? Mutator.PASSIVE : this.mutatorFactory.createMutator(new SessionAttributesKey(id), this.marshaller.write(attributes));
            SessionActivationNotifier notifier = this.properties.isPersistent() ? new ImmutableSessionActivationNotifier<>(this.provider, new CompositeImmutableSession(id, metaData, this.createImmutableSessionAttributes(id, attributes)), context) : null;
            return new CoarseSessionAttributes(attributes, mutator, this.marshaller, this.immutability, this.properties, notifier);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public ImmutableSessionAttributes createImmutableSessionAttributes(String id, Map<String, Object> values) {
        return new SimpleImmutableSessionAttributes(values);
    }

    private void cascadeEvict(SessionMetaDataKey key) {
        this.cache.evict(new SessionAttributesKey(key.getId()));
    }

    private void prePassivate(SessionAttributesKey key, V value) {
        this.notify(key, value, SessionAttributeActivationNotifier.PRE_PASSIVATE);
    }

    private void postActivate(SessionAttributesKey key, V value) {
        this.notify(key, value, SessionAttributeActivationNotifier.POST_ACTIVATE);
    }

    private void notify(SessionAttributesKey key, V value, BiConsumer<SessionAttributeActivationNotifier, Object> notification) {
        String sessionId = key.getId();
        try (SessionAttributeActivationNotifier notifier = this.notifierFactory.apply(sessionId)) {
            Map<String, Object> attributes = this.marshaller.read(value);
            for (Object attributeValue : attributes.values()) {
                notification.accept(notifier, attributeValue);
            }
        } catch (IOException e) {
            InfinispanWebLogger.ROOT_LOGGER.failedToActivateSession(e, sessionId);
        }
    }
}
