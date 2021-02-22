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
import java.util.function.Supplier;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.impl.WireFormat;
import org.wildfly.common.function.ExceptionFunction;
import org.wildfly.common.function.ExceptionPredicate;
import org.wildfly.common.function.Functions;

import protostream.com.google.protobuf.CodedOutputStream;

/**
 * Marshaller that reads/writes a single field by applying functions to a {@link ScalarMarshaller}.
 * @author Paul Ferraro
 * @param <T> the type of this marshaller
 * @param <V> the type of the mapped scalar marshaller
 */
public class FunctionalScalarMarshaller<T, V> implements ProtoStreamMarshaller<T> {

    private static final int VALUE_INDEX = 1;

    private final Class<? extends T> targetClass;
    private final Supplier<T> defaultFactory;
    private final ExceptionPredicate<T, IOException> skipWrite;
    private final ScalarMarshaller<V> marshaller;
    private final ExceptionFunction<T, V, IOException> function;
    private final ExceptionFunction<V, T, IOException> factory;

    /**
     * Constructs a new single field marshaller based on single scalar marshaller.
     * @param marshaller the scalar marshaller used by this marshaller
     * @param defaultFactory generates a default value returned by {@link #readFrom(ProtoStreamReader)} if no field was written.
     * @param function a function that returns a value suitable for use by the specified scalar marshaller
     * @param factory a function applied to the value read from the specified scalar marshaller
     */
    @SuppressWarnings("unchecked")
    public FunctionalScalarMarshaller(ScalarMarshaller<V> marshaller, Supplier<T> defaultFactory, ExceptionFunction<T, V, IOException> function, ExceptionFunction<V, T, IOException> factory) {
        this((Class<T>) defaultFactory.get().getClass(), marshaller, defaultFactory, function, factory);
    }

    /**
     * Constructs a new single field marshaller based on single scalar marshaller.
     * @param marshaller the scalar marshaller used by this marshaller
     * @param defaultFactory generates a default value returned by {@link #readFrom(ProtoStreamReader)} if no field was written.
     * @param equals a predicate used to determine if {@link #writeTo(ProtoStreamWriter, Object)} should skip writing the field.
     * @param function a function that returns a value suitable for use by the specified scalar marshaller
     * @param factory a function applied to the value read from the specified scalar marshaller
     */
    @SuppressWarnings("unchecked")
    public FunctionalScalarMarshaller(ScalarMarshaller<V> marshaller, Supplier<T> defaultFactory, BiPredicate<V, V> equals, ExceptionFunction<T, V, IOException> function, ExceptionFunction<V, T, IOException> factory) {
        this((Class<T>) defaultFactory.get().getClass(), marshaller, defaultFactory, equals, function, factory);
    }

    /**
     * Constructs a new single field marshaller based on single scalar marshaller.
     * @param marshaller the scalar marshaller used by this marshaller
     * @param defaultFactory generates a default value returned by {@link #readFrom(ProtoStreamReader)} if no field was written.
     * @param skipWrite a predicate used to determine if {@link #writeTo(ProtoStreamWriter, Object)} should skip writing the field.
     * @param function a function that returns a value suitable for use by the specified scalar marshaller
     * @param factory a function applied to the value read from the specified scalar marshaller
     */
    @SuppressWarnings("unchecked")
    public FunctionalScalarMarshaller(ScalarMarshaller<V> marshaller, Supplier<T> defaultFactory, ExceptionPredicate<T, IOException> skipWrite, ExceptionFunction<T, V, IOException> function, ExceptionFunction<V, T, IOException> factory) {
        this((Class<T>) defaultFactory.get().getClass(), marshaller, defaultFactory, skipWrite, function, factory);
    }

    /**
     * Constructs a new single field marshaller based on single scalar marshaller.
     * @param targetClass the type of this marshaller
     * @param marshaller the scalar marshaller used by this marshaller
     * @param function a function that returns a value suitable for use by the specified scalar marshaller
     * @param factory a function applied to the value read from the specified scalar marshaller
     */
    public FunctionalScalarMarshaller(Class<? extends T> targetClass, ScalarMarshaller<V> marshaller, ExceptionFunction<T, V, IOException> function, ExceptionFunction<V, T, IOException> factory) {
        this(targetClass, marshaller, Functions.constantSupplier(null), Objects::isNull, function, factory);
    }

