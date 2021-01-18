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

package org.wildfly.clustering.marshalling.protostream.util.concurrent.atomic;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import org.wildfly.clustering.marshalling.protostream.MarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.ObjectMarshaller;
import org.wildfly.clustering.marshalling.protostream.PrimitiveMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ScalarMarshaller;
import org.wildfly.clustering.marshalling.protostream.SingleFieldMarshaller;

/**
 * ProtoStream optimized marshallers for java.util.concurrent.atomic types.
 * @author Paul Ferraro
 */
public enum AtomicMarshaller implements MarshallerProvider {
    BOOLEAN(PrimitiveMarshaller.BOOLEAN.cast(Boolean.class), AtomicBoolean::new, AtomicBoolean::get, AtomicBoolean::new),
    INTEGER(PrimitiveMarshaller.INTEGER.cast(Integer.class), AtomicInteger::new, AtomicInteger::get, AtomicInteger::new),
    LONG(PrimitiveMarshaller.LONG.cast(Long.class), AtomicLong::new, AtomicLong::get, AtomicLong::new),
    REFERENCE(ObjectMarshaller.INSTANCE, AtomicReference::new, AtomicReference::get, AtomicReference::new),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    <T, V> AtomicMarshaller(ScalarMarshaller<V> marshaller, Supplier<T> defaultFactory, Function<T, V> function, Function<V, T> factory) {
        this.marshaller = new SingleFieldMarshaller<>(marshaller, defaultFactory, function, factory);
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
