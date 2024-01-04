/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A wrapper for an arbitrary object.
 * @author Paul Ferraro
 */
class Any implements Supplier<Object> {

    private final Object value;

    Any(Object value) {
        this.value = value;
    }

    @Override
    public Object get() {
        return this.value;
    }

    @Override
    public int hashCode() {
        return (this.value != null) ? this.value.hashCode() : 0;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Any)) return false;
        return Objects.equals(this.value, ((Any) object).value);
    }

    @Override
    public String toString() {
        return (this.value != null) ? this.value.toString() : null;
    }
}
