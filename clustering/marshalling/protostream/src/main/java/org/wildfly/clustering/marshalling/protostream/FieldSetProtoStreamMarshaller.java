/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;

import org.infinispan.protostream.descriptors.WireType;

/**
 * Marshaller for an object whose fields are fully marshallable via a {@link FieldSetMarshaller}.
 * @param <T> the writer type
 * @param <V> the reader type
 */
public class FieldSetProtoStreamMarshaller<T, V> implements ProtoStreamMarshaller<T> {
    private static final int START_INDEX = 1;

    private final Class<T> targetClass;
    private final FieldSetMarshaller<T, V> marshaller;

    @SuppressWarnings("unchecked")
    public FieldSetProtoStreamMarshaller(FieldSetMarshaller<T, V> marshaller) {
        this((Class<T>) marshaller.build(marshaller.createInitialValue()).getClass(), marshaller);
    }

    public FieldSetProtoStreamMarshaller(Class<T> targetClass, FieldSetMarshaller<T, V> marshaller) {
        this.targetClass = targetClass;
        this.marshaller = marshaller;
    }

    @Override
    public T readFrom(ProtoStreamReader reader) throws IOException {
        FieldSetReader<V> valueReader = reader.createFieldSetReader(this.marshaller, START_INDEX);
        V value = this.marshaller.createInitialValue();
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            int index = WireType.getTagFieldNumber(tag);
            if (valueReader.contains(index)) {
                value = valueReader.readField(value);
            } else {
                reader.skipField(tag);
            }
        }
        return this.marshaller.build(value);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, T value) throws IOException {
        writer.createFieldSetWriter(this.marshaller, START_INDEX).writeFields(value);
    }

    @Override
    public Class<? extends T> getJavaClass() {
        return this.targetClass;
    }
}
