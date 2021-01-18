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
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.impl.WireFormat;
import org.wildfly.common.function.Functions;

import protostream.com.google.protobuf.CodedOutputStream;

/**
 * Marshaller that reads/writes a single field via a {@link ScalarMarshaller}.
 * @author Paul Ferraro
 */
public class SingleFieldMarshaller<T, V> implements ProtoStreamMarshaller<T> {

    private static final int VALUE_INDEX = 1;

    private final Class<? extends T> targetClass;
    private final Supplier<T> defaultFactory;
    private final Predicate<T> equalsDefault;
    private final ScalarMarshaller<V> marshaller;
    private final Function<T, V> function;
    private final Function<V, T> factory;

    @SuppressWarnings("unchecked")
    public SingleFieldMarshaller(ScalarMarshaller<V> marshaller, Supplier<T> defaultFactory, Function<T, V> function, Function<V, T> factory) {
        this((Class<T>) defaultFactory.get().getClass(), marshaller, defaultFactory, function, factory);
    }

    @SuppressWarnings("unchecked")
    public SingleFieldMarshaller(ScalarMarshaller<V> marshaller, Supplier<T> defaultFactory, BiPredicate<V, V> equals, Function<T, V> function, Function<V, T> factory) {
        this((Class<T>) defaultFactory.get().getClass(), marshaller, defaultFactory, equals, function, factory);
    }

    @SuppressWarnings("unchecked")
    public SingleFieldMarshaller(ScalarMarshaller<V> marshaller, Supplier<T> defaultFactory, Predicate<T> equalsDefault, Function<T, V> function, Function<V, T> factory) {
        this((Class<T>) defaultFactory.get().getClass(), marshaller, defaultFactory, equalsDefault, function, factory);
    }

    public SingleFieldMarshaller(Class<? extends T> targetClass, ScalarMarshaller<V> marshaller, Function<T, V> function, Function<V, T> factory) {
        this(targetClass, marshaller, Functions.constantSupplier(null), Objects::isNull, function, factory);
    }

    public SingleFieldMarshaller(Class<? extends T> targetClass, ScalarMarshaller<V> marshaller, Supplier<T> defaultFactory, Function<T, V> function, Function<V, T> factory) {
        this(targetClass, marshaller, defaultFactory, Objects::equals, function, factory);
    }

    public SingleFieldMarshaller(Class<? extends T> targetClass, ScalarMarshaller<V> marshaller, Supplier<T> defaultFactory, BiPredicate<V, V> equals, Function<T, V> function, Function<V, T> factory) {
        this(targetClass, marshaller, defaultFactory, new Predicate<T>() {
            private final V defaultValue = function.apply(defaultFactory.get());

            @Override
            public boolean test(T value) {
                return equals.test(function.apply(value), this.defaultValue);
            }
        }, function, factory);
    }

    public SingleFieldMarshaller(Class<? extends T> targetClass, ScalarMarshaller<V> marshaller, Supplier<T> defaultFactory, Predicate<T> equalsDefault, Function<T, V> function, Function<V, T> factory) {
        this.targetClass = targetClass;
        this.defaultFactory = defaultFactory;
        this.equalsDefault = equalsDefault;
        this.marshaller = marshaller;
        this.function = function;
        this.factory = factory;
    }

    @Override
    public T readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        T value = this.defaultFactory.get();
        boolean reading = true;
        while (reading) {
            int tag = reader.readTag();
            switch (WireFormat.getTagFieldNumber(tag)) {
                case VALUE_INDEX:
                    value = this.factory.apply(this.marshaller.readFrom(context, reader));
                    break;
                default:
                    reading = (tag != 0) && reader.skipField(tag);
            }
        }
        return value;
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, T value) throws IOException {
        if (!this.equalsDefault.test(value)) {
            writer.writeTag(VALUE_INDEX, this.marshaller.getWireType());
            this.marshaller.writeTo(context, writer, this.function.apply(value));
        }
    }

    @Override
    public OptionalInt size(ImmutableSerializationContext context, T value) {
        int size = 0;
        if (!this.equalsDefault.test(value)) {
            OptionalInt valueSize = this.marshaller.size(context, this.function.apply(value));
            if (valueSize.isPresent()) {
                size += valueSize.getAsInt() + CodedOutputStream.computeTagSize(VALUE_INDEX);
            } else {
                return valueSize;
            }
        }
        return OptionalInt.of(size);
    }

    @Override
    public Class<? extends T> getJavaClass() {
        return this.targetClass;
    }
}
