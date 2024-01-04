/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.descriptors.WireType;

/**
 * Generic marshaller for instances of {@link Class}.
 * @author Paul Ferraro
 */
public class ClassMarshaller implements ProtoStreamMarshaller<Class<?>> {

    private final Field<Class<?>> field;

    public ClassMarshaller(ClassLoaderMarshaller marshaller) {
        ClassField[] fields = ClassField.values();
        this.field = new LoadedClassField(marshaller, fields[fields.length - 1].getIndex() + 1);
    }

    @Override
    public Class<?> readFrom(ProtoStreamReader reader) throws IOException {
        Class<?> result = Object.class;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            int index = WireType.getTagFieldNumber(tag);
            Field<Class<?>> field = index == this.field.getIndex() ? this.field : ClassField.fromIndex(index);
            if (field != null) {
                result = field.getMarshaller().readFrom(reader);
            } else {
                reader.skipField(tag);
            }
        }
        return result;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, Class<?> targetClass) throws IOException {
        if (targetClass != Object.class) {
            Field<Class<?>> field = this.getField(writer.getSerializationContext(), targetClass);
            writer.writeTag(field.getIndex(), field.getMarshaller().getWireType());
            field.getMarshaller().writeTo(writer, targetClass);
        }
    }

    Field<Class<?>> getField(ImmutableSerializationContext context, Class<?> targetClass) {
        AnyField classField = AnyField.fromJavaType(targetClass);
        if (classField != null) return ClassField.FIELD;
        if (targetClass.isArray()) return ClassField.ARRAY;
        try {
            BaseMarshaller<?> marshaller = context.getMarshaller(targetClass);
            if (marshaller.getJavaClass() != targetClass) return this.field;
            return context.getDescriptorByName(marshaller.getTypeName()).getTypeId() != null ? ClassField.ID : ClassField.NAME;
        } catch (IllegalArgumentException e) {
            // If class does not represent a registered type, then use the loader based marshaller.
            return this.field;
        }
    }

    @Override
    public Class<? extends Class<?>> getJavaClass() {
        return this.field.getMarshaller().getJavaClass();
    }
}
