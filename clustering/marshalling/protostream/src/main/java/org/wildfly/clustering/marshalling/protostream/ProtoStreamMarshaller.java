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

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.RawProtobufMarshaller;

/**
 * @author Paul Ferraro
 */
public interface ProtoStreamMarshaller<T> extends RawProtobufMarshaller<T>, Marshallable<T> {

    @Override
    default String getTypeName() {
        Class<?> targetClass = this.getJavaClass();
        Package targetPackage = targetClass.getPackage();
        return (targetPackage != null) ? (targetPackage.getName() + '.' + targetClass.getSimpleName()) : targetClass.getSimpleName();
    }

    @Override
    default T readFrom(ProtoStreamReader reader) throws IOException {
        // Temporary default implementation
        return this.readFrom(reader.getSerializationContext(), reader);
    }

    @Override
    default void writeTo(ProtoStreamWriter writer, T value) throws IOException {
        // Temporary default implementation
        this.writeTo(writer.getSerializationContext(), writer, value);
    }

    @Override
    default T readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        return this.readFrom(new SerializationContextProtoStreamReader(context, reader));
    }

    @Override
    default void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, T value) throws IOException {
        this.writeTo(new SerializationContextProtoStreamWriter(context, writer), value);
    }
}
