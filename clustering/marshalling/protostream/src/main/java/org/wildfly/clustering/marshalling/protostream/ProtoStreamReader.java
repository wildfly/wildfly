/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;

import org.infinispan.protostream.TagReader;
import org.infinispan.protostream.descriptors.WireType;

/**
 * A {@link TagReader} with the additional ability to read an arbitrary embedded object.
 * @author Paul Ferraro
 */
public interface ProtoStreamReader extends ProtoStreamOperation, TagReader {

    /**
     * Returns the tag of the current field, or optional if {@link #readTag()} was not yet called for the next field.
     * @return
     */
    int getCurrentTag();

    /**
     * Skips over the field of the specified wire type.
     * @return true, if the current tag is a normal field, false otherwise
     * @throws IOException if the stream does not conform to the wire type of the skipped field.
     */
    default boolean skipField(WireType type) throws IOException {
        return this.skipField(WireType.makeTag(0, type));
    }

    /**
     * Returns a reader for a field set whose fields start at the specified index.
     * @param <T> the field builder type
     * @param reader a field reader
     * @param startIndex the start index for the field set
     * @return a field set reader
     */
    <T> FieldSetReader<T> createFieldSetReader(FieldReadable<T> reader, int startIndex);

    /**
     * Reads an object of an arbitrary type from this reader.
     * @return a supplier of the unmarshalled object
     * @throws IOException if the object could not be read with the associated marshaller.
     */
    Object readAny() throws IOException;

    /**
     * Reads an object of an arbitrary type from this reader, cast to the specified type.
     * @param the expected type
     * @return a supplier of the unmarshalled object
     * @throws IOException if the object could not be read with the associated marshaller.
     */
    default <T> T readAny(Class<T> targetClass) throws IOException {
        return targetClass.cast(this.readAny());
    }

    /**
     * Reads an object of the specified type from this reader.
     * @param <T> the type of the associated marshaller
     * @param targetClass the class of the associated marshaller
     * @return the unmarshalled object
     * @throws IOException if no marshaller is associated with the specified class, or if the object could not be read with the associated marshaller.
     */
    <T> T readObject(Class<T> targetClass) throws IOException;

    /**
     * Reads an num of the specified type from this reader.
     * @param <T> the type of the associated marshaller
     * @param targetClass the class of the associated marshaller
     * @return the unmarshalled enum
     * @throws IOException if no marshaller is associated with the specified enum class, or if the enum could not be read with the associated marshaller.
     */
    default <E extends Enum<E>> E readEnum(Class<E> enumClass) throws IOException {
        EnumMarshaller<E> marshaller = (EnumMarshaller<E>) this.getSerializationContext().getMarshaller(enumClass);
        int code = this.readEnum();
        return marshaller.decode(code);
    }

    /**
     * Deprecated to discourage use.
     * @deprecated Use {@link #readUInt32()} or {@link #readSInt32()}
     */
    @Deprecated
    @Override
    int readInt32() throws IOException;

    /**
     * Deprecated to discourage use.
     * @deprecated Use {@link #readSFixed32()} instead.
     */
    @Deprecated
    @Override
    int readFixed32() throws IOException;

    /**
     * Deprecated to discourage use.
     * @deprecated Use {@link #readUInt64()} or {@link #readSInt64()}
     */
    @Deprecated
    @Override
    long readInt64() throws IOException;

    /**
     * Deprecated to discourage use.
     * @deprecated Use {@link #readSFixed64()} instead.
     */
    @Deprecated
    @Override
    long readFixed64() throws IOException;
}
