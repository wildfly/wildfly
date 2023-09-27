/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.cache.session.fine;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.wildfly.clustering.marshalling.spi.Marshaller;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;

/**
 * Exposes session attributes for fine granularity sessions.
 * @author Paul Ferraro
 */
public class FineImmutableSessionAttributes<K, V> implements ImmutableSessionAttributes {
    private final AtomicReference<Map<String, UUID>> names;
    private final Function<UUID, K> keyFactory;
    private final Map<K, V> attributeCache;
    private final Marshaller<Object, V> marshaller;

    public FineImmutableSessionAttributes(AtomicReference<Map<String, UUID>> names, Function<UUID, K> keyFactory, Map<K, V> attributeCache, Marshaller<Object, V> marshaller) {
        this.names = names;
        this.keyFactory = keyFactory;
        this.attributeCache = attributeCache;
        this.marshaller = marshaller;
    }

    @Override
    public Set<String> getAttributeNames() {
        return this.names.get().keySet();
    }

    @Override
    public Object getAttribute(String name) {
        UUID attributeId = this.names.get().get(name);
        if (attributeId == null) return null;
        K key = this.keyFactory.apply(attributeId);
        return this.read(this.attributeCache.get(key));
    }

    private Object read(V value) {
        try {
            return this.marshaller.read(value);
        } catch (IOException e) {
            // This should not happen here, since attributes were pre-activated when session was constructed
            throw new IllegalStateException(e);
        }
    }
}
