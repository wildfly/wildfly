/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;
import java.util.OptionalInt;

import org.infinispan.protostream.descriptors.WireType;

/**
 * Provider for a {@link ScalarMarshaller}.
 * @author Paul Ferraro
 */
public interface ScalarMarshallerProvider extends ScalarMarshaller<Object> {
    ScalarMarshaller<?> getMarshaller();

    @Override
    default Class<? extends Object> getJavaClass() {
        return this.getMarshaller().getJavaClass();
    }

    @Override
    default WireType getWireType() {
        return this.getMarshaller().getWireType();
    }

    @Override
    default Object readFrom(ProtoStreamReader reader) throws IOException {
        return this.getMarshaller().readFrom(reader);
    }

    @Override
    default void writeTo(ProtoStreamWriter writer, Object value) throws IOException {
        this.cast(Object.class).writeTo(writer, value);
    }

    @Override
    default OptionalInt size(ProtoStreamSizeOperation operation, Object value) {
        return this.cast(Object.class).size(operation, value);
    }

    @SuppressWarnings("unchecked")
    default <T> ScalarMarshaller<T> cast(Class<T> type) {
        if (!type.isAssignableFrom(this.getJavaClass())) {
            throw new IllegalArgumentException(type.getName());
        }
        return (ScalarMarshaller<T>) this.getMarshaller();
    }
}
