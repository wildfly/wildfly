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
import java.nio.ByteBuffer;
import java.util.OptionalInt;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.ProtobufTagMarshaller.WriteContext;
import org.infinispan.protostream.impl.TagWriterImpl;
import org.wildfly.clustering.marshalling.spi.ByteBufferOutputStream;

/**
 * {@link ProtoStreamWriter} implementation that writes to a {@link CodedOutputStream}.
 * @author Paul Ferraro
 */
public class DefaultProtoStreamWriter extends AbstractProtoStreamWriter {

    public DefaultProtoStreamWriter(WriteContext context) {
        super(context);
    }

    @Override
    public void writeObjectNoTag(Object value) throws IOException {
        ImmutableSerializationContext context = this.getSerializationContext();
        ProtoStreamMarshaller<Object> marshaller = this.findMarshaller(value.getClass());
        OptionalInt size = this.size(marshaller, value);
        try (ByteBufferOutputStream output = new ByteBufferOutputStream(size)) {
            TagWriterImpl writer = size.isPresent() ? TagWriterImpl.newInstance(context, output, size.getAsInt()) : TagWriterImpl.newInstance(context,  output);
            marshaller.writeTo(new DefaultProtoStreamWriter(writer), value);
            writer.flush();
            ByteBuffer buffer = output.getBuffer();
            int offset = buffer.arrayOffset();
            int length = buffer.limit() - offset;
            this.writeVarint32(length);
            this.writeRawBytes(buffer.array(), offset, length);
        }
    }

    private OptionalInt size(ProtoStreamMarshaller<Object> marshaller, Object value) {
        SizeComputingProtoStreamWriter writer = new SizeComputingProtoStreamWriter(this.getSerializationContext());
        try (ProtoStreamWriterContext context = ProtoStreamWriterContext.FACTORY.get().apply(writer)) {
            marshaller.writeTo(writer, value);
            return writer.get();
        } catch (IOException e) {
            return OptionalInt.empty();
        }
    }
}