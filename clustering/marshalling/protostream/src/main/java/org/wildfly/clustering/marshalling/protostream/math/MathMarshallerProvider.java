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

package org.wildfly.clustering.marshalling.protostream.math;

import java.math.RoundingMode;

import org.wildfly.clustering.marshalling.protostream.EnumMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.SimpleFieldSetMarshaller;

/**
 * Provider for java.math marshallers.
 * @author Paul Ferraro
 */
public enum MathMarshallerProvider implements ProtoStreamMarshallerProvider {

    BIG_DECIMAL(new BigDecimalMarshaller()),
    BIG_INTEGER(new SimpleFieldSetMarshaller<>(BigIntegerMarshaller.INSTANCE)),
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
