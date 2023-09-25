/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;
import java.util.OptionalInt;

import org.infinispan.protostream.descriptors.WireType;
import org.infinispan.protostream.impl.TagWriterImpl;
import org.wildfly.common.function.ExceptionBiConsumer;

/**
 * A ProtoStream size operation.
 * @author Paul Ferraro
 */
public interface ProtoStreamSizeOperation extends ProtoStreamOperation {

    /**
     * Computes the size of the specified object using the specified operation.
     * @param <T> the source object type
     * @param operation a write operation used to compute the size
     * @param value the object to be sized
     * @return the computed size
     */
    <T> OptionalInt computeSize(ExceptionBiConsumer<ProtoStreamWriter, T, IOException> operation, T value);

    /**
     * Computes the marshalled size of the protobuf tag containing the specified field index and wire type.
     * @param index a field index
     * @param type a wire type
     * @return the marshalled size of the protobuf tag
     */
    default int tagSize(int index, WireType type) {
        return this.varIntSize(WireType.makeTag(index, type));
    }

    /**
     * Computes the marshalled size of the specified variable-width integer.
     * @param index a variable-width integer
     * @return the marshalled size of the specified variable-width integer.
     */
    default int varIntSize(int value) {
        TagWriterImpl writer = TagWriterImpl.newInstance(this.getSerializationContext());
        try {
            writer.writeVarint32(value);
            return writer.getWrittenBytes();
        } catch (IOException e) {
            return WireType.MAX_VARINT_SIZE;
        }
    }
}
