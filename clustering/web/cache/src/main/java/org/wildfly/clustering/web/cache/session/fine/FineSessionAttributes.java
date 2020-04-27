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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

import org.wildfly.clustering.ee.Immutability;
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
    private final Map<K, Optional<Object>> mutations = new ConcurrentHashMap<>();
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

        this.setNames(this.namesCache.compute(this.key, this.properties.isTransactional() ? new CopyOnWriteMapRemoveFunction<>(name) : new ConcurrentMapRemoveFunction<>(name)));

        K key = this.keyFactory.apply(attributeId);
        Object result = this.read(this.attributeCache.remove(key));
        if (result != null) {
            this.mutations.remove(key);
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
            UUID newAttributeId = createUUID();
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
            // Add an empty value to prevent any subsequent mutable getAttribute(...) from triggering a redundant mutation on close.
            this.mutations.put(key, Optional.empty());
        } else {
            // If the object is mutable, we need to indicate trigger a mutation on close
            if (this.immutability.test(attribute)) {
                this.mutations.remove(key);
            } else {
                this.mutations.put(key, Optional.of(attribute));
            }
        }
        return result;
    }

    @Override
    public Object getAttribute(String name) {
        UUID attributeId = this.names.get(name);
        if (attributeId == null) return null;

        K key = this.keyFactory.apply(attributeId);

        // Return mutable value if present, this preserves referential integrity when this member is not an owner.
        Optional<Object> mutableValue = this.mutations.get(key);
        if ((mutableValue != null) && mutableValue.isPresent()) {
            return mutableValue.get();
        }

        V value = this.attributeCache.get(key);
        Object result = this.read(value);
        if (result != null) {
            if (this.properties.isPersistent()) {
                this.notifier.postActivate(result);
            }

            // If the object is mutable, we need to trigger a mutation on close
            if (!this.immutability.test(result)) {
                this.mutations.putIfAbsent(key, Optional.of(result));
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

        for (Map.Entry<K, Optional<Object>> entry : this.mutations.entrySet()) {
            Optional<Object> optional = entry.getValue();
            if (optional.isPresent()) {
                K key = entry.getKey();
                V value = this.marshaller.write(optional.get());
                this.mutatorFactory.createMutator(key, value).mutate();
            }
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

    private static UUID createUUID() {
        byte[] data = new byte[16];
        ThreadLocalRandom.current().nextBytes(data);
        data[6] &= 0x0f; /* clear version */
        data[6] |= 0x40; /* set to version 4 */
        data[8] &= 0x3f; /* clear variant */
        data[8] |= 0x80; /* set to IETF variant */
        long msb = 0;
        long lsb = 0;
        for (int i = 0; i < 8; i++) {
           msb = (msb << 8) | (data[i] & 0xff);
        }
        for (int i = 8; i < 16; i++) {
           lsb = (lsb << 8) | (data[i] & 0xff);
        }
        return new UUID(msb, lsb);
    }
}
