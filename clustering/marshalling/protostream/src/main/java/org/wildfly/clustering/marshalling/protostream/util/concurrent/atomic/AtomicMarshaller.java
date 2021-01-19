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

import java.io.IOException;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.impl.RawProtoStreamWriterImpl;
import org.wildfly.clustering.marshalling.protostream.ExternalizerMarshaller;
import org.wildfly.clustering.marshalling.protostream.FunctionalObjectMarshaller;
import org.wildfly.clustering.marshalling.protostream.MarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.spi.util.concurrent.atomic.AtomicExternalizerProvider;

import protostream.com.google.protobuf.CodedOutputStream;

/**
 * ProtoStream optimized marshallers for java.util.concurrent.atomic types.
 * @author Paul Ferraro
 */
public enum AtomicMarshaller implements MarshallerProvider {
    BOOLEAN(AtomicBoolean.class) {
        private final ProtoStreamMarshaller<AtomicBoolean> marshaller = new ExternalizerMarshaller<>(AtomicExternalizerProvider.ATOMIC_BOOLEAN.cast(AtomicBoolean.class));

        @Override
        public ProtoStreamMarshaller<?> getMarshaller() {
            return this.marshaller;
        }
    },
    INTEGER(AtomicInteger.class) {
        @Override
        public AtomicInteger readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return new AtomicInteger(reader.readSInt32());
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            ((RawProtoStreamWriterImpl) writer).getDelegate().writeSInt32NoTag(((AtomicInteger) value).intValue());
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            return OptionalInt.of(CodedOutputStream.computeSInt32SizeNoTag(((AtomicInteger) value).intValue()));
        }
    },
    LONG(AtomicLong.class) {
        @Override
        public AtomicLong readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return new AtomicLong(reader.readSInt64());
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            ((RawProtoStreamWriterImpl) writer).getDelegate().writeSInt64NoTag(((AtomicLong) value).longValue());
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            return OptionalInt.of(CodedOutputStream.computeSInt64SizeNoTag(((AtomicLong) value).longValue()));
        }
    },
    REFERENCE(AtomicReference.class) {
        @SuppressWarnings("rawtypes")
        private final ProtoStreamMarshaller<AtomicReference> marshaller = new FunctionalObjectMarshaller<>(AtomicReference.class, AtomicReference::new, AtomicReference::get);

        @Override
        public ProtoStreamMarshaller<?> getMarshaller() {
            return this.marshaller;
        }
    }
    ;
    private final Class<?> targetClass;

    AtomicMarshaller(Class<?> targetClass) {
        this.targetClass = targetClass;
    }

    @Override
    public Class<? extends Object> getJavaClass() {
        return this.targetClass;
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this;
    }
}
