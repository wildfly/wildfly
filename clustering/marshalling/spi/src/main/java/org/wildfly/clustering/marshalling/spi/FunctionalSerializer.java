/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.function.Function;

/**
 * A serializer that delegates to the serializer of a mapped value.
 * @author Paul Ferraro
 * @param <T> the target type
 * @param <V> the mapped type
 */
public class FunctionalSerializer<T, V> implements Serializer<T> {

    private final Serializer<V> serializer;
    private final Function<T, V> accessor;
    private final Function<V, T> factory;

    public FunctionalSerializer(Serializer<V> serializer, Function<T, V> accessor, Function<V, T> factory) {
        this.serializer = serializer;
        this.accessor = accessor;
        this.factory = factory;
    }

    @Override
    public void write(DataOutput output, T value) throws IOException {
        this.serializer.write(output, this.accessor.apply(value));
    }

    @Override
    public T read(DataInput input) throws IOException {
        return this.factory.apply(this.serializer.read(input));
    }
}
