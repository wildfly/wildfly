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
import java.util.OptionalInt;

import org.infinispan.protostream.EnumMarshaller;
import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;

/**
 * @author Paul Ferraro
 */
public class TypedEnumMarshaller<E extends Enum<E>> implements ProtoStreamMarshaller<E> {

    private final ProtoStreamMarshaller<Class<?>> typeMarshaller;

    public TypedEnumMarshaller(ProtoStreamMarshaller<Class<?>> typeMarshaller) {
        this.typeMarshaller = typeMarshaller;
    }

    @Override
    public E readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        int code = reader.readUInt32();
        @SuppressWarnings("unchecked")
        Class<E> enumClass = (Class<E>) this.typeMarshaller.readFrom(context, reader);
        EnumMarshaller<E> marshaller = (EnumMarshaller<E>) context.getMarshaller(enumClass);
        return marshaller.decode(code);
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, E value) throws IOException {
        Class<E> enumClass = value.getDeclaringClass();
        EnumMarshaller<E> marshaller = (EnumMarshaller<E>) context.getMarshaller(enumClass);

        int code = marshaller.encode(value);
        writer.writeUInt32NoTag(code);
        this.typeMarshaller.writeTo(context, writer, enumClass);
    }

    @Override
    public OptionalInt size(ImmutableSerializationContext context, E value) {
        OptionalInt typeSize = this.typeMarshaller.size(context, value.getDeclaringClass());
        return typeSize.isPresent() ? OptionalInt.of(typeSize.getAsInt() + Predictable.unsignedIntSize(value.ordinal())) : typeSize;
    }

    @Override
    public Class<? extends E> getJavaClass() {
        return null;
    }
}
