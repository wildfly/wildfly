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

package org.wildfly.clustering.web.infinispan.session.coarse;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntriesEvicted;
import org.infinispan.notifications.cachelistener.event.CacheEntriesEvictedEvent;
import org.wildfly.clustering.ee.Immutability;
import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.ee.MutatorFactory;
import org.wildfly.clustering.ee.cache.CacheProperties;
import org.wildfly.clustering.ee.infinispan.GroupedKey;
import org.wildfly.clustering.ee.infinispan.InfinispanMutatorFactory;
import org.wildfly.clustering.marshalling.spi.Marshaller;
import org.wildfly.clustering.web.cache.session.CompositeImmutableSession;
import org.wildfly.clustering.web.cache.session.ImmutableSessionActivationNotifier;
import org.wildfly.clustering.web.cache.session.SessionActivationNotifier;
import org.wildfly.clustering.web.cache.session.SessionAttributes;
import org.wildfly.clustering.web.cache.session.SessionAttributesFactory;
import org.wildfly.clustering.web.cache.session.coarse.CoarseImmutableSessionAttributes;
import org.wildfly.clustering.web.cache.session.coarse.CoarseSessionAttributes;
import org.wildfly.clustering.web.infinispan.logging.InfinispanWebLogger;
import org.wildfly.clustering.web.infinispan.session.InfinispanSessionAttributesFactoryConfiguration;
import org.wildfly.clustering.web.infinispan.session.SessionCreationMetaDataKey;
import org.wildfly.clustering.web.session.HttpSessionActivationListenerProvider;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;

/**
 * {@link SessionAttributesFactory} for coarse granularity sessions, where all session attributes are stored in a single cache entry.
 * @author Paul Ferraro
 */
@Listener(sync = false)
public class CoarseSessionAttributesFactory<S, C, L, V> implements SessionAttributesFactory<C, Map<String, Object>> {

    private final Cache<SessionAttributesKey, V> cache;
    private final Marshaller<Map<String, Object>, V> marshaller;
    private final CacheProperties properties;
    private final Immutability immutability;
    private final MutatorFactory<SessionAttributesKey, V> mutatorFactory;
    private final HttpSessionActivationListenerProvider<S, C, L> provider;

    public CoarseSessionAttributesFactory(InfinispanSessionAttributesFactoryConfiguration<S, C, L, Map<String, Object>, V> configuration) {
        this.cache = configuration.getCache();
        this.marshaller = configuration.getMarshaller();
        this.immutability = configuration.getImmutability();
        this.properties = configuration.getCacheProperties();
        this.mutatorFactory = new InfinispanMutatorFactory<>(this.cache, this.properties);
        this.provider = configuration.getHttpSessionActivationListenerProvider();
    }

    @Override
    public Map<String, Object> createValue(String id, Void context) {
        Map<String, Object> attributes = this.properties.isLockOnRead() ? new HashMap<>() : new ConcurrentHashMap<>();
        try {
            V value = this.marshaller.write(attributes);
            this.cache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).put(new SessionAttributesKey(id), value);
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
        return this.delete(id);
    }

    @Override
    public boolean purge(String id) {
        return this.delete(id, Flag.SKIP_LISTENER_NOTIFICATION);
    }

    private boolean delete(String id, Flag... flags) {
        this.cache.getAdvancedCache().withFlags(EnumSet.of(Flag.IGNORE_RETURN_VALUES, flags)).remove(new SessionAttributesKey(id));
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
        return new CoarseImmutableSessionAttributes(values);
    }

    @CacheEntriesEvicted
    public void evicted(CacheEntriesEvictedEvent<GroupedKey<String>, ?> event) {
        if (!event.isPre()) {
            Cache<SessionAttributesKey, V> cache = this.cache.getAdvancedCache().withFlags(Flag.SKIP_LISTENER_NOTIFICATION);
            for (GroupedKey<String> key : event.getEntries().keySet()) {
                // Workaround for ISPN-8324
                if (key instanceof SessionCreationMetaDataKey) {
                    cache.evict(new SessionAttributesKey(key.getId()));
                }
            }
        }
    }
}
