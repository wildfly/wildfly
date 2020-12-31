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
import java.util.OptionalInt;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;

/**
 * Marshaller whose fields are designed to be embedded into another marshaller.
 * @author Paul Ferraro
 */
public interface EmbeddableMarshaller<T> extends ProtoStreamMarshaller<T> {

    /**
     * Reads the specified embedded field from the specified reader.
     * @param reader a ProtoStream reader
     * @param index the field index
     * @param value the existing value
     * @return the value read from the reader
     * @throws IOException if the field could not be read
     */
    T readField(ImmutableSerializationContext context, RawProtoStreamReader reader, int index, T value) throws IOException;

    /**
     * Writes the embedded fields of the specified object to the specified writer beginning at the specified index.
     * @param writer a ProtoStream writer
     * @param startIndex the start index for the embedded fields
     * @param value the value to be written
     * @throws IOException if the value could not be written
     */
    void writeFields(ImmutableSerializationContext context, RawProtoStreamWriter writer, int startIndex, T value) throws IOException;

    /**
     * Returns the size of the embedded fields of the specified object beginning at the specified index.
     * @param writer a ProtoStream writer
     * @param startIndex the start index for the embedded fields
     * @param value the value to be written
     * @throws IOException if the value could not be written
     */
    default OptionalInt size(ImmutableSerializationContext context, int startIndex, T value) {
        SizeComputingWriter writer = new SizeComputingWriter();
        try {
            this.writeFields(context, writer, startIndex, value);
            return OptionalInt.of(writer.getAsInt());
        } catch (IOException e) {
            return OptionalInt.empty();
        }
    }

    @Override
    default void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, T value) throws IOException {
        this.writeFields(context, writer, 1, value);
    }

    @Override
    default OptionalInt size(ImmutableSerializationContext context, T value) {
        return this.size(context, 1, value);
    }
}
