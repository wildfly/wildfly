/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.util;

import java.io.IOException;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.Supplier;

import org.wildfly.clustering.marshalling.protostream.FunctionalScalarMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.Scalar;
import org.wildfly.clustering.marshalling.protostream.ScalarMarshaller;
import org.wildfly.common.function.ExceptionFunction;
import org.wildfly.common.function.ExceptionPredicate;

/**
 * Marshallers for java.util.Optional* instances.
 * @author Paul Ferraro
 */
public enum OptionalMarshaller implements ProtoStreamMarshallerProvider {

    OBJECT(Scalar.ANY, Optional::empty, Optional::isPresent, Optional::get, Optional::of),
    DOUBLE(Scalar.DOUBLE.cast(Double.class), OptionalDouble::empty, OptionalDouble::isPresent, OptionalDouble::getAsDouble, OptionalDouble::of),
    INT(Scalar.INTEGER.cast(Integer.class), OptionalInt::empty, OptionalInt::isPresent, OptionalInt::getAsInt, OptionalInt::of),
    LONG(Scalar.LONG.cast(Long.class), OptionalLong::empty, OptionalLong::isPresent, OptionalLong::getAsLong, OptionalLong::of),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    <T, V> OptionalMarshaller(ScalarMarshaller<V> marshaller, Supplier<T> defaultFactory, ExceptionPredicate<T, IOException> present, ExceptionFunction<T, V, IOException> function, ExceptionFunction<V, T, IOException> factory) {
        this.marshaller = new FunctionalScalarMarshaller<>(marshaller, defaultFactory, present.not(), function, factory);
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
