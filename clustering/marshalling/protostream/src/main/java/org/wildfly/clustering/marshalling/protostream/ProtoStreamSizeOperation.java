/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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
