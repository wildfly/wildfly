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

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;
import java.io.StreamCorruptedException;
import java.util.EnumSet;
import java.util.OptionalInt;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.impl.WireFormat;

/**
 * Initializer that registers protobuf schema for java.lang.* classes.
 * @author Paul Ferraro
 */
public class LangSerializationContextInitializer extends AbstractSerializationContextInitializer {

    public LangSerializationContextInitializer() {
        super("java.lang.proto");
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        for (AnyField field : EnumSet.allOf(AnyField.class)) {
            Class<?> fieldClass = field.getJavaClass();
            if (fieldClass != Void.class) {
                context.registerMarshaller(new AnyFieldProtoStreamMarshaller(field));
            }
        }
    }

    private static class AnyFieldProtoStreamMarshaller implements ProtoStreamMarshaller<Object> {
        private final AnyField field;

        AnyFieldProtoStreamMarshaller(AnyField field) {
            this.field = field;
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            OptionalInt size = this.field.size(context, value);
            return size.isPresent() ? OptionalInt.of(size.getAsInt() + Predictable.unsignedIntSize(this.field.getIndex() << 3 | WireFormat.WIRETYPE_VARINT)) : OptionalInt.empty();
        }

        @Override
        public String getTypeName() {
            return this.field.getTypeName();
        }

        @Override
        public Class<? extends Object> getJavaClass() {
            return this.field.getJavaClass();
        }

        @Override
        public Object readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            if (WireFormat.getTagFieldNumber(reader.readTag()) != this.field.getIndex()) {
                throw new StreamCorruptedException();
            }
            Object result = this.field.readFrom(context, reader);
            if (reader.readTag() != 0) {
                throw new StreamCorruptedException();
            }
            return result;
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object object) throws IOException {
            this.field.writeTo(context, writer, object);
        }
    }
}
