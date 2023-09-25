/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi.util;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;
import java.util.function.BiFunction;

import org.wildfly.clustering.marshalling.Externalizer;

/**
 * Externalizer for {@link Map.Entry} types
 * @author Paul Ferraro
 */
public class MapEntryExternalizer<T extends Map.Entry<Object, Object>> implements Externalizer<T> {

    private final Class<T> targetClass;
    private final BiFunction<Object, Object, T> factory;

    @SuppressWarnings("unchecked")
    public MapEntryExternalizer(Class<?> targetClass, BiFunction<Object, Object, T> factory) {
        this.targetClass = (Class<T>) targetClass;
        this.factory = factory;
    }

    @Override
    public void writeObject(ObjectOutput output, T entry) throws IOException {
        output.writeObject(entry.getKey());
        output.writeObject(entry.getValue());
    }

    @Override
    public T readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        return this.factory.apply(input.readObject(), input.readObject());
    }

    @Override
    public Class<T> getTargetClass() {
        return this.targetClass;
    }
}
