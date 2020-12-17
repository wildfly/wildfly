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

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.impl.WireFormat;

/**
 * Generic marshaller for instances of {@link Class}.
 * @author Paul Ferraro
 */
public class ClassMarshaller implements ProtoStreamMarshaller<Class<?>> {

    private final Field<Class<?>> field;
    private final Field<Class<?>>[] fields = ClassField.values();

    public ClassMarshaller(ClassResolver resolver) {
        this.field = new ClassResolverField(resolver, this.fields.length);
    }

    @Override
    public Class<?> readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        int tag = reader.readTag();
        if (tag == 0) return null;
        int index = WireFormat.getTagFieldNumber(tag);
        Field<Class<?>> field = index == this.field.getIndex() ? this.field : this.fields[index];
        return field.readFrom(context, reader);
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Class<?> targetClass) throws IOException {
        Field<Class<?>> field = this.getField(context, targetClass);
        writer.writeTag(field.getIndex(), WireFormat.WIRETYPE_VARINT);
        field.writeTo(context, writer, targetClass);
    }

    @Override
    public OptionalInt size(ImmutableSerializationContext context, Class<?> targetClass) {
        Field<Class<?>> field = this.getField(context, targetClass);
        OptionalInt size = field.size(context, targetClass);
        return size.isPresent() ? OptionalInt.of(size.getAsInt() + Predictable.unsignedIntSize(field.getIndex() << 3 | WireFormat.WIRETYPE_VARINT)) : OptionalInt.empty();
    }

    Field<Class<?>> getField(ImmutableSerializationContext context, Class<?> targetClass) {
        if (targetClass == Object.class) return ClassField.OBJECT;
        AnyField classField = AnyField.fromJavaType(targetClass);
        if (classField != null) return ClassField.FIELD;
        if (targetClass.isArray()) return ClassField.ARRAY;
        try {
            BaseMarshaller<?> marshaller = context.getMarshaller(targetClass);
            return context.getDescriptorByName(marshaller.getTypeName()).getTypeId() != null ? ClassField.ID : ClassField.NAME;
        } catch (IllegalArgumentException e) {
            // If class does not represent a registered type, then use resolver-based marshalling.
            return this.field;
        }
    }

    @Override
    public Class<? extends Class<?>> getJavaClass() {
        return this.field.getJavaClass();
    }
}
