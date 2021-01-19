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
import java.util.function.Function;
import java.util.function.Supplier;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.impl.WireFormat;

/**
 * Marshaller for an object whose fields are marshalled via a {@link FieldMarshaller}.
 * @param <T> the type of this marshaller
 * @param <B> the builder type for reading embedded fields
 */
public class FieldSetMarshaller<T, B> implements ProtoStreamMarshaller<T> {
    private static final int START_INDEX = 1;

    private final Class<? extends T> targetClass;
    private final FieldMarshaller<T, B> marshaller;
    private final Supplier<B> builderFactory;
    private final Function<B, T> factory;

    public FieldSetMarshaller(Class<? extends T> targetClass, FieldMarshaller<T, B> marshaller, Supplier<B> builderFactory, Function<B, T> factory) {
        this.targetClass = targetClass;
        this.marshaller = marshaller;
        this.builderFactory = builderFactory;
        this.factory = factory;
    }

    @Override
    public T readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        B builder = this.builderFactory.get();
        boolean reading = true;
        while (reading) {
            int tag = reader.readTag();
            int index = WireFormat.getTagFieldNumber(tag);
            if ((index >= START_INDEX) && (index < START_INDEX + this.marshaller.getFields())) {
                builder = this.marshaller.readField(context, reader, index - START_INDEX, builder);
            } else {
                reading = (tag != 0) && reader.skipField(tag);
            }
        }
        return this.factory.apply(builder);
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, T value) throws IOException {
        this.marshaller.writeFields(context, writer, START_INDEX, value);
    }

    @Override
    public OptionalInt size(ImmutableSerializationContext context, T value) {
        return this.marshaller.size(context, START_INDEX, value);
    }

    @Override
    public Class<? extends T> getJavaClass() {
        return this.targetClass;
    }
}
