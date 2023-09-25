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
 * {@link Externalizer} based on a {@link Serializer}.
 * @author Paul Ferraro
 */
public class SerializerExternalizer<T> implements Externalizer<T> {

    private final Class<T> targetClass;
    private final Serializer<T> serializer;

    public SerializerExternalizer(Class<T> targetClass, Serializer<T> serializer) {
        this.targetClass = targetClass;
        this.serializer = serializer;
    }

    @Override
    public void writeObject(ObjectOutput output, T object) throws IOException {
        this.serializer.write(output, object);
    }

    @Override
    public T readObject(ObjectInput input) throws IOException {
        return this.serializer.read(input);
    }

    @Override
    public Class<T> getTargetClass() {
        return this.targetClass;
    }

    @Override
    public OptionalInt size(T object) {
        return this.serializer.size(object);
    }
}
