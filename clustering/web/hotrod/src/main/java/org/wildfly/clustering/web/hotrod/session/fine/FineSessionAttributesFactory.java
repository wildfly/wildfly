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

package org.wildfly.clustering.web.hotrod.session.fine;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.wildfly.clustering.ee.Immutability;
import org.wildfly.clustering.ee.MutatorFactory;
import org.wildfly.clustering.ee.cache.CacheProperties;
import org.wildfly.clustering.ee.hotrod.RemoteCacheMap;
import org.wildfly.clustering.ee.hotrod.RemoteCacheMutatorFactory;
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
public class FineSessionAttributesFactory<S, C, L, V> implements SessionAttributesFactory<C, Map<String, UUID>> {

    private final RemoteCache<SessionAttributeNamesKey, Map<String, UUID>> namesCache;
    private final RemoteCache<SessionAttributeKey, V> attributeCache;
    private final Marshaller<Object, V> marshaller;
    private final Immutability immutability;
    private final CacheProperties properties;
    private final MutatorFactory<SessionAttributeKey, V> mutatorFactory;
    private final HttpSessionActivationListenerProvider<S, C, L> provider;

    public FineSessionAttributesFactory(HotRodSessionAttributesFactoryConfiguration<S, C, L, Object, V> configuration) {
        this.namesCache = configuration.getCache();
        this.attributeCache = configuration.getCache();
        this.marshaller = configuration.getMarshaller();
        this.immutability = configuration.getImmutability();
        this.properties = configuration.getCacheProperties();
        this.mutatorFactory = new RemoteCacheMutatorFactory<>(this.attributeCache);
        this.provider = configuration.getHttpSessionActivationListenerProvider();
    }

    @Override
    public Map<String, UUID> createValue(String id, Void context) {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, UUID> findValue(String id) {
        Map<String, UUID> names = this.namesCache.get(new SessionAttributeNamesKey(id));
        if (names != null) {
            for (Map.Entry<String, UUID> nameEntry : names.entrySet()) {
                V value = this.attributeCache.get(new SessionAttributeKey(id, nameEntry.getValue()));
                if (value != null) {
                    try {
                        this.marshaller.read(value);
                        continue;
                    } catch (IOException e) {
                        Logger.ROOT_LOGGER.failedToActivateSessionAttribute(e, id, nameEntry.getKey());
                    }
                } else {
                    Logger.ROOT_LOGGER.missingSessionAttributeCacheEntry(id, nameEntry.getKey());
                }
                this.remove(id);
                return null;
            }
            return names;
        }
        return Collections.emptyMap();
    }

    @Override
    public boolean remove(String id) {
        Map<String, UUID> names = this.namesCache.withFlags(Flag.FORCE_RETURN_VALUE).remove(new SessionAttributeNamesKey(id));
        if (names != null) {
            for (UUID attributeId : names.values()) {
                this.attributeCache.remove(new SessionAttributeKey(id, attributeId));
            }
        }
        return true;
    }

    @Override
    public SessionAttributes createSessionAttributes(String id, Map<String, UUID> names, ImmutableSessionMetaData metaData, C context) {
        SessionAttributeActivationNotifier notifier = new ImmutableSessionAttributeActivationNotifier<>(this.provider, new CompositeImmutableSession(id, metaData, this.createImmutableSessionAttributes(id, names)), context);
        return new FineSessionAttributes<>(new SessionAttributeNamesKey(id), names, this.namesCache, getKeyFactory(id), new RemoteCacheMap<>(this.attributeCache), this.marshaller, this.mutatorFactory, this.immutability, this.properties, notifier);
    }

    @Override
    public ImmutableSessionAttributes createImmutableSessionAttributes(String id, Map<String, UUID> names) {
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
}
