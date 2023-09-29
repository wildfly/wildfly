/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.cache.session.fine;

import java.io.IOException;
import java.io.NotSerializableException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;

import org.wildfly.clustering.ee.Immutability;
import org.wildfly.clustering.ee.UUIDFactory;
import org.wildfly.clustering.ee.cache.CacheProperties;
import org.wildfly.clustering.ee.cache.function.MapComputeFunction;
import org.wildfly.clustering.marshalling.spi.Marshaller;
import org.wildfly.clustering.web.cache.session.SessionAttributeActivationNotifier;
import org.wildfly.clustering.web.cache.session.SessionAttributes;

/**
 * Exposes session attributes for fine granularity sessions.
 * @author Paul Ferraro
 */
public class FineSessionAttributes<NK, AK, AV> extends FineImmutableSessionAttributes implements SessionAttributes {

    private final Map<NK, Map<String, UUID>> namesCache;
    private final Map<AK, AV> attributeCache;
    private final NK namesKey;
    private final Map<String, UUID> names;
    private final Map<UUID, Object> attributes;
    private final Marshaller<Object, AV> marshaller;
    private final Function<UUID, AK> keyFactory;
    private final Immutability immutability;
    private final CacheProperties properties;
    private final SessionAttributeActivationNotifier notifier;
    private final Map<String, UUID> nameUpdates = new TreeMap<>();
    private final Map<UUID, Object> updates = new TreeMap<>();

    public FineSessionAttributes(Map<NK, Map<String, UUID>> namesCache, NK namesKey, Map<String, UUID> names, Map<AK, AV> attributeCache, Function<UUID, AK> keyFactory, Map<UUID, Object> attributes, Marshaller<Object, AV> marshaller, Immutability immutability, CacheProperties properties, SessionAttributeActivationNotifier notifier) {
        super(names, attributes);
        this.namesCache = namesCache;
        this.namesKey = namesKey;
        this.names = names;
        this.attributeCache = attributeCache;
        this.keyFactory = keyFactory;
        this.attributes = attributes;
        this.marshaller = marshaller;
        this.immutability = immutability;
        this.properties = properties;
        this.notifier = notifier;

        if (this.notifier != null) {
            for (Object value : this.attributes.values()) {
                this.notifier.postActivate(value);
            }
        }
    }

    @Override
    public Object getAttribute(String name) {
        UUID id = this.names.get(name);
        Object value = (id != null) ? this.attributes.get(id) : null;

        if (value != null) {
            // If the object is mutable, we need to mutate this value on close
            if (!this.immutability.test(value)) {
                synchronized (this.updates) {
                    this.updates.put(id, value);
                }
            }
        }

        return value;
    }

    @Override
    public Object removeAttribute(String name) {
        UUID id = this.names.remove(name);

        if (id != null) {
            synchronized (this.nameUpdates) {
                this.nameUpdates.put(name, null);
            }
        }

        Object result = (id != null) ? this.attributes.remove(id) : null;

        if (result != null) {
            synchronized (this.updates) {
                this.updates.put(id, null);
            }
        }

        return result;
    }

    @Override
    public Object setAttribute(String name, Object value) {
        if (value == null) {
            return this.removeAttribute(name);
        }

        if (this.properties.isMarshalling() && !this.marshaller.isMarshallable(value)) {
            throw new IllegalArgumentException(new NotSerializableException(value.getClass().getName()));
        }

        UUID id = UUIDFactory.INSECURE.get();
        UUID existing = this.names.putIfAbsent(name, id);
        if (existing == null) {
            synchronized (this.nameUpdates) {
                this.nameUpdates.put(name, id);
            }
        } else {
            id = existing;
        }

        Object result = this.attributes.put(id, value);

        if (value != result) {
            synchronized (this.updates) {
                this.updates.put(id, value);
            }
        }

        return result;
    }

    @Override
    public void close() {
        if (this.notifier != null) {
            for (Object value : this.attributes.values()) {
                this.notifier.prePassivate(value);
            }
        }
        Set<AK> removals = new TreeSet<>();
        synchronized (this.updates) {
            if (!this.updates.isEmpty()) {
                Map<AK, AV> updates = new TreeMap<>();
                for (Map.Entry<UUID, Object> entry : this.updates.entrySet()) {
                    AK key = this.keyFactory.apply(entry.getKey());
                    Object value = entry.getValue();
                    if (value != null) {
                        updates.put(key, this.write(value));
                    } else {
                        removals.add(key);
                    }
                }
                if (!updates.isEmpty()) {
                    this.attributeCache.putAll(updates);
                }
            }
        }
        synchronized (this.nameUpdates) {
            if (!this.nameUpdates.isEmpty()) {
                this.namesCache.compute(this.namesKey, new MapComputeFunction<>(this.nameUpdates));
            }
        }
        for (AK key : removals) {
            this.attributeCache.remove(key);
        }
    }

    private AV write(Object value) {
        try {
            return this.marshaller.write(value);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
