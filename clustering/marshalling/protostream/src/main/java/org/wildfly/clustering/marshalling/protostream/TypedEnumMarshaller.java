/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;

import org.infinispan.protostream.descriptors.WireType;

/**
 * Marshaller for a typed enumeration.
 * @author Paul Ferraro
 * @param <E> the enum type of this marshaller
 */
public class TypedEnumMarshaller<E extends Enum<E>> implements FieldMarshaller<E> {
    // Optimize for singleton enums
    private static final int DEFAULT_ORDINAL = 0;

    private final ScalarMarshaller<Class<?>> type;

    public TypedEnumMarshaller(ScalarMarshaller<Class<?>> type) {
        this.type = type;
    }

    @Override
    public E readFrom(ProtoStreamReader reader) throws IOException {
        @SuppressWarnings("unchecked")
        Class<E> enumClass = (Class<E>) this.type.readFrom(reader);
        E result = enumClass.getEnumConstants()[DEFAULT_ORDINAL];
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            int index = WireType.getTagFieldNumber(tag);
            if (index == AnyField.ANY.getIndex()) {
                result = reader.readEnum(enumClass);
            } else {
                reader.skipField(tag);
            }
        }
        return result;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, E value) throws IOException {
        Class<E> enumClass = value.getDeclaringClass();
        this.type.writeTo(writer, enumClass);

        if (value.ordinal() != DEFAULT_ORDINAL) {
            writer.writeEnum(AnyField.ANY.getIndex(), value);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends E> getJavaClass() {
        return (Class<E>) (Class<?>) Enum.class;
    }

    @Override
    public WireType getWireType() {
        return this.type.getWireType();
    }
}
