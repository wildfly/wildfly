/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.OptionalInt;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

import org.wildfly.clustering.marshalling.Externalizer;

/**
 * Base {@link Externalizer} for int-based externalization.
 * @author Paul Ferraro
 */
public class IntExternalizer<T> implements Externalizer<T> {
    private final IntFunction<T> reader;
    private final ToIntFunction<T> writer;
    private final Class<T> targetClass;

    public IntExternalizer(Class<T> targetClass, IntFunction<T> reader, ToIntFunction<T> writer) {
        this.reader = reader;
        this.writer = writer;
        this.targetClass = targetClass;
    }

    @Override
    public void writeObject(ObjectOutput output, T object) throws IOException {
        output.writeInt(this.writer.applyAsInt(object));
    }

    @Override
    public T readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        return this.reader.apply(input.readInt());
    }

    @Override
    public Class<T> getTargetClass() {
        return this.targetClass;
    }

    @Override
    public OptionalInt size(T object) {
        return OptionalInt.of(Integer.BYTES);
    }
}
