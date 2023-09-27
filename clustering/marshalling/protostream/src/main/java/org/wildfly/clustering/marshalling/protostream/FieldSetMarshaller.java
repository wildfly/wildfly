/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;

/**
 * Marshaller for a set of fields, to be shared between multiple marshallers.
 * @author Paul Ferraro
 * @param <T> the type of this marshaller
 * @param <B> the builder type for reading embedded fields
 */
public interface FieldSetMarshaller<T, B> {

    /**
     * Returns a builder for use with {@link #readField(ProtoStreamReader, int, Object)}.
     * May return a shared instance, if the builder type is immutable, or a new instance, if the builder is mutable.
     * @return a builder.
     */
    B getBuilder();

    /**
     * Returns the number of fields written by this marshaller.
     * @return a positive number
     */
    int getFields();

    /**
     * Reads a single field from the specified reader at the specified index.
     * @param reader a ProtoStream reader
     * @param index the field index
     * @param builder the builder to be populated with the read field
     * @return the builder containing the read field
     * @throws IOException if the field could not be read
     */
    B readField(ProtoStreamReader reader, int index, B builder) throws IOException;

    /**
     * Writes the set of fields from the specified object to the specified writer beginning at the specified index.
     * @param writer a ProtoStream writer
     * @param startIndex the start index for the embedded fields
     * @param value the value to be written
     * @throws IOException if the value could not be written
     */
    void writeFields(ProtoStreamWriter writer, int startIndex, T value) throws IOException;
}
