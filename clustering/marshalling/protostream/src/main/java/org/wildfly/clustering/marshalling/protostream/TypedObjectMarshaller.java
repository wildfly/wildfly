/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;

import org.infinispan.protostream.descriptors.WireType;

/**
 * Marshaller for a typed object.
 * @author Paul Ferraro
 */
public class TypedObjectMarshaller implements FieldMarshaller<Object> {

    private final ScalarMarshaller<Class<?>> type;

    public TypedObjectMarshaller(ScalarMarshaller<Class<?>> typeValue) {
        this.type = typeValue;
    }

    @Override
    public Object readFrom(ProtoStreamReader reader) throws IOException {
        Class<?> targetClass = this.type.readFrom(reader);
        Object result = null;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            int index = WireType.getTagFieldNumber(tag);
            if (index == AnyField.ANY.getIndex()) {
                result = reader.readObject(targetClass);
            } else {
                reader.skipField(tag);
            }
        }
        return result;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, Object value) throws IOException {
        this.type.writeTo(writer, value.getClass());
        writer.writeObject(AnyField.ANY.getIndex(), value);
    }

    @Override
    public Class<? extends Object> getJavaClass() {
        return Object.class;
    }

    @Override
    public WireType getWireType() {
        return this.type.getWireType();
    }
}
