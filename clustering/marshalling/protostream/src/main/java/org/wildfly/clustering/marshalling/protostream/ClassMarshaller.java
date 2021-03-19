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

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.impl.WireFormat;

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
        boolean reading = true;
        while (reading) {
            int tag = reader.readTag();
            int index = WireFormat.getTagFieldNumber(tag);
            Field<Class<?>> field = index == this.field.getIndex() ? this.field : ClassField.fromIndex(index);
            if (field != null) {
                result = field.getMarshaller().readFrom(reader);
            } else {
                reading = (tag != 0) && reader.skipField(tag);
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
