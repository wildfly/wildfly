/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.infinispan.Cache;
import org.infinispan.commons.marshall.NotSerializableException;
import org.infinispan.context.Flag;
import org.wildfly.clustering.ee.Immutability;
import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.ee.infinispan.CacheEntryMutator;
import org.wildfly.clustering.ee.infinispan.CacheProperties;
import org.wildfly.clustering.infinispan.spi.function.ConcurrentMapPutFunction;
import org.wildfly.clustering.infinispan.spi.function.ConcurrentMapRemoveFunction;
import org.wildfly.clustering.infinispan.spi.function.CopyOnWriteMapPutFunction;
import org.wildfly.clustering.infinispan.spi.function.CopyOnWriteMapRemoveFunction;
import org.wildfly.clustering.marshalling.spi.InvalidSerializedFormException;
import org.wildfly.clustering.marshalling.spi.Marshaller;
import org.wildfly.clustering.web.infinispan.logging.InfinispanWebLogger;
import org.wildfly.clustering.web.infinispan.session.SessionAttributes;

/**
 * Exposes session attributes for fine granularity sessions.
 * @author Paul Ferraro
 */
public class FineSessionAttributes<V> implements SessionAttributes {
    private final String id;
    private final Cache<SessionAttributeNamesKey, Map<String, UUID>> namesCache;
    private final Cache<SessionAttributeKey, V> attributeCache;
    private final Map<String, Mutator> mutations = new ConcurrentHashMap<>();
    private final Marshaller<Object, V> marshaller;
    private final Immutability immutability;
    private final CacheProperties properties;

    private volatile Map<String, UUID> names;

    public FineSessionAttributes(String id, Map<String, UUID> names, Cache<SessionAttributeNamesKey, Map<String, UUID>> namesCache, Cache<SessionAttributeKey, V> attributeCache, Marshaller<Object, V> marshaller, Immutability immutability, CacheProperties properties) {
        this.id = id;
        this.setNames(names);
        this.namesCache = namesCache;
        this.attributeCache = attributeCache;
        this.marshaller = marshaller;
        this.immutability = immutability;
        this.properties = properties;
    }

    @Override
    public Object removeAttribute(String name) {
        UUID attributeId = this.names.get(name);
        if (attributeId == null) return null;

        this.setNames(this.namesCache.getAdvancedCache().withFlags(Flag.FORCE_SYNCHRONOUS).computeIfPresent(this.createKey(), this.properties.isTransactional() ? new CopyOnWriteMapRemoveFunction<>(name) : new ConcurrentMapRemoveFunction<>(name)));

        Object result = this.read(name, this.attributeCache.getAdvancedCache().withFlags(Flag.FORCE_SYNCHRONOUS).remove(this.createKey(attributeId)));
        this.mutations.remove(name);
        return result;
    }

    @Override
    public Object setAttribute(String name, Object attribute) {
        if (attribute == null) {
            return this.removeAttribute(name);
        }
        if (this.properties.isMarshalling() && !this.marshaller.isMarshallable(attribute)) {
            throw new IllegalArgumentException(new NotSerializableException(attribute.getClass().getName()));
        }

        V value = this.marshaller.write(attribute);
        UUID attributeId = this.names.get(name);
        if (attributeId == null) {
            UUID newAttributeId = UUID.randomUUID();
            this.setNames(this.namesCache.getAdvancedCache().withFlags(Flag.FORCE_SYNCHRONOUS).compute(this.createKey(), this.properties.isTransactional() ? new CopyOnWriteMapPutFunction<>(name, newAttributeId) : new ConcurrentMapPutFunction<>(name, newAttributeId)));
            attributeId = this.names.get(name);
        }

        SessionAttributeKey key = this.createKey(attributeId);
        Object result = this.read(name, this.attributeCache.getAdvancedCache().withFlags(Flag.FORCE_SYNCHRONOUS).put(key, value));
        if (this.properties.isTransactional()) {
            // Add a passive mutation to prevent any subsequent mutable getAttribute(...) from triggering a redundant mutation on close.
            this.mutations.put(name, Mutator.PASSIVE);
        } else {
            // If the object is mutable, we need to indicate trigger a mutation on close
            if (this.immutability.test(attribute)) {
                this.mutations.remove(name);
            } else {
                this.mutations.put(name, new CacheEntryMutator<>(this.attributeCache, key, value));
            }
        }
        return result;
    }

    @Override
    public Object getAttribute(String name) {
        UUID attributeId = this.names.get(name);
        if (attributeId == null) return null;

        SessionAttributeKey key = this.createKey(attributeId);
        V value = this.attributeCache.get(key);
        Object attribute = this.read(name, value);
        if (attribute != null) {
            // If the object is mutable, we need to trigger a mutation on close
            if (!this.immutability.test(attribute)) {
                this.mutations.putIfAbsent(name, new CacheEntryMutator<>(this.attributeCache, key, value));
            }
        }
        return attribute;
    }

    @Override
    public Set<String> getAttributeNames() {
        return this.names.keySet();
    }

    @Override
    public void close() {
        for (Mutator mutator : this.mutations.values()) {
            mutator.mutate();
        }
        this.mutations.clear();
    }

    private void setNames(Map<String, UUID> names) {
        this.names = (names != null) ? Collections.unmodifiableMap(names) : Collections.emptyMap();
    }

    private SessionAttributeNamesKey createKey() {
        return new SessionAttributeNamesKey(this.id);
    }

    private SessionAttributeKey createKey(UUID attributeId) {
        return new SessionAttributeKey(this.id, attributeId);
    }

    private Object read(String name, V value) {
        try {
            return this.marshaller.read(value);
        } catch (InvalidSerializedFormException e) {
            // This should not happen here, since attributes were pre-activated during FineSessionFactory.findValue(...)
            throw InfinispanWebLogger.ROOT_LOGGER.failedToReadSessionAttribute(e, this.id, name);
        }
    }
}
