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

package org.wildfly.clustering.marshalling.protostream.util;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.wildfly.clustering.marshalling.protostream.ObjectMarshaller;
import org.wildfly.clustering.marshalling.protostream.PrimitiveMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.ScalarMarshaller;
import org.wildfly.clustering.marshalling.protostream.SingleFieldMarshaller;

/**
 * Marshallers for java.util.Optional* instances.
 * @author Paul Ferraro
 */
public enum OptionalMarshaller implements ProtoStreamMarshallerProvider {

    OBJECT(ObjectMarshaller.INSTANCE, Optional::empty, Optional::isPresent, Optional::get, Optional::of),
    DOUBLE(PrimitiveMarshaller.DOUBLE.cast(Double.class), OptionalDouble::empty, OptionalDouble::isPresent, OptionalDouble::getAsDouble, OptionalDouble::of),
    INT(PrimitiveMarshaller.INTEGER.cast(Integer.class), OptionalInt::empty, OptionalInt::isPresent, OptionalInt::getAsInt, OptionalInt::of),
    LONG(PrimitiveMarshaller.LONG.cast(Long.class), OptionalLong::empty, OptionalLong::isPresent, OptionalLong::getAsLong, OptionalLong::of),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    <T, V> OptionalMarshaller(ScalarMarshaller<V> marshaller, Supplier<T> defaultFactory, Predicate<T> isPresent, Function<T, V> function, Function<V, T> factory) {
        this.marshaller = new SingleFieldMarshaller<>(marshaller, defaultFactory, isPresent.negate(), function, factory);
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
