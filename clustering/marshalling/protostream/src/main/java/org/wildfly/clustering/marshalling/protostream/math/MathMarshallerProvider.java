/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.math;

import java.math.BigInteger;
import java.math.RoundingMode;

import org.wildfly.clustering.marshalling.protostream.EnumMarshaller;
import org.wildfly.clustering.marshalling.protostream.FunctionalScalarMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.Scalar;
import org.wildfly.common.function.Functions;

/**
 * Provider for java.math marshallers.
 * @author Paul Ferraro
 */
public enum MathMarshallerProvider implements ProtoStreamMarshallerProvider {

    BIG_DECIMAL(BigDecimalMarshaller.INSTANCE),
    BIG_INTEGER(new FunctionalScalarMarshaller<>(Scalar.BYTE_ARRAY.cast(byte[].class), Functions.constantSupplier(BigInteger.ZERO), BigDecimalMarshaller.INSTANCE, BigInteger::toByteArray, BigInteger::new)),
    MATH_CONTEXT(new MathContextMarshaller()),
    ROUNDING_MODE(new EnumMarshaller<>(RoundingMode.class)),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    MathMarshallerProvider(ProtoStreamMarshaller<?> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
