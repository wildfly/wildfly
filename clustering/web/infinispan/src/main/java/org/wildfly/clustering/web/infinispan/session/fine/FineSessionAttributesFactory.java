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

import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.wildfly.clustering.ee.infinispan.CacheEntryMutator;
import org.wildfly.clustering.ee.infinispan.CacheProperties;
import org.wildfly.clustering.ee.infinispan.Mutator;
import org.wildfly.clustering.marshalling.jboss.InvalidSerializedFormException;
import org.wildfly.clustering.marshalling.jboss.MarshalledValue;
import org.wildfly.clustering.marshalling.jboss.Marshaller;
import org.wildfly.clustering.marshalling.jboss.MarshallingContext;
import org.wildfly.clustering.web.infinispan.logging.InfinispanWebLogger;
import org.wildfly.clustering.web.infinispan.session.SessionAttributes;
import org.wildfly.clustering.web.infinispan.session.SessionAttributesFactory;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;

/**
 * {@link SessionAttributesFactory} for fine granularity sessions.
 * A given session's attributes are mapped to N+1 co-located cache entries, where N is the number of session attributes.
 * A separate cache entry stores the activate attribute names for the session.
 * @author Paul Ferraro
 */
public class FineSessionAttributesFactory implements SessionAttributesFactory<SessionAttributeNamesEntry> {

    private final Cache<SessionAttributeNamesKey, SessionAttributeNamesEntry> namesCache;
    private final Cache<SessionAttributeKey, MarshalledValue<Object, MarshallingContext>> attributeCache;
    private final Marshaller<Object, MarshalledValue<Object, MarshallingContext>, MarshallingContext> marshaller;
    private final CacheProperties properties;

    public FineSessionAttributesFactory(Cache<SessionAttributeNamesKey, SessionAttributeNamesEntry> namesCache, Cache<SessionAttributeKey, MarshalledValue<Object, MarshallingContext>> attributeCache, Marshaller<Object, MarshalledValue<Object, MarshallingContext>, MarshallingContext> marshaller, CacheProperties properties) {
        this.namesCache = namesCache;
        this.attributeCache = attributeCache;
        this.marshaller = marshaller;
        this.properties = properties;
    }

    @Override
    public SessionAttributeNamesEntry createValue(String id, Void context) {
        SessionAttributeNamesEntry entry = new SessionAttributeNamesEntry(new AtomicInteger(), new ConcurrentHashMap<>());
        this.namesCache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).put(new SessionAttributeNamesKey(id), entry);
        return entry;
    }

    @Override
    public SessionAttributeNamesEntry findValue(String id) {
        SessionAttributeNamesEntry entry = this.namesCache.get(new SessionAttributeNamesKey(id));
        if (entry != null) {
            ConcurrentMap<String, Integer> names = entry.getNames();
            Map<SessionAttributeKey, MarshalledValue<Object, MarshallingContext>> attributes = this.attributeCache.getAdvancedCache().getAll(names.values().stream().map(attributeId -> new SessionAttributeKey(id, attributeId)).collect(Collectors.toSet()));
            Predicate<Map.Entry<String, MarshalledValue<Object, MarshallingContext>>> invalidAttribute = attribute -> {
                MarshalledValue<Object, MarshallingContext> value = attribute.getValue();
                if (value == null) {
                    InfinispanWebLogger.ROOT_LOGGER.missingSessionAttributeCacheEntry(id, attribute.getKey());
                    return true;
                }
                try {
                    this.marshaller.read(attribute.getValue());
                    return false;
                } catch (InvalidSerializedFormException e) {
                    InfinispanWebLogger.ROOT_LOGGER.failedToActivateSessionAttribute(e, id, attribute.getKey());
                    return true;
                }
            };
            if (names.entrySet().stream().map(name -> new AbstractMap.SimpleImmutableEntry<>(name.getKey(), attributes.get(new SessionAttributeKey(id, name.getValue())))).anyMatch(invalidAttribute)) {
                // If any attributes are invalid - remove them all
                this.remove(id);
                return null;
            }
        }
        return entry;
    }

    @Override
    public boolean remove(String id) {
        SessionAttributeNamesEntry entry = this.namesCache.getAdvancedCache().withFlags(Flag.FORCE_SYNCHRONOUS).remove(new SessionAttributeNamesKey(id));
        if (entry == null) return false;
        entry.getNames().values().forEach(attributeId -> this.attributeCache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).remove(new SessionAttributeKey(id, attributeId)));
        return true;
    }

    @Override
    public void evict(String id) {
        SessionAttributeNamesKey key = new SessionAttributeNamesKey(id);
        SessionAttributeNamesEntry entry = this.namesCache.getAdvancedCache().withFlags(Flag.SKIP_CACHE_LOAD).get(key);
        if (entry != null) {
            entry.getNames().entrySet().stream().forEach(attribute -> {
                try {
                    this.attributeCache.evict(new SessionAttributeKey(id, attribute.getValue()));
                } catch (Throwable e) {
                    InfinispanWebLogger.ROOT_LOGGER.failedToPassivateSessionAttribute(e, id, attribute.getKey());
                }
            });
            this.namesCache.getAdvancedCache().withFlags(Flag.FAIL_SILENTLY).evict(key);
        }
    }

    @Override
    public SessionAttributes createSessionAttributes(String id, SessionAttributeNamesEntry entry) {
        SessionAttributeNamesKey key = new SessionAttributeNamesKey(id);
        Mutator mutator = this.properties.isTransactional() && this.namesCache.getAdvancedCache().getCacheEntry(key).isCreated() ? Mutator.PASSIVE : new CacheEntryMutator<>(this.namesCache, key, entry);
        return new FineSessionAttributes<>(id, entry.getSequence(), entry.getNames(), mutator, this.attributeCache, this.marshaller, this.properties);
    }

    @Override
    public ImmutableSessionAttributes createImmutableSessionAttributes(String id, SessionAttributeNamesEntry entry) {
        return new FineImmutableSessionAttributes<>(id, entry.getNames(), this.attributeCache, this.marshaller);
    }
}
