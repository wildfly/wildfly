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

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.impl.WireFormat;

/**
 * Set of scalar marshallers for marshalling a {@link Class}.
 * @author Paul Ferraro
 */
public enum ScalarClass implements ScalarMarshaller<Class<?>> {

    ANY(WireFormat.WIRETYPE_LENGTH_DELIMITED) {
        @Override
        public Class<?> readFrom(ProtoStreamReader reader) throws IOException {
            return reader.readObject(Class.class);
        }

        @Override
        public void writeTo(ProtoStreamWriter writer, Class<?> value) throws IOException {
            writer.writeObjectNoTag(value);
        }
    },
    ID(WireFormat.WIRETYPE_VARINT) {
        @Override
        public Class<?> readFrom(ProtoStreamReader reader) throws IOException {
            int typeId = reader.readUInt32();
            ImmutableSerializationContext context = reader.getSerializationContext();
            String typeName = context.getDescriptorByTypeId(typeId).getFullName();
            BaseMarshaller<?> marshaller = context.getMarshaller(typeName);
            return marshaller.getJavaClass();
        }

        @Override
        public void writeTo(ProtoStreamWriter writer, Class<?> value) throws IOException {
            ImmutableSerializationContext context = writer.getSerializationContext();
            BaseMarshaller<?> marshaller = context.getMarshaller(value);
            String typeName = marshaller.getTypeName();
            int typeId = context.getDescriptorByName(typeName).getTypeId();
            writer.writeUInt32NoTag(typeId);
        }
    },
    NAME(WireFormat.WIRETYPE_LENGTH_DELIMITED) {
        @Override
        public Class<?> readFrom(ProtoStreamReader reader) throws IOException {
            ImmutableSerializationContext context = reader.getSerializationContext();
            String typeName = reader.readString();
            BaseMarshaller<?> marshaller = context.getMarshaller(typeName);
            return marshaller.getJavaClass();
        }

        @Override
        public void writeTo(ProtoStreamWriter writer, Class<?> value) throws IOException {
            ImmutableSerializationContext context = writer.getSerializationContext();
            BaseMarshaller<?> marshaller = context.getMarshaller(value);
            String typeName = marshaller.getTypeName();
            writer.writeStringNoTag(typeName);
        }
    },
    FIELD(WireFormat.WIRETYPE_VARINT) {
        @Override
        public Class<?> readFrom(ProtoStreamReader reader) throws IOException {
            return AnyField.fromIndex(reader.readUInt32() + 1).getMarshaller().getJavaClass();
        }

        @Override
        public void writeTo(ProtoStreamWriter writer, Class<?> value) throws IOException {
            writer.writeUInt32NoTag(AnyField.fromJavaType(value).getIndex() - 1);
        }
    },
    ;
    private final int wireType;

    ScalarClass(int wireType) {
        this.wireType = wireType;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends Class<?>> getJavaClass() {
        return (Class<? extends Class<?>>) (Class<?>) Class.class;
    }

    @Override
    public int getWireType() {
        return this.wireType;
    }
}
