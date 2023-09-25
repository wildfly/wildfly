/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.infinispan;

import java.util.Objects;

import org.wildfly.clustering.ee.Key;
import org.wildfly.clustering.infinispan.distribution.KeyGroup;

/**
 * An embedded cache key supporting group co-location.
 * @author Paul Ferraro
 */
public class GroupedKey<K> implements Key<K>, KeyGroup<K> {
    private final K id;

    public GroupedKey(K id) {
        this.id = id;
    }

    /**
     * Returns the value of this key.
     * @return the key value
     */
    @Override
    public K getId() {
        return this.id;
    }

    @Override
    public boolean equals(Object object) {
        if ((object == null) || (object.getClass() != this.getClass())) return false;
        @SuppressWarnings("unchecked")
        GroupedKey<K> key = (GroupedKey<K>) object;
        return this.id.equals(key.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getClass().getName(), this.id);
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", this.getClass().getSimpleName(), this.id.toString());
    }
}
