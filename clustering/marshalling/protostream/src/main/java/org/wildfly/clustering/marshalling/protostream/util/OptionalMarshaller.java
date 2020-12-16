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

package org.wildfly.clustering.marshalling.protostream.util;

import java.io.IOException;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.impl.WireFormat;
import org.wildfly.clustering.marshalling.protostream.FunctionalMarshaller;
import org.wildfly.clustering.marshalling.protostream.ObjectMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.Predictable;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;

/**
 * ProtoStream optimized marshallers for optional types.
 * @author Paul Ferraro
 */
public enum OptionalMarshaller implements ProtoStreamMarshallerProvider {
    INT(OptionalInt.class) {
        @Override
        public OptionalInt readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            OptionalInt result = OptionalInt.empty();
            int tag = reader.readTag();
            while (tag != 0) {
                int field = WireFormat.getTagFieldNumber(tag);
                switch (field) {
                    case 1: {
                        result = OptionalInt.of(reader.readSInt32());
                        break;
                    }
                    default: {
                        reader.skipField(tag);
                    }
                }
                tag = reader.readTag();
            }
            return result;
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            OptionalInt optional = (OptionalInt) value;
            if (optional.isPresent()) {
                writer.writeSInt64(1, optional.getAsInt());
            }
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            OptionalInt optional = (OptionalInt) value;
            return OptionalInt.of(optional.isPresent() ? Predictable.signedIntSize(optional.getAsInt()) + 1 : 0);
        }
    },
    LONG(OptionalLong.class) {
        @Override
        public OptionalLong readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            OptionalLong result = OptionalLong.empty();
            int tag = reader.readTag();
            while (tag != 0) {
                int field = WireFormat.getTagFieldNumber(tag);
                switch (field) {
                    case 1: {
                        result = OptionalLong.of(reader.readSInt64());
                        break;
                    }
                    default: {
                        reader.skipField(tag);
                    }
                }
                tag = reader.readTag();
            }
            return result;
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            OptionalLong optional = (OptionalLong) value;
            if (optional.isPresent()) {
                writer.writeSInt64(1, optional.getAsLong());
            }
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            OptionalLong optional = (OptionalLong) value;
            return OptionalInt.of(optional.isPresent() ? Predictable.signedLongSize(optional.getAsLong()) + 1 : 0);
        }
    },
    DOUBLE(OptionalDouble.class) {
        @Override
        public OptionalDouble readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            OptionalDouble result = OptionalDouble.empty();
            int tag = reader.readTag();
            while (tag != 0) {
                int field = WireFormat.getTagFieldNumber(tag);
                switch (field) {
                    case 1: {
                        result = OptionalDouble.of(reader.readDouble());
                        break;
                    }
                    default: {
                        reader.skipField(tag);
                    }
                }
                tag = reader.readTag();
            }
            return result;
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            OptionalDouble optional = (OptionalDouble) value;
            if (optional.isPresent()) {
                writer.writeDouble(1, optional.getAsDouble());
            }
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            OptionalDouble optional = (OptionalDouble) value;
            return OptionalInt.of(optional.isPresent() ? Double.BYTES + 1 : 0);
        }
    },
    OBJECT(Optional.class) {
        @SuppressWarnings({ "rawtypes", "unchecked" })
        private final ProtoStreamMarshaller<Optional> marshaller = new FunctionalMarshaller<>(Optional.class, ObjectMarshaller.INSTANCE, value -> value.orElse(null), Optional::ofNullable);

        @Override
        public ProtoStreamMarshaller<?> getMarshaller() {
            return this.marshaller;
        }
    }
    ;
    private final Class<?> targetClass;

    OptionalMarshaller(Class<?> targetClass) {
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
