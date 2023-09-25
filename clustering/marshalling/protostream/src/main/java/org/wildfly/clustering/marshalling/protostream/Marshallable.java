/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;
import java.util.OptionalInt;

/**
 * Interface inherited by marshallable components.
 * @author Paul Ferraro
 * @param <T> the type of this marshaller
 */
public interface Marshallable<T> {

    /**
     * Reads an object from the specified reader.
     * @param reader a ProtoStream reader
     * @return the read object
     * @throws IOException if the object could not be read
     */
    T readFrom(ProtoStreamReader reader) throws IOException;

    /**
     * Writes the specified object to the specified writer.
     * @param writer a ProtoStream writer
     * @param value the object to be written
     * @throws IOException if the object could not be written
     */
    void writeTo(ProtoStreamWriter writer, T value) throws IOException;

    /**
     * Computes the size of the specified object.
     * @param context the marshalling operation
     * @param value the value whose size is to be calculated
     * @return an optional buffer size, only present if the buffer size could be computed
     */
    default OptionalInt size(ProtoStreamSizeOperation operation, T value) {
        return operation.computeSize(this::writeTo, value);
    }

    /**
     * Returns the type of object handled by this marshallable instance.
     * @return the type of object handled by this marshallable instance.
     */
    Class<? extends T> getJavaClass();
}
