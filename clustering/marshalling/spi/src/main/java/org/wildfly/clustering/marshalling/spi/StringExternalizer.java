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
 * Base {@link Externalizer} for string-based externalization.
 * @author Paul Ferraro
 */
public class StringExternalizer<T> implements Externalizer<T> {
    private final Formatter<T> formatter;

    public StringExternalizer(Formatter<T> formatter) {
        this.formatter = formatter;
    }

    public StringExternalizer(Class<T> targetClass, Function<String, T> reader) {
        this.formatter = new SimpleFormatter<>(targetClass, reader);
    }

    public StringExternalizer(Class<T> targetClass, Function<String, T> reader, Function<T, String> writer) {
        this.formatter = new SimpleFormatter<>(targetClass, reader, writer);
    }

    @Override
    public void writeObject(ObjectOutput output, T object) throws IOException {
        output.writeUTF(this.formatter.format(object));
    }

    @Override
    public T readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        return this.formatter.parse(input.readUTF());
    }

    @Override
    public OptionalInt size(T object) {
        return OptionalInt.of(this.formatter.format(object).length() + 1);
    }

    @Override
    public Class<T> getTargetClass() {
        return this.formatter.getTargetClass();
    }
}
