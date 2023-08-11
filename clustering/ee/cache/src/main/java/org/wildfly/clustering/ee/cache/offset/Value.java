/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.offset;

import java.util.function.Supplier;

/**
 * A mutable reference to some value.
 * @author Paul Ferraro
 * @param <V> the referenced value type
 */
public interface Value<V> extends Supplier<V> {

    /**
     * Updates the referenced value.
     * @param value the new value.
     */
    void set(V value);

    abstract class AbstractValue<V> implements Value<V> {

        AbstractValue() {
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof Value)) return false;
            @SuppressWarnings("unchecked")
            Value<V> value = (Value<V>) object;
            return this.get().equals(value.get());
        }

        @Override
        public int hashCode() {
            return this.get().hashCode();
        }

        @Override
        public String toString() {
            return this.get().toString();
        }
    }
}
