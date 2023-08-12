/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.attributes.fine;

import java.util.Map;

/**
 * @author Paul Ferraro
 */
public class SessionAttributeMapEntry<V> implements Map.Entry<String, V> {

    private final String name;
    private final V value;

    public SessionAttributeMapEntry(Map.Entry<String, V> entry) {
        this(entry.getKey(), entry.getValue());
    }

    public SessionAttributeMapEntry(String name, V value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String getKey() {
        return this.name;
    }

    @Override
    public V getValue() {
        return this.value;
    }

    @Override
    public V setValue(V value) {
        throw new IllegalStateException();
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Map.Entry)) return false;
        Map.Entry<?, ?> entry = (Map.Entry<?, ?>) object;
        return this.name.equals(entry.getKey()) && this.value.equals(entry.getValue());
    }

    @Override
    public String toString() {
        return Map.entry(this.name, this.value).toString();
    }
}
