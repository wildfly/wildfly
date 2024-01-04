/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;
import java.util.OptionalInt;

/**
 * Provides a {@link ProtoStreamMarshaller}.
 * @author Paul Ferraro
 */
public interface ProtoStreamMarshallerProvider extends ProtoStreamMarshaller<Object> {
    ProtoStreamMarshaller<?> getMarshaller();

    @Override
    default Object readFrom(ProtoStreamReader reader) throws IOException {
        return this.getMarshaller().readFrom(reader);
    }

    @Override
    default void writeTo(ProtoStreamWriter writer, Object value) throws IOException {
        this.cast(Object.class).writeTo(writer, value);
    }

    @Override
    default Object read(ReadContext context) throws IOException {
        return this.getMarshaller().read(context);
    }

    @Override
    default void write(WriteContext context, Object value) throws IOException {
        this.cast(Object.class).write(context, value);
    }

    @Override
    default OptionalInt size(ProtoStreamSizeOperation operation, Object value) {
        return this.cast(Object.class).size(operation, value);
    }

    @Override
    default Class<? extends Object> getJavaClass() {
        return this.getMarshaller().getJavaClass();
    }

    @SuppressWarnings("unchecked")
    default <T> ProtoStreamMarshaller<T> cast(Class<T> type) {
        if (!type.isAssignableFrom(this.getJavaClass())) {
            throw new IllegalArgumentException(type.getName());
        }
        return (ProtoStreamMarshaller<T>) this.getMarshaller();
    }
}