    /**
     * Constructs a new single field marshaller based on single scalar marshaller.
     * @param targetClass the type of this marshaller
     * @param marshaller the scalar marshaller used by this marshaller
     * @param defaultFactory generates a default value returned by {@link #readFrom(ProtoStreamReader)} if no field was written.
     * @param function a function that returns a value suitable for use by the specified scalar marshaller
     * @param factory a function applied to the value read from the specified scalar marshaller
     */
    public FunctionalScalarMarshaller(Class<? extends T> targetClass, ScalarMarshaller<V> marshaller, Supplier<T> defaultFactory, ExceptionFunction<T, V, IOException> function, ExceptionFunction<V, T, IOException> factory) {
        this(targetClass, marshaller, defaultFactory, Objects::equals, function, factory);
    }

    /**
     * Constructs a new single field marshaller based on single scalar marshaller.
     * @param targetClass the type of this marshaller
     * @param marshaller the scalar marshaller used by this marshaller
     * @param defaultFactory generates a default value returned by {@link #readFrom(ProtoStreamReader)} if no field was written.
     * @param equals a predicate comparing the default value with the value to write, used to determine if {@link #writeTo(ProtoStreamWriter, Object)} should skip writing the field.
     * @param function a function that returns a value suitable for use by the specified scalar marshaller
     * @param factory a function applied to the value read from the specified scalar marshaller
     */
    public FunctionalScalarMarshaller(Class<? extends T> targetClass, ScalarMarshaller<V> marshaller, Supplier<T> defaultFactory, BiPredicate<V, V> equals, ExceptionFunction<T, V, IOException> function, ExceptionFunction<V, T, IOException> factory) {
        this(targetClass, marshaller, defaultFactory, new ExceptionPredicate<T, IOException>() {
            @Override
            public boolean test(T value) throws IOException {
                return equals.test(function.apply(value), function.apply(defaultFactory.get()));
            }
        }, function, factory);
    }

    /**
     * Constructs a new single field marshaller based on single scalar marshaller.
     * @param targetClass the type of this marshaller
     * @param marshaller the scalar marshaller used by this marshaller
     * @param defaultFactory generates a default value returned by {@link #readFrom(ProtoStreamReader)} if no field was written.
     * @param skipWrite a predicate used to determine if {@link #writeTo(ProtoStreamWriter, Object)} should skip writing the field.
     * @param function a function that returns a value suitable for use by the specified scalar marshaller
     * @param factory a function applied to the value read from the specified scalar marshaller
     */
    public FunctionalScalarMarshaller(Class<? extends T> targetClass, ScalarMarshaller<V> marshaller, Supplier<T> defaultFactory, ExceptionPredicate<T, IOException> skipWrite, ExceptionFunction<T, V, IOException> function, ExceptionFunction<V, T, IOException> factory) {
        this.targetClass = targetClass;
        this.defaultFactory = defaultFactory;
        this.skipWrite = skipWrite;
        this.marshaller = marshaller;
        this.function = function;
        this.factory = factory;
    }

    @Override
    public T readFrom(ProtoStreamReader reader) throws IOException {
        T value = this.defaultFactory.get();
        boolean reading = true;
        while (reading) {
            int tag = reader.readTag();
            switch (WireFormat.getTagFieldNumber(tag)) {
                case VALUE_INDEX:
                    value = this.factory.apply(this.marshaller.readFrom(reader));
                    break;
                default:
                    reading = (tag != 0) && reader.skipField(tag);
            }
        }
        return value;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, T value) throws IOException {
        if (!this.skipWrite.test(value)) {
            writer.writeTag(VALUE_INDEX, this.marshaller.getWireType());
            this.marshaller.writeTo(writer, this.function.apply(value));
        }
    }

    @Override
    public OptionalInt size(ImmutableSerializationContext context, T value) {
        try {
            int size = 0;
            if (!this.skipWrite.test(value)) {
                OptionalInt valueSize = this.marshaller.size(context, this.function.apply(value));
                if (valueSize.isPresent()) {
                    size += CodedOutputStream.computeTagSize(VALUE_INDEX) + valueSize.getAsInt();
                } else {
                    return valueSize;
                }
            }
            return OptionalInt.of(size);
        } catch (IOException e) {
            return OptionalInt.empty();
        }
    }

    @Override
    public Class<? extends T> getJavaClass() {
        return this.targetClass;
    }
}
