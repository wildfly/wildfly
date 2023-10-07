/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;

import org.infinispan.protostream.TagWriter;
import org.infinispan.protostream.descriptors.WireType;

/**
 * A {@link TagWriter} with the additional ability to write an arbitrary embedded object.
 * @author Paul Ferraro
 */
public interface ProtoStreamWriter extends ProtoStreamOperation, TagWriter {

    <T> FieldSetWriter<T> createFieldSetWriter(Writable<T> writer, int startIndex);

    /**
     * Writes the specified object of an abitrary type using the specified index.
     * Object will be read via {@link ProtoStreamReader#readAny()}.
     * @param index a field index
     * @param value a value to be written
     * @throws IOException if no marshaller is associated with the type of the specified object, or if the marshaller fails to write the specified object
     */
    default void writeAny(int index, Object value) throws IOException {
        this.writeTag(index, WireType.LENGTH_DELIMITED);
        this.writeAnyNoTag(value);
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
     * Writes the specified object of an arbitrary type.  Must be preceded by {{@link #writeTag(int, org.infinispan.protostream.descriptors.WireType)}.
     * @param value a value to be written
     * @throws IOException if no marshaller is associated with the type of the specified object, or if the marshaller fails to write the specified object
     */
    void writeAnyNoTag(Object value) throws IOException;

    /**
     * Writes the specified object of a specific type.  Must be preceded by {{@link #writeTag(int, org.infinispan.protostream.descriptors.WireType)}.
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
     * Deprecated to discourage use.
     * @deprecated Use {@link #writeTag(int, WireType)} instead.
     */
    @Deprecated
    @Override
    default void writeTag(int index, int wireType) throws IOException {
        this.writeTag(index, WireType.fromValue(wireType));
    }

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
