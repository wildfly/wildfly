/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.OptionalInt;
import java.util.function.LongFunction;
import java.util.function.ToLongFunction;

import org.wildfly.clustering.marshalling.Externalizer;

/**
 * Base {@link Externalizer} for long-based externalization.
 * @author Paul Ferraro
 */
public class LongExternalizer<T> implements Externalizer<T> {
    private final LongFunction<T> reader;
    private final ToLongFunction<T> writer;
    private final Class<T> targetClass;

    public LongExternalizer(Class<T> targetClass, LongFunction<T> reader, ToLongFunction<T> writer) {
        this.reader = reader;
        this.writer = writer;
        this.targetClass = targetClass;
    }

    @Override
    public void writeObject(ObjectOutput output, T object) throws IOException {
        output.writeLong(this.writer.applyAsLong(object));
    }

    @Override
    public T readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        return this.reader.apply(input.readLong());
    }

    @Override
    public Class<T> getTargetClass() {
        return this.targetClass;
    }

    @Override
    public OptionalInt size(T object) {
        return OptionalInt.of(Long.BYTES);
    }
}
