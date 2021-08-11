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

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.TagWriter;
import org.infinispan.protostream.descriptors.WireType;

/**
 * A {@link TagWriter} with the additional ability to write an arbitrary embedded object.
 * @author Paul Ferraro
 */
public interface ProtoStreamWriter extends ProtoStreamOperation, TagWriter {

    default Context getContext() {
        return ProtoStreamWriterContext.FACTORY.get().apply(this);
    }

    /**
     * Writes the specified object of an abitrary type using the specified index.
     * Object will be read via {@link ProtoStreamReader#readAny()}.
     * @param index a field index
     * @param value a value to be written
     * @throws IOException if no marshaller is associated with the type of the specified object, or if the marshaller fails to write the specified object
     */
    default void writeAny(int index, Object value) throws IOException {
        this.writeObject(index, new Any(value));
    }

    /**
     * Writes the specified object of a specific type using the specified index.
     * Object will be read via {@link ProtoStreamReader#readObject(Class)}.
     * @param index a field index
     * @param value a value to be written
     * @throws IOException if no marshaller is associated with the type of the specified object, or if the marshaller fails to write the specified object
     */
    default void writeObject(int index, Object value) throws IOException {
        this.writeTag(index, WireType.LENGTH_DELIMITED);
        this.writeObjectNoTag(value);
    }

    /**
     * Writes the specified object.  Must be preceded by {{@link #writeTag(int, org.infinispan.protostream.descriptors.WireType)}.
     * @param value a value to be written
     * @throws IOException if no marshaller is associated with the type of the specified object, or if the marshaller fails to write the specified object
     */
    void writeObjectNoTag(Object value) throws IOException;

    /**
     * Writes the specified enum field using the specified index.
     * Object will be read via {@link ProtoStreamReader#readEnum(Class)}.
     * @param index a field index
     * @param value an enum to be written
     * @throws IOException if no marshaller is associated with the type of the specified object, or if the marshaller fails to write the specified object
     */
    default <E extends Enum<E>> void writeEnum(int index, E value) throws IOException {
        EnumMarshaller<E> marshaller = (EnumMarshaller<E>) this.getSerializationContext().getMarshaller(value.getDeclaringClass());
        this.writeEnum(index, marshaller.encode(value));
    }

    /**
     * Returns a marshaller suitable of marshalling an object of the specified type.
     * @param <T> the type of the associated marshaller
     * @param <V> the type of the object to be marshalled
     * @param javaClass the type of the value to be written.
     * @return a marshaller suitable for the specified type
     * @throws IllegalArgumentException if no suitable marshaller exists
     */
    @SuppressWarnings("unchecked")
    default <T, V extends T> ProtoStreamMarshaller<T> findMarshaller(Class<V> javaClass) {
        ImmutableSerializationContext context = this.getSerializationContext();
        Class<?> targetClass = javaClass;
        IllegalArgumentException exception = null;
        while (targetClass != null) {
            try {
                return (ProtoStreamMarshaller<T>) context.getMarshaller((Class<T>) targetClass);
            } catch (IllegalArgumentException e) {
                // If no marshaller was found, check super class
                if (exception == null) {
                    exception = e;
                }
                targetClass = targetClass.getSuperclass();
            }
        }
        throw exception;
    }

    /**
     * Deprecated to discourage use.
     * @deprecated Use {@link #writeTag(int, WireType)} instead.
     */
    @Deprecated
    @Override
    void writeTag(int index, int wireType) throws IOException;

    /**
     * Deprecated to discourage use.
     * @deprecated Use {@link #writeUInt32(int, int)} or {@link #writeSInt32(int, int)}
     */
    @Deprecated
    @Override
    void writeInt32(int index, int value) throws IOException;

    /**
     * Deprecated to discourage use.
     * @deprecated Use {@link #writeUInt64(int, int)} or {@link #writeSInt64(int, int)}
     */
    @Deprecated
    @Override
    void writeInt64(int index, long value) throws IOException;

    /**
     * Deprecated to discourage use.
     * @deprecated Use {@link #writeSFixed32(int, int)} instead.
     */
    @Deprecated
    @Override
    void writeFixed32(int index, int value) throws IOException;

    /**
     * Deprecated to discourage use.
     * @deprecated Use {@link #writeSFixed64(int, int)} instead.
     */
    @Deprecated
    @Override
    void writeFixed64(int index, long value) throws IOException;
}
