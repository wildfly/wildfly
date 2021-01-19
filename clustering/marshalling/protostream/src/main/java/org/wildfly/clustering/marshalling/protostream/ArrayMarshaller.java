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
import java.lang.reflect.Array;
import java.util.OptionalInt;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.impl.WireFormat;

import protostream.com.google.protobuf.CodedOutputStream;

/**
 * @author Paul Ferraro
 */
public class ArrayMarshaller implements ScalarMarshaller<Object> {
    private final Class<? extends Object> arrayClass;
    private final ScalarMarshaller<Class<?>> componentTypeMarshaller;
    private final ScalarMarshaller<Object> elementMarshaller;

    public ArrayMarshaller(ScalarMarshaller<Class<?>> componentTypeMarshaller, ScalarMarshaller<Object> elementMarshaller) {
        this(Object.class, componentTypeMarshaller, elementMarshaller);
    }

    public ArrayMarshaller(Class<? extends Object> arrayClass, ScalarMarshaller<Class<?>> componentTypeMarshaller, ScalarMarshaller<Object> elementMarshaller) {
        this.arrayClass = arrayClass;
        this.componentTypeMarshaller = componentTypeMarshaller;
        this.elementMarshaller = elementMarshaller;
    }

    @Override
    public Object readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        int length = reader.readUInt32();
        Class<?> componentType = this.componentTypeMarshaller.readFrom(context, reader);
        Object array = Array.newInstance((componentType == Any.class) ? Object.class : componentType, length);
        for (int i = 0; i < length; ++i) {
            Object element = this.elementMarshaller.readFrom(context, reader);
            Array.set(array, i, element);
        }
        return array;
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object array) throws IOException {
        int length = Array.getLength(array);
        writer.writeUInt32NoTag(length);
        this.componentTypeMarshaller.writeTo(context, writer, array.getClass().getComponentType());
        for (int i = 0; i < length; ++i) {
            Object element = Array.get(array, i);
            this.elementMarshaller.writeTo(context, writer, element);
        }
    }

    @Override
    public OptionalInt size(ImmutableSerializationContext context, Object array) {
        OptionalInt componentTypeSize = this.componentTypeMarshaller.size(context, array.getClass().getComponentType());
        if (componentTypeSize.isPresent()) {
            int length = Array.getLength(array);
            int size = CodedOutputStream.computeUInt32SizeNoTag(length);
            for (int i = 0; i < length; ++i) {
                Object element = Array.get(array, i);
                OptionalInt elementSize = this.elementMarshaller.size(context, element);
                if (elementSize.isPresent()) {
                    size += elementSize.getAsInt();
                } else {
                    return OptionalInt.empty();
                }
            }
            return OptionalInt.of(size + componentTypeSize.getAsInt());
        }
        return OptionalInt.empty();
    }

    @Override
    public Class<? extends Object> getJavaClass() {
        return this.arrayClass;
    }

    @Override
    public int getWireType() {
        return WireFormat.WIRETYPE_LENGTH_DELIMITED;
    }
}
