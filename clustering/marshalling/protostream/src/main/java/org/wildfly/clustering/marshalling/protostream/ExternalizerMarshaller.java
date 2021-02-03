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
import java.io.InvalidClassException;
import java.io.StreamCorruptedException;
import java.util.OptionalInt;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.wildfly.clustering.marshalling.Externalizer;

/**
 * ProtoStream marshaller that delegates to an externalizer.
 * @author Paul Ferraro
 * @param <T> the target type of the associated externalizer
 */
@Deprecated
public class ExternalizerMarshaller<T> implements ProtoStreamMarshaller<T> {
    private final Externalizer<T> externalizer;

    public ExternalizerMarshaller(Externalizer<T> externalizer) {
        this.externalizer = externalizer;
    }

    @Override
    public Class<? extends T> getJavaClass() {
        return this.externalizer.getTargetClass();
    }

    @Override
    public T readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        try {
            T result = this.externalizer.readObject(new ProtoStreamObjectInput(context, reader));
            if (reader.readTag() != 0) {
                throw new StreamCorruptedException();
            }
            return result;
        } catch (ClassNotFoundException e) {
            InvalidClassException exception = new InvalidClassException(e.getMessage());
            exception.initCause(e);
            throw exception;
        }
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, T object) throws IOException {
        this.externalizer.writeObject(new ProtoStreamObjectOutput(context, writer), object);
    }

    @Override
    public T readFrom(ProtoStreamReader reader) throws IOException {
        throw new IllegalStateException();
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, T value) throws IOException {
        throw new IllegalStateException();
    }

    @Override
    public OptionalInt size(ImmutableSerializationContext context, T value) {
        return this.externalizer.size(value);
    }
}
