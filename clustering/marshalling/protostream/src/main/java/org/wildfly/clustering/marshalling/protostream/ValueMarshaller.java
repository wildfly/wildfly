/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;
import java.util.function.Supplier;

import org.wildfly.common.function.Functions;

/**
 * ProtoStream marshaller for fixed values.
 * @author Paul Ferraro
 * @param <T> the type of this marshaller
 */
public class ValueMarshaller<T> implements ProtoStreamMarshaller<T> {

    private final Class<T> targetClass;
    private final Supplier<T> factory;

    public ValueMarshaller(T value) {
        this(Functions.constantSupplier(value));
    }

    @SuppressWarnings("unchecked")
    public ValueMarshaller(Supplier<T> factory) {
        this.targetClass = (Class<T>) factory.get().getClass();
        this.factory = factory;
    }

    @Override
    public T readFrom(ProtoStreamReader reader) throws IOException {
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            reader.skipField(tag);
        }
        return this.factory.get();
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, T value) {
        // Nothing to write
    }

    @Override
    public Class<? extends T> getJavaClass() {
        return this.targetClass;
    }
}
