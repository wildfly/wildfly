/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.util.concurrent.atomic;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.wildfly.clustering.marshalling.protostream.FunctionalScalarMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.Scalar;
import org.wildfly.clustering.marshalling.protostream.ScalarMarshaller;
import org.wildfly.common.function.ExceptionFunction;

/**
 * ProtoStream optimized marshallers for java.util.concurrent.atomic types.
 * @author Paul Ferraro
 */
public enum AtomicMarshallerProvider implements ProtoStreamMarshallerProvider {

    BOOLEAN(Scalar.BOOLEAN.cast(Boolean.class), AtomicBoolean::new, AtomicBoolean::get, AtomicBoolean::new),
    INTEGER(Scalar.INTEGER.cast(Integer.class), AtomicInteger::new, AtomicInteger::get, AtomicInteger::new),
    LONG(Scalar.LONG.cast(Long.class), AtomicLong::new, AtomicLong::get, AtomicLong::new),
    REFERENCE(Scalar.ANY, AtomicReference::new, AtomicReference::get, AtomicReference::new),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    <T, V> AtomicMarshallerProvider(ScalarMarshaller<V> marshaller, Supplier<T> defaultFactory, ExceptionFunction<T, V, IOException> function, ExceptionFunction<V, T, IOException> factory) {
        this.marshaller = new FunctionalScalarMarshaller<>(marshaller, defaultFactory, function, factory);
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
