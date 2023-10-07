/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;

import org.infinispan.protostream.descriptors.WireType;

/**
 * Reads a single field of a field set from a reader.
 * @author Paul Ferraro
 */
public interface FieldReadable<T> {
    /**
     * Reads a single field from the specified reader.
     * @param reader a reader
     * @param index the zero-based index, relative to this field set, of the field to be read
     * @param type the wire type of the field to be read
     * @param current the current value
     * @return the read value
     * @throws IOException if a field could not be read
     */
    T readFrom(ProtoStreamReader reader, int index, WireType type, T current) throws IOException;

    /**
     * Returns the index that should follow this field set.
     * @param startIndex the starting index of this field set
     * @return the next index
     */
    default int nextIndex(int startIndex) {
        return startIndex + this.getFields();
    }

    /**
     * Returns the number of fields in this field set
     * @return a number of fields
     */
    int getFields();
}
