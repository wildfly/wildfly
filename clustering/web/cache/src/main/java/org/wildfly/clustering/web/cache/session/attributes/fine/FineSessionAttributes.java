/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.attributes.fine;

import java.io.IOException;
import java.io.NotSerializableException;
import java.util.Map;
import java.util.TreeMap;

import org.wildfly.clustering.ee.Immutability;
import org.wildfly.clustering.ee.MutatorFactory;
import org.wildfly.clustering.ee.cache.CacheProperties;
import org.wildfly.clustering.marshalling.spi.Marshaller;
import org.wildfly.clustering.web.cache.session.attributes.SessionAttributes;
import org.wildfly.clustering.web.cache.session.attributes.SimpleImmutableSessionAttributes;

/**
 * Exposes session attributes for a fine granularity sessions.
 * @author Paul Ferraro
 */
public class FineSessionAttributes<K, V> extends SimpleImmutableSessionAttributes implements SessionAttributes {

    private final K key;
    private final Map<String, Object> attributes;
    private final Marshaller<Object, V> marshaller;
    private final MutatorFactory<K, Map<String, V>> mutatorFactory;
    private final Immutability immutability;
    private final CacheProperties properties;
    private final SessionAttributeActivationNotifier notifier;
    private final Map<String, Object> updates = new TreeMap<>();

    public FineSessionAttributes(K key, Map<String, Object> attributes, MutatorFactory<K, Map<String, V>> mutatorFactory, Marshaller<Object, V> marshaller, Immutability immutability, CacheProperties properties, SessionAttributeActivationNotifier notifier) {
        super(attributes);
        this.key = key;
        this.attributes = attributes;
        this.mutatorFactory = mutatorFactory;
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
        Object value = this.attributes.get(name);

        if (value != null) {
            // If the object is mutable, we need to mutate this value on close
            synchronized (this.updates) {
                // Bypass immutability check if we are already updating this attribute
                if (!this.updates.containsKey(name) && !this.immutability.test(value)) {
                    this.updates.put(name, value);
                }
            }
        }

        return value;
    }

    @Override
    public Object removeAttribute(String name) {
        Object result = this.attributes.remove(name);

        if (result != null) {
            synchronized (this.updates) {
                this.updates.put(name, null);
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

        Object result = this.attributes.put(name, value);

        // Always trigger attribute update, even if called with an existing reference
        synchronized (this.updates) {
            this.updates.put(name, value);
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
        synchronized (this.updates) {
            if (!this.updates.isEmpty()) {
                Map<String, V> updates = new TreeMap<>();
                for (Map.Entry<String, Object> entry : this.updates.entrySet()) {
                    String name = entry.getKey();
                    Object value = entry.getValue();
                    updates.put(name, (value != null) ? this.write(value) : null);
                }

                this.mutatorFactory.createMutator(this.key, updates).mutate();
            }
        }
    }

    private V write(Object value) {
        try {
            return this.marshaller.write(value);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
