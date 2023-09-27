/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.OptionalInt;

import org.wildfly.clustering.marshalling.Externalizer;

/**
 * Trivial {@link Externalizer} for a constant value.
 * @author Paul Ferraro
 */
public class ValueExternalizer<T> implements Externalizer<T> {
    public static final Externalizer<Void> VOID = new ValueExternalizer<>(null);

    private final T value;

    public ValueExternalizer(T value) {
        this.value = value;
    }

    @Override
    public void writeObject(ObjectOutput output, T object) throws IOException {
        // Nothing to write
    }

    @Override
    public T readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        return this.value;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<T> getTargetClass() {
        return (Class<T>) this.value.getClass();
    }

    @Override
    public OptionalInt size(T object) {
        return OptionalInt.of(0);
    }
}
