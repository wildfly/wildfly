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

package org.wildfly.clustering.marshalling.protostream.util;

import java.io.IOException;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.function.Predicate;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.impl.WireFormat;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;

import protostream.com.google.protobuf.CodedOutputStream;

/**
 * ProtoStream optimized marshallers for optional types.
 * @author Paul Ferraro
 */
public class OptionalMarshaller<T, V> implements ProtoStreamMarshaller<T> {

    private static final int VALUE = 1;

    private final Class<T> targetClass;
    private final Predicate<T> present;
    private final T empty;
    private final int wireType;
    private final ProtoStreamMarshaller<V> marshaller;
    private final Function<T, V> function;
    private final Function<V, T> factory;

    public OptionalMarshaller(Class<T> targetClass, Predicate<T> present, T empty, int wireType, ProtoStreamMarshaller<V> marshaller, Function<T, V> function, Function<V, T> factory) {
        this.targetClass = targetClass;
        this.present = present;
        this.empty = empty;
        this.wireType = wireType;
        this.marshaller = marshaller;
        this.function = function;
        this.factory = factory;
    }

    @Override
    public T readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        T result = this.empty;
        boolean reading = true;
        while (reading) {
            int tag = reader.readTag();
            switch (WireFormat.getTagFieldNumber(tag)) {
                case VALUE:
                    V value = this.marshaller.readFrom(context, reader);
                    result = this.factory.apply(value);
                    break;
                default:
                    reading = (tag != 0) && reader.skipField(tag);
            }
        }
        return result;
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, T optional) throws IOException {
        if (this.present.test(optional)) {
            V value = this.function.apply(optional);
            writer.writeTag(VALUE, this.wireType);
            this.marshaller.writeTo(context, writer, value);
        }
    }

    @Override
    public OptionalInt size(ImmutableSerializationContext context, T object) {
        if (!this.present.test(object)) return OptionalInt.of(0);
        V value = this.function.apply(object);
        OptionalInt size = this.marshaller.size(context, value);
        return size.isPresent() ? OptionalInt.of(size.getAsInt() + CodedOutputStream.computeTagSize(VALUE)) : OptionalInt.empty();
    }

    @Override
    public Class<? extends T> getJavaClass() {
        return this.targetClass;
    }
}
