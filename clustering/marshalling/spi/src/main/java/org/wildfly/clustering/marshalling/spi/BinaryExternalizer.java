/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.OptionalInt;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.wildfly.clustering.marshalling.Externalizer;

/**
 * Generic {@link Externalizer} for an object composed of 2 externalizable components.
 * @author Paul Ferraro
 */
public class BinaryExternalizer<T, X, Y> implements Externalizer<T> {

    private final Class<T> targetClass;
    private final Externalizer<X> externalizer1;
    private final Externalizer<Y> externalizer2;
    private final Function<T, X> accessor1;
    private final Function<T, Y> accessor2;
    private final BiFunction<X, Y, T> factory;

    public BinaryExternalizer(Class<T> targetClass, Externalizer<X> externalizer1, Externalizer<Y> externalizer2, Function<T, X> accessor1, Function<T, Y> accessor2, BiFunction<X, Y, T> factory) {
        this.targetClass = targetClass;
        this.externalizer1 = externalizer1;
        this.externalizer2 = externalizer2;
        this.accessor1 = accessor1;
        this.accessor2 = accessor2;
        this.factory = factory;
    }

    @Override
    public void writeObject(ObjectOutput output, T object) throws IOException {
        this.externalizer1.writeObject(output, this.accessor1.apply(object));
        this.externalizer2.writeObject(output, this.accessor2.apply(object));
    }

    @Override
    public T readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        return this.factory.apply(this.externalizer1.readObject(input), this.externalizer2.readObject(input));
    }

    @Override
    public Class<T> getTargetClass() {
        return this.targetClass;
    }

    @Override
    public OptionalInt size(T object) {
        OptionalInt size1 = this.externalizer1.size(this.accessor1.apply(object));
        if (size1.isPresent()) {
            OptionalInt size2 = this.externalizer2.size(this.accessor2.apply(object));
            if (size2.isPresent()) {
                return OptionalInt.of(size1.getAsInt() + size2.getAsInt());
            }
        }
        return OptionalInt.empty();
    }
}
