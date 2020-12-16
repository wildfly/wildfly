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

import org.wildfly.clustering.marshalling.protostream.FunctionalMarshaller;
import org.wildfly.clustering.marshalling.protostream.ObjectMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.PrimitiveMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;

/**
 * ProtoStream optimized marshallers for java.util.concurrent.atomic types.
 * @author Paul Ferraro
 */
public enum AtomicMarshallerProvider implements ProtoStreamMarshallerProvider {
    BOOLEAN(new FunctionalMarshaller<>(AtomicBoolean.class, PrimitiveMarshaller.BOOLEAN.cast(Boolean.class), AtomicBoolean::get, AtomicBoolean::new)),
    INTEGER(new FunctionalMarshaller<>(AtomicInteger.class, PrimitiveMarshaller.INTEGER.cast(Integer.class), AtomicInteger::get, AtomicInteger::new)),
    LONG(new FunctionalMarshaller<>(AtomicLong.class, PrimitiveMarshaller.LONG.cast(Long.class), AtomicLong::get, AtomicLong::new)),
    REFERENCE(new FunctionalMarshaller<>(AtomicReference.class, ObjectMarshaller.INSTANCE, AtomicReference::get, AtomicReference::new)),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    AtomicMarshallerProvider(ProtoStreamMarshaller<?> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
