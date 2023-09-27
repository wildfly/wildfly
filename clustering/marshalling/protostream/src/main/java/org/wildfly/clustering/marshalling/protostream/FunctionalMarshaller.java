/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;
import java.util.function.Function;

import org.wildfly.common.function.ExceptionFunction;

/**
 * Marshaller that uses a functional transformation of another marshaller.
 * @author Paul Ferraro
 * @param <T> the type of this marshaller
 * @param <V> the type of the mapped marshaller
 */
public class FunctionalMarshaller<T, V> implements ProtoStreamMarshaller<T> {

    private final Class<T> targetClass;
    private final Function<ProtoStreamOperation, ProtoStreamMarshaller<V>> marshallerFactory;
    private final ExceptionFunction<T, V, IOException> function;
    private final ExceptionFunction<V, T, IOException> factory;

    public FunctionalMarshaller(Class<T> targetClass, Class<V> sourceClass, ExceptionFunction<T, V, IOException> function, ExceptionFunction<V, T, IOException> factory) {
        this(targetClass, operation -> operation.findMarshaller(sourceClass), function, factory);
    }

    public FunctionalMarshaller(Class<T> targetClass, ProtoStreamMarshaller<V> marshaller, ExceptionFunction<T, V, IOException> function, ExceptionFunction<V, T, IOException> factory) {
        this(targetClass, operation -> marshaller, function, factory);
    }

    public FunctionalMarshaller(Class<T> targetClass, Function<ProtoStreamOperation, ProtoStreamMarshaller<V>> marshallerFactory, ExceptionFunction<T, V, IOException> function, ExceptionFunction<V, T, IOException> factory) {
        this.targetClass = targetClass;
        this.marshallerFactory = marshallerFactory;
        this.function = function;
        this.factory = factory;
    }

    @Override
    public T readFrom(ProtoStreamReader reader) throws IOException {
        ProtoStreamMarshaller<V> marshaller = this.marshallerFactory.apply(reader);
        V value = marshaller.readFrom(reader);
        return this.factory.apply(value);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, T object) throws IOException {
        V value = this.function.apply(object);
        ProtoStreamMarshaller<V> marshaller = this.marshallerFactory.apply(writer);
        marshaller.writeTo(writer, value);
    }

    @Override
    public Class<? extends T> getJavaClass() {
        return this.targetClass;
    }
}
