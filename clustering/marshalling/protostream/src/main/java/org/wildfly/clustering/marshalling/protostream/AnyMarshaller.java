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
import java.io.StreamCorruptedException;
import java.lang.reflect.Proxy;
import java.util.OptionalInt;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.impl.WireFormat;
import org.wildfly.security.manager.WildFlySecurityManager;

import protostream.com.google.protobuf.CodedOutputStream;

/**
 * Marshaller for an {@link Any} object.
 * @author Paul Ferraro
 */
public enum AnyMarshaller implements ProtoStreamMarshaller<Any> {
    INSTANCE;

    @Override
    public Class<? extends Any> getJavaClass() {
        return Any.class;
    }

    @Override
    public Any readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        int tag = reader.readTag();
        if (tag == 0) return new Any(null);

        AnyField field = AnyField.fromIndex(WireFormat.getTagFieldNumber(tag));
        if (field == null) {
            throw new StreamCorruptedException(String.valueOf(tag));
        }

        Object value = field.readFrom(context, reader);

        if (field == AnyField.REFERENCE) {
            value = ProtoStreamReaderContext.INSTANCE.get().findByReference((Integer) value);
        } else {
            ProtoStreamReaderContext.INSTANCE.get().setReference(value);
        }

        if (reader.readTag() != 0) {
            throw new StreamCorruptedException();
        }
        return new Any(value);
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Any value) throws IOException {
        Object object = value.get();
        if (object == null) return;

        // If we already wrote this object to the stream, write the object reference
        Integer referenceId = ProtoStreamWriterContext.INSTANCE.get().getReferenceId(object);

        AnyField field = (referenceId == null) ? getField(context, object) : AnyField.REFERENCE;

        writer.writeTag(field.getIndex(), field.getWireType());
        field.writeTo(context, writer, (referenceId == null) ? object : referenceId);

        if (referenceId == null) {
            ProtoStreamWriterContext.INSTANCE.get().setReference(object);
        }
    }

    @Override
    public OptionalInt size(ImmutableSerializationContext context, Any value) {
        Object object = value.get();
        if (object == null) return OptionalInt.of(0);

        Integer referenceId = ProtoStreamWriterContext.INSTANCE.get().getReferenceId(object);
        AnyField field = (referenceId == null) ? getField(context, object) : AnyField.REFERENCE;
        OptionalInt size = field.size(context, (referenceId == null) ? object : referenceId);

        return size.isPresent() ? OptionalInt.of(size.getAsInt() + CodedOutputStream.computeTagSize(field.getIndex())) : OptionalInt.empty();
    }

    private static AnyField getField(ImmutableSerializationContext context, Object value) {
        Class<?> valueClass = value.getClass();
        AnyField field = AnyField.fromJavaType(valueClass);
        if (field != null) return field;

        if (value instanceof Enum) {
            Enum<?> enumValue = (Enum<?>) value;
            BaseMarshaller<?> marshaller = context.getMarshaller(enumValue.getDeclaringClass());
            return hasTypeId(context, marshaller) ? AnyField.IDENTIFIED_ENUM : AnyField.ENUM;
        }

        if (valueClass.isArray()) {
            Class<?> componentType = valueClass.getComponentType();
            AnyField componentTypeField = AnyField.fromJavaType(componentType);
            if (componentTypeField != null) return AnyField.FIELD_ARRAY;
            if (componentType.isArray()) return AnyField.MULTI_DIMENSIONAL_ARRAY;
            try {
                BaseMarshaller<?> marshaller = context.getMarshaller(componentType);
                return hasTypeId(context, marshaller) ? AnyField.IDENTIFIED_ARRAY : AnyField.ARRAY;
            } catch (IllegalArgumentException e) {
                return AnyField.ARRAY;
            }
        }

        // Only handle JDK exceptions
        if ((value instanceof Throwable) && (WildFlySecurityManager.getClassLoaderPrivileged(value.getClass()) == WildFlySecurityManager.getClassLoaderPrivileged(Throwable.class))) {
            return AnyField.THROWABLE;
        }

        if (Proxy.isProxyClass(valueClass)) {
            return AnyField.PROXY;
        }

        BaseMarshaller<?> marshaller = context.getMarshaller(valueClass);
        return hasTypeId(context, marshaller) ? AnyField.IDENTIFIED_OBJECT : AnyField.OBJECT;
    }

    private static boolean hasTypeId(ImmutableSerializationContext context, BaseMarshaller<?> marshaller) {
        return context.getDescriptorByName(marshaller.getTypeName()).getTypeId() != null;
    }
}
