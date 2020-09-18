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
import java.util.function.Function;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;

/**
 * Generic marshaller for a object wrapper.
 * @author Paul Ferraro
 */
public class FunctionalObjectMarshaller<T> implements ProtoStreamMarshaller<T> {

    private final Class<T> targetClass;
    private final Function<Object, T> reader;
    private final Function<T, Object> writer;

    public FunctionalObjectMarshaller(Class<T> targetClass, Function<Object, T> reader, Function<T, Object> writer) {
        this.targetClass = targetClass;
        this.reader = reader;
        this.writer = writer;
    }

    @Override
    public T readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        return this.reader.apply(ObjectMarshaller.INSTANCE.readFrom(context, reader));
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
        ObjectMarshaller.INSTANCE.writeTo(context, writer, this.writer.apply(this.targetClass.cast(value)));
    }

    @Override
    public OptionalInt size(ImmutableSerializationContext context, Object value) {
        return ObjectMarshaller.INSTANCE.size(context, this.writer.apply(this.targetClass.cast(value)));
    }

    @Override
    public Class<? extends T> getJavaClass() {
        return this.targetClass;
    }
}
