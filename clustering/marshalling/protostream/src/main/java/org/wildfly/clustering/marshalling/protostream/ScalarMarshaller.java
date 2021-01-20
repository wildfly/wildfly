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
import java.nio.ByteBuffer;
import java.util.OptionalInt;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;

/**
 * A marshaller of a scalar value.  A scalar value will not write a tag in its {@link #writeTo(ImmutableSerializationContext, RawProtoStreamWriter, Object)} method.
 * @author Paul Ferraro
 */
public interface ScalarMarshaller<T> extends Predictable<T> {

    /**
     * Reads an object from the specified reader.
     * @param context a serialization context
     * @param reader a ProtoStream reader
     * @return the read object
     * @throws IOException if the object could not be read
     */
    T readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException;

    /**
     * Writes the specified object to the specified writer.
     * @param context a serialization context
     * @param writer a ProtoStream writer
     * @param the object to be written
     * @throws IOException if the object could not be written
     */
    void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, T value) throws IOException;

    /**
     * Predicts the size of the specified object that would be written via the {@link #writeTo(ImmutableSerializationContext, RawProtoStreamWriter, Object)} method.
     * @param context a serialization context
     * @param the object to be written
     * @return the computed size of the specified object, or empty if the size could not be determined.
     */
    @Override
    default OptionalInt size(ImmutableSerializationContext context, T value) {
        SizeComputingWriter writer = new SizeComputingWriter(context);
        try {
            this.writeTo(writer, writer, value);
            return OptionalInt.of(writer.getAsInt());
        } catch (IOException | IllegalArgumentException e) {
            return OptionalInt.empty();
        }
    }

    /**
     * Returns the type of this marshaller.
     * @return the type of this marshaller
     */
    Class<? extends T> getJavaClass();

    /**
     * Reads an object from a specified ProtoStream reader.
     * @param <T> the object type
     * @param context a serialization context
     * @param buffer a byte buffer
     * @param targetClass the type of the object to read
     * @return the unmarshalled object
     * @throws IOException if the object could not be unmarshalled
     */
    static <T> T readObject(ImmutableSerializationContext context, RawProtoStreamReader reader, Class<T> targetClass) throws IOException {
        ByteBuffer buffer = ByteBufferMarshaller.INSTANCE.readFrom(context, reader);
        return ProtoStreamMarshaller.read(context, buffer, targetClass);
    }

    /**
     * Writes an object to the specified ProtoStream writer.
     * @param <T> the object type
     * @param context a serialization context
     * @param writer a ProtoStream writer
     * @param value the object to write
     * @throws IOException if the object could not be marshalled
     */
    static <T> void writeObject(ImmutableSerializationContext context, RawProtoStreamWriter writer, T value) throws IOException {
        ByteBuffer buffer = ProtoStreamMarshaller.write(context, value);
        ByteBufferMarshaller.INSTANCE.writeTo(context, writer, buffer);
    }
}
