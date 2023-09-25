/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.OptionalInt;
import java.util.function.Function;

import org.wildfly.clustering.marshalling.Externalizer;

/**
 * Base {@link Externalizer} for boolean-based externalization.
 * @author Paul Ferraro
 */
public class BooleanExternalizer<T> implements Externalizer<T> {

    private final Class<T> targetClass;
    private final Function<Boolean, T> reader;
    private final Function<T, Boolean> writer;

    public BooleanExternalizer(Class<T> targetClass, Function<Boolean, T> reader, Function<T, Boolean> writer) {
        this.targetClass = targetClass;
        this.reader = reader;
        this.writer = writer;
    }

    @Override
    public void writeObject(ObjectOutput output, T object) throws IOException {
        output.writeBoolean(this.writer.apply(object));
    }

    @Override
    public T readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        return this.reader.apply(input.readBoolean());
    }

    @Override
    public Class<T> getTargetClass() {
        return this.targetClass;
    }

    @Override
    public OptionalInt size(T object) {
        return OptionalInt.of(Byte.BYTES);
    }
}
