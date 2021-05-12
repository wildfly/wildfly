/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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
import java.util.LinkedList;
import java.util.List;

import org.infinispan.protostream.descriptors.WireType;

/**
 * Marshaller for an Object array, using a repeated element field.
 * @author Paul Ferraro
 */
public class TypedArrayMarshaller implements FieldMarshaller<Object> {

    private final ScalarMarshaller<Class<?>> componentType;

    public TypedArrayMarshaller(ScalarMarshaller<Class<?>> componentType) {
        this.componentType = componentType;
    }

    @Override
    public Object readFrom(ProtoStreamReader reader) throws IOException {
        Class<?> componentType = this.componentType.readFrom(reader);
        List<Object> list = new LinkedList<>();
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            int index = WireType.getTagFieldNumber(tag);
            if (index == AnyField.ANY.getIndex()) {
                list.add(Scalar.ANY.readFrom(reader));
            } else {
                reader.skipField(tag);
            }
        }
        Object array = Array.newInstance((componentType == Any.class) ? Object.class : componentType, list.size());
        int index = 0;
        for (Object element : list) {
            Array.set(array, index++, element);
        }
        return array;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, Object array) throws IOException {
        this.componentType.writeTo(writer, array.getClass().getComponentType());
        for (int i = 0; i < Array.getLength(array); ++i) {
            Object element = Array.get(array, i);
            writer.writeTag(AnyField.ANY.getIndex(), Scalar.ANY.getWireType());
            Scalar.ANY.writeTo(writer, element);
        }
    }

    @Override
    public Class<? extends Object> getJavaClass() {
        return Object.class;
    }

    @Override
    public WireType getWireType() {
        return this.componentType.getWireType();
    }
}
