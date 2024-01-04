/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;

import org.infinispan.protostream.descriptors.WireType;

/**
 * ProtoStream marshaller for enums.
 * @author Paul Ferraro
 * @param <E> the enum type of this marshaller
 */
public class EnumMarshaller<E extends Enum<E>> implements org.infinispan.protostream.EnumMarshaller<E>, ProtoStreamMarshaller<E> {
    // Optimize for singleton enums
    private static final int DEFAULT_ORDINAL = 0;

    private static final int ORDINAL_INDEX = 1;

    private final Class<E> enumClass;
    private final E[] values;

    public EnumMarshaller(Class<E> enumClass) {
        this.enumClass = enumClass;
        this.values = enumClass.getEnumConstants();
    }

    @Override
    public Class<? extends E> getJavaClass() {
        return this.enumClass;
    }

    @Override
    public E decode(int ordinal) {
        return this.values[ordinal];
    }

    @Override
    public int encode(E value) {
        return value.ordinal();
    }

    @Override
    public E readFrom(ProtoStreamReader reader) throws IOException {
        E result = this.values[DEFAULT_ORDINAL];
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case ORDINAL_INDEX:
                    result = reader.readEnum(this.enumClass);
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return result;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, E value) throws IOException {
        if (value.ordinal() != DEFAULT_ORDINAL) {
            writer.writeEnum(ORDINAL_INDEX, value);
        }
    }
}
