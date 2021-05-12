/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
