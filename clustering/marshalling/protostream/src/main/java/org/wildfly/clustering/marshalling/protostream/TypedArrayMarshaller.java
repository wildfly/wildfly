/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
