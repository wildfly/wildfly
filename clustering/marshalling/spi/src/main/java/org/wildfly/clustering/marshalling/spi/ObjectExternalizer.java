/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.function.Function;

import org.wildfly.clustering.marshalling.Externalizer;

/**
 * Base {@link Externalizer} for object wrapper externalization.
 * @author Paul Ferraro
 */
public class ObjectExternalizer<T> implements Externalizer<T> {
    private final Function<Object, T> reader;
    private final Function<T, Object> writer;
    private final Class<T> targetClass;

    public ObjectExternalizer(Class<T> targetClass, Function<Object, T> reader, Function<T, Object> writer) {
        this.targetClass = targetClass;
        this.reader = reader;
        this.writer = writer;
    }

    @Override
    public void writeObject(ObjectOutput output, T object) throws IOException {
        output.writeObject(this.writer.apply(object));
    }

    @Override
    public T readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        return this.reader.apply(input.readObject());
    }

    @Override
    public Class<T> getTargetClass() {
        return this.targetClass;
    }
}
