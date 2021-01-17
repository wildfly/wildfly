/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.RawProtobufMarshaller;
import org.wildfly.clustering.marshalling.spi.ByteBufferInputStream;
import org.wildfly.clustering.marshalling.spi.ByteBufferOutputStream;

/**
 * @author Paul Ferraro
 */
public interface ProtoStreamMarshaller<T> extends RawProtobufMarshaller<T>, Predictable<T> {

    @Override
    default String getTypeName() {
        Class<?> targetClass = this.getJavaClass();
        Package targetPackage = targetClass.getPackage();
        return (targetPackage != null) ? (targetPackage.getName() + '.' + targetClass.getSimpleName()) : targetClass.getSimpleName();
    }

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
     * Reads an object from a specified byte buffer.
     * @param <T> the object type
     * @param context a serialization context
     * @param buffer a byte buffer
     * @param targetClass the type of the object to read
     * @return the unmarshalled object
     * @throws IOException if the object could not be unmarshalled
     */
    static <T> T read(ImmutableSerializationContext context, ByteBuffer buffer, Class<T> targetClass) throws IOException {
        try (InputStream input = new ByteBufferInputStream(buffer)) {
            return ProtobufUtil.readFrom(context, input, targetClass);
        }
    }

    /**
     * Writes an object to a byte buffer.
     * @param <T> the object type
     * @param context a serialization context
     * @param value the object to write
     * @return a byte buffer
     * @throws IOException if the object could not be marshalled
     */
    static ByteBuffer write(ImmutableSerializationContext context, Object value) throws IOException {
        BaseMarshaller<?> marshaller = context.getMarshaller(value.getClass());
        @SuppressWarnings("unchecked")
        OptionalInt size = (marshaller instanceof Predictable) ? ((Predictable<Object>) marshaller).size(context, value) : OptionalInt.empty();
        try (ByteBufferOutputStream output = new ByteBufferOutputStream(size)) {
            ProtobufUtil.writeTo(context, output, value);
            return output.getBuffer();
        }
    }
}
