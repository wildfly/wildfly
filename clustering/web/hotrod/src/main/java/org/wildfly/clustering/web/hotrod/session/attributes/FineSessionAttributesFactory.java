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
import org.wildfly.clustering.ee.MutatorFactory;
import org.wildfly.clustering.ee.cache.CacheProperties;
import org.wildfly.clustering.ee.hotrod.RemoteCacheComputeMutatorFactory;
import org.wildfly.clustering.marshalling.spi.Marshaller;
import org.wildfly.clustering.web.cache.session.CompositeImmutableSession;
import org.wildfly.clustering.web.cache.session.attributes.SessionAttributes;
import org.wildfly.clustering.web.cache.session.attributes.SessionAttributesFactory;
import org.wildfly.clustering.web.cache.session.attributes.SimpleImmutableSessionAttributes;
import org.wildfly.clustering.web.cache.session.attributes.fine.FineSessionAttributes;
import org.wildfly.clustering.web.cache.session.attributes.fine.ImmutableSessionAttributeActivationNotifier;
import org.wildfly.clustering.web.cache.session.attributes.fine.SessionAttributeActivationNotifier;
import org.wildfly.clustering.web.cache.session.attributes.fine.SessionAttributeMapComputeFunction;
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
public class FineSessionAttributesFactory<S, C, L, V> implements SessionAttributesFactory<C, Map<String, Object>> {

    private final RemoteCache<SessionAttributesKey, Map<String, V>> cache;
    private final Flag[] ignoreReturnFlags;
    private final Marshaller<Object, V> marshaller;
    private final Immutability immutability;
    private final CacheProperties properties;
    private final MutatorFactory<SessionAttributesKey, Map<String, V>> mutatorFactory;
    private final HttpSessionActivationListenerProvider<S, C, L> provider;

    public FineSessionAttributesFactory(HotRodSessionAttributesFactoryConfiguration<S, C, L, Object, V> configuration) {
        this.cache = configuration.getCache();
        this.ignoreReturnFlags = configuration.getIgnoreReturnFlags();
        this.marshaller = configuration.getMarshaller();
        this.immutability = configuration.getImmutability();
        this.properties = configuration.getCacheProperties();
        this.mutatorFactory = new RemoteCacheComputeMutatorFactory<>(this.cache, this.ignoreReturnFlags, SessionAttributeMapComputeFunction::new);
        this.provider = configuration.getHttpSessionActivationListenerProvider();
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
                    Logger.ROOT_LOGGER.failedToActivateSessionAttribute(e, id, attributeName);
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
        this.cache.withFlags(this.ignoreReturnFlags).remove(new SessionAttributesKey(id));
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
}
