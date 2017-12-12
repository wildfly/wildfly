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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.infinispan.Cache;
import org.infinispan.commons.marshall.NotSerializableException;
import org.infinispan.context.Flag;
import org.wildfly.clustering.ee.infinispan.CacheProperties;
import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.ee.infinispan.CacheEntryMutator;
import org.wildfly.clustering.marshalling.spi.Marshaller;
import org.wildfly.clustering.web.infinispan.session.SessionAttributes;
import org.wildfly.clustering.web.session.SessionAttributeImmutability;

/**
 * Exposes session attributes for fine granularity sessions.
 * @author Paul Ferraro
 */
public class FineSessionAttributes<V> extends FineImmutableSessionAttributes<V> implements SessionAttributes {
    private final AtomicInteger sequence;
    private final ConcurrentMap<String, Integer> names;
    private final Mutator namesMutator;
    private final Cache<SessionAttributeKey, V> cache;
    private final Map<String, Mutator> mutations = new ConcurrentHashMap<>();
    private final Marshaller<Object, V> marshaller;
    private final CacheProperties properties;

    public FineSessionAttributes(String id, AtomicInteger sequence, ConcurrentMap<String, Integer> names, Mutator namesMutator, Cache<SessionAttributeKey, V> cache, Marshaller<Object, V> marshaller, CacheProperties properties) {
        super(id, names, cache, marshaller);
        this.sequence = sequence;
        this.names = names;
        this.namesMutator = namesMutator;
        this.cache = cache;
        this.marshaller = marshaller;
        this.properties = properties;
    }

    @Override
    public Object removeAttribute(String name) {
        Integer attributeId = this.names.remove(name);
        if (attributeId == null) return null;
        this.namesMutator.mutate();
        SessionAttributeKey key = this.createKey(attributeId);
        Object result = this.read(name, this.cache.getAdvancedCache().withFlags(Flag.FORCE_SYNCHRONOUS).remove(key));
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
        int currentId = this.sequence.get();
        int attributeId = this.names.computeIfAbsent(name, key -> this.sequence.incrementAndGet());
        if (attributeId > currentId) {
            this.namesMutator.mutate();
        }
        SessionAttributeKey key = this.createKey(attributeId);
        Object result = this.read(name, this.cache.getAdvancedCache().withFlags(Flag.FORCE_SYNCHRONOUS).put(key, value));
        this.mutations.remove(name);
        return result;
    }

    @Override
    public Object getAttribute(String name) {
        Integer attributeId = this.names.get(name);
        if (attributeId == null) return null;
        SessionAttributeKey key = this.createKey(attributeId);
        V value = this.cache.get(key);
        Object attribute = this.read(name, value);
        if (attribute != null) {
            // If the object is mutable, we need to indicate that the attribute should be replicated
            if (!SessionAttributeImmutability.INSTANCE.test(attribute)) {
                Mutator mutator = this.mutations.computeIfAbsent(name, k -> new CacheEntryMutator<>(this.cache, key, value));
                // If cache is not transactional, mutate on close instead.
                if (this.properties.isTransactional()) {
                    mutator.mutate();
                }
            }
        }
        return attribute;
    }

    @Override
    public void close() {
        if (!this.properties.isTransactional()) {
            for (Mutator mutator : this.mutations.values()) {
                mutator.mutate();
            }
        }
        this.mutations.clear();
    }
}
