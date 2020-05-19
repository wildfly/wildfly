/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.hotrod.session.coarse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.infinispan.client.hotrod.RemoteCache;
import org.wildfly.clustering.ee.Immutability;
import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.ee.MutatorFactory;
import org.wildfly.clustering.ee.cache.CacheProperties;
import org.wildfly.clustering.ee.hotrod.RemoteCacheMutatorFactory;
import org.wildfly.clustering.marshalling.spi.InvalidSerializedFormException;
import org.wildfly.clustering.marshalling.spi.Marshaller;
import org.wildfly.clustering.web.cache.session.CompositeImmutableSession;
import org.wildfly.clustering.web.cache.session.ImmutableSessionActivationNotifier;
import org.wildfly.clustering.web.cache.session.SessionActivationNotifier;
import org.wildfly.clustering.web.cache.session.SessionAttributes;
import org.wildfly.clustering.web.cache.session.SessionAttributesFactory;
import org.wildfly.clustering.web.cache.session.coarse.CoarseImmutableSessionAttributes;
import org.wildfly.clustering.web.cache.session.coarse.CoarseSessionAttributes;
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
    private final Marshaller<Map<String, Object>, V> marshaller;
    private final Immutability immutability;
    private final CacheProperties properties;
    private final MutatorFactory<SessionAttributesKey, V> mutatorFactory;
    private final HttpSessionActivationListenerProvider<S, C, L> provider;

    public CoarseSessionAttributesFactory(HotRodSessionAttributesFactoryConfiguration<S, C, L, Map<String, Object>, V> configuration) {
        this.cache = configuration.getCache();
        this.marshaller = configuration.getMarshaller();
        this.immutability = configuration.getImmutability();
        this.properties = configuration.getCacheProperties();
        this.mutatorFactory = new RemoteCacheMutatorFactory<>(this.cache);
        this.provider = configuration.getHttpSessionActivationListenerProvider();
    }

    @Override
    public Map<String, Object> createValue(String id, Void context) {
        Map<String, Object> attributes = new ConcurrentHashMap<>();
        V value = this.marshaller.write(attributes);
        this.cache.put(new SessionAttributesKey(id), value);
        return attributes;
    }

    @Override
    public Map<String, Object> findValue(String id) {
        V value = this.cache.get(new SessionAttributesKey(id));
        if (value != null) {
            try {
                return this.marshaller.read(value);
            } catch (InvalidSerializedFormException e) {
                Logger.ROOT_LOGGER.failedToActivateSession(e, id.toString());
                this.remove(id);
            }
        }
        return null;
    }

    @Override
    public SessionAttributes createSessionAttributes(String id, Map<String, Object> values, ImmutableSessionMetaData metaData, C context) {
        ImmutableSessionAttributes attributes = this.createImmutableSessionAttributes(id, values);
        SessionActivationNotifier notifier = new ImmutableSessionActivationNotifier<>(this.provider, new CompositeImmutableSession(id, metaData, attributes), context);
        Mutator mutator = this.mutatorFactory.createMutator(new SessionAttributesKey(id), this.marshaller.write(values));
        return new CoarseSessionAttributes(values, mutator, this.marshaller, this.immutability, this.properties, notifier);
    }

    @Override
    public ImmutableSessionAttributes createImmutableSessionAttributes(String id, Map<String, Object> values) {
        return new CoarseImmutableSessionAttributes(values);
    }

    @Override
    public boolean remove(String id) {
        this.cache.remove(new SessionAttributesKey(id));
        return true;
    }
}
