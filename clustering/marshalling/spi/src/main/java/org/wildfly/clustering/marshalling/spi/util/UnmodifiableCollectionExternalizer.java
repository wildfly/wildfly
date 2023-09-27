/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi.util;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.function.Function;

import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.IndexSerializer;

/**
 * Externalizer for unmodifiable collections created via {@link java.util.List#of()} or {@link java.util.Set#of()} methods.
 * @author Paul Ferraro
 */
public class UnmodifiableCollectionExternalizer<T extends Collection<Object>> implements Externalizer<T> {

    private final Class<T> targetClass;
    private final Function<Object[], T> factory;

    public UnmodifiableCollectionExternalizer(Class<T> targetClass, Function<Object[], T> factory) {
        this.targetClass = targetClass;
        this.factory = factory;
    }

    @Override
    public void writeObject(ObjectOutput output, T collection) throws IOException {
        IndexSerializer.VARIABLE.writeInt(output, collection.size());
        for (Object object : collection) {
            output.writeObject(object);
        }
    }

    @Override
    public T readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        Object[] elements = new Object[IndexSerializer.VARIABLE.readInt(input)];
        for (int i = 0; i < elements.length; ++i) {
            elements[i] = input.readObject();
        }
        return this.factory.apply(elements);
    }

    @Override
    public Class<T> getTargetClass() {
        return this.targetClass;
    }
}
