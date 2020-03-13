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
package org.wildfly.clustering.web.cache.session.fine;

import java.io.NotSerializableException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.wildfly.clustering.ee.Immutability;
import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.ee.MutatorFactory;
import org.wildfly.clustering.ee.cache.CacheProperties;
import org.wildfly.clustering.ee.cache.function.ConcurrentMapPutFunction;
import org.wildfly.clustering.ee.cache.function.ConcurrentMapRemoveFunction;
import org.wildfly.clustering.ee.cache.function.CopyOnWriteMapPutFunction;
import org.wildfly.clustering.ee.cache.function.CopyOnWriteMapRemoveFunction;
import org.wildfly.clustering.marshalling.spi.InvalidSerializedFormException;
import org.wildfly.clustering.marshalling.spi.Marshaller;
import org.wildfly.clustering.web.cache.session.SessionAttributeActivationNotifier;
import org.wildfly.clustering.web.cache.session.SessionAttributes;

/**
 * Exposes session attributes for fine granularity sessions.
 * @author Paul Ferraro
 */
public class FineSessionAttributes<NK, K, V> implements SessionAttributes {
    private final NK key;
    private final Map<NK, Map<String, UUID>> namesCache;
    private final Function<UUID, K> keyFactory;
    private final Map<K, V> attributeCache;
    private final Map<UUID, Mutator> mutations = new ConcurrentHashMap<>();
    private final Marshaller<Object, V> marshaller;
    private final MutatorFactory<K, V> mutatorFactory;
    private final Immutability immutability;
    private final CacheProperties properties;
    private final SessionAttributeActivationNotifier notifier;

    private volatile Map<String, UUID> names;

    public FineSessionAttributes(NK key, Map<String, UUID> names, Map<NK, Map<String, UUID>> namesCache, Function<UUID, K> keyFactory, Map<K, V> attributeCache, Marshaller<Object, V> marshaller, MutatorFactory<K, V> mutatorFactory, Immutability immutability, CacheProperties properties, SessionAttributeActivationNotifier notifier) {
        this.key = key;
        this.setNames(names);
        this.namesCache = namesCache;
        this.keyFactory = keyFactory;
        this.attributeCache = attributeCache;
        this.marshaller = marshaller;
        this.mutatorFactory = mutatorFactory;
        this.immutability = immutability;
        this.properties = properties;
        this.notifier = notifier;
    }

    @Override
    public Object removeAttribute(String name) {
        UUID attributeId = this.names.get(name);
        if (attributeId == null) return null;

        this.setNames(this.namesCache.computeIfPresent(this.key, this.properties.isTransactional() ? new CopyOnWriteMapRemoveFunction<>(name) : new ConcurrentMapRemoveFunction<>(name)));

        Object result = this.read(this.attributeCache.remove(this.keyFactory.apply(attributeId)));
        if (result != null) {
            this.mutations.remove(attributeId);
            if (this.properties.isPersistent()) {
                this.notifier.postActivate(result);
            }
        }
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

        UUID attributeId = this.names.get(name);
        if (attributeId == null) {
            UUID newAttributeId = UUID.randomUUID();
            this.setNames(this.namesCache.compute(this.key, this.properties.isTransactional() ? new CopyOnWriteMapPutFunction<>(name, newAttributeId) : new ConcurrentMapPutFunction<>(name, newAttributeId)));
            attributeId = this.names.get(name);
        }

        K key = this.keyFactory.apply(attributeId);
        V value = this.marshaller.write(attribute);

        if (this.properties.isPersistent()) {
            this.notifier.prePassivate(attribute);
        }

        Object result = this.read(this.attributeCache.put(key, value));

        if (this.properties.isPersistent()) {
            this.notifier.postActivate(attribute);

            if (result != attribute) {
                this.notifier.postActivate(result);
            }
        }

        if (this.properties.isTransactional()) {
            // Add a passive mutation to prevent any subsequent mutable getAttribute(...) from triggering a redundant mutation on close.
            this.mutations.put(attributeId, Mutator.PASSIVE);
        } else {
            // If the object is mutable, we need to indicate trigger a mutation on close
            if (this.immutability.test(attribute)) {
                this.mutations.remove(attributeId);
            } else {
                this.mutations.put(attributeId, this.mutatorFactory.createMutator(key, value));
            }
        }
        return result;
    }

    @Override
    public Object getAttribute(String name) {
        UUID attributeId = this.names.get(name);
        if (attributeId == null) return null;

        K key = this.keyFactory.apply(attributeId);
        V value = this.attributeCache.get(key);
        Object result = this.read(value);
        if (result != null) {
            if (this.properties.isPersistent()) {
                this.notifier.postActivate(result);
            }

            // If the object is mutable, we need to trigger a mutation on close
            if (!this.immutability.test(result)) {
                this.mutations.put(attributeId, this.mutatorFactory.createMutator(key, value));
            }
        }
        return result;
    }

    @Override
    public Set<String> getAttributeNames() {
        return this.names.keySet();
    }

    @Override
    public void close() {
        this.notifier.close();

        for (Mutator mutator : this.mutations.values()) {
            mutator.mutate();
        }
        this.mutations.clear();
    }

    private void setNames(Map<String, UUID> names) {
        this.names = (names != null) ? Collections.unmodifiableMap(names) : Collections.emptyMap();
    }

    private Object read(V value) {
        try {
            return this.marshaller.read(value);
        } catch (InvalidSerializedFormException e) {
            // This should not happen here, since attributes were pre-activated during session construction
            throw new IllegalStateException(e);
        }
    }
}
