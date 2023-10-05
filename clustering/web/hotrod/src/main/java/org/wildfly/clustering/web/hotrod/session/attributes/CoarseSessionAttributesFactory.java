/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.hotrod.session.attributes;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.wildfly.clustering.ee.Immutability;
import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.ee.MutatorFactory;
import org.wildfly.clustering.ee.cache.CacheProperties;
import org.wildfly.clustering.ee.hotrod.RemoteCacheMutatorFactory;
import org.wildfly.clustering.marshalling.spi.Marshaller;
import org.wildfly.clustering.web.cache.session.CompositeImmutableSession;
import org.wildfly.clustering.web.cache.session.attributes.SessionAttributes;
import org.wildfly.clustering.web.cache.session.attributes.SessionAttributesFactory;
import org.wildfly.clustering.web.cache.session.attributes.SimpleImmutableSessionAttributes;
import org.wildfly.clustering.web.cache.session.attributes.coarse.CoarseSessionAttributes;
import org.wildfly.clustering.web.cache.session.attributes.coarse.ImmutableSessionActivationNotifier;
import org.wildfly.clustering.web.cache.session.attributes.coarse.SessionActivationNotifier;
import org.wildfly.clustering.web.hotrod.logging.Logger;
import org.wildfly.clustering.web.hotrod.session.HotRodSessionAttributesFactoryConfiguration;
import org.wildfly.clustering.web.session.HttpSessionActivationListenerProvider;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;

/**
 * @author Paul Ferraro
 */
public class CoarseSessionAttributesFactory<S, C, L, V> implements SessionAttributesFactory<C, Map<String, Object>> {

    private final RemoteCache<SessionAttributesKey, V> cache;
    private final Flag[] ignoreReturnFlags;
    private final Marshaller<Map<String, Object>, V> marshaller;
    private final Immutability immutability;
    private final CacheProperties properties;
    private final MutatorFactory<SessionAttributesKey, V> mutatorFactory;
    private final HttpSessionActivationListenerProvider<S, C, L> provider;

    public CoarseSessionAttributesFactory(HotRodSessionAttributesFactoryConfiguration<S, C, L, Map<String, Object>, V> configuration) {
        this.cache = configuration.getCache();
        this.ignoreReturnFlags = configuration.getIgnoreReturnFlags();
        this.marshaller = configuration.getMarshaller();
        this.immutability = configuration.getImmutability();
        this.properties = configuration.getCacheProperties();
        this.mutatorFactory = new RemoteCacheMutatorFactory<>(this.cache, this.ignoreReturnFlags);
        this.provider = configuration.getHttpSessionActivationListenerProvider();
    }

    @Override
    public Map<String, Object> createValue(String id, Void context) {
        Map<String, Object> attributes = new ConcurrentHashMap<>();
        try {
            V value = this.marshaller.write(attributes);
            this.cache.withFlags(this.ignoreReturnFlags).put(new SessionAttributesKey(id), value);
            return attributes;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Map<String, Object> findValue(String id) {
        V value = this.cache.get(new SessionAttributesKey(id));
        if (value != null) {
            try {
                return this.marshaller.read(value);
            } catch (IOException e) {
                Logger.ROOT_LOGGER.failedToActivateSession(e, id.toString());
                this.remove(id);
            }
        }
        return null;
    }

    @Override
    public SessionAttributes createSessionAttributes(String id, Map<String, Object> attributes, ImmutableSessionMetaData metaData, C context) {
        try {
            Mutator mutator = this.mutatorFactory.createMutator(new SessionAttributesKey(id), this.marshaller.write(attributes));
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

    @Override
    public boolean remove(String id) {
        this.cache.withFlags(this.ignoreReturnFlags).remove(new SessionAttributesKey(id));
        return true;
    }
}
