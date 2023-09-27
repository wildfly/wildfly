/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi.util;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;
import java.util.function.Function;

import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.IndexSerializer;

/**
 * Externalizer for unmodifiable maps created via {@link java.util.Map#of()} or {@link java.util.Map#ofEntries()} methods.
 * @author Paul Ferraro
 */
public class UnmodifiableMapExternalizer<T extends Map<Object, Object>> implements Externalizer<T> {

    private final Class<T> targetClass;
    private final Function<Map.Entry<Object, Object>[], T> factory;

    public UnmodifiableMapExternalizer(Class<T> targetClass, Function<Map.Entry<Object, Object>[], T> factory) {
        this.targetClass = targetClass;
        this.factory = factory;
    }

    @Override
    public void writeObject(ObjectOutput output, T map) throws IOException {
        IndexSerializer.VARIABLE.writeInt(output, map.size());
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            output.writeObject(entry.getKey());
            output.writeObject(entry.getValue());
        }
    }

    @Override
    public T readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        @SuppressWarnings("unchecked")
        Map.Entry<Object, Object>[] entries = new Map.Entry[IndexSerializer.VARIABLE.readInt(input)];
        for (int i = 0; i < entries.length; ++i) {
            entries[i] = Map.entry(input.readObject(), input.readObject());
        }
        return this.factory.apply(entries);
    }

    @Override
    public Class<T> getTargetClass() {
        return this.targetClass;
    }
}
