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
import java.io.ObjectOutput;
import java.nio.ByteBuffer;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.wildfly.clustering.marshalling.spi.ByteBufferOutputStream;

/**
 * {@link ObjectOutput} facade for a {@link RawProtoStreamWriter} allowing externalizers to write protobuf messages.
 * This implementation intentionally does not conform to the binary layout prescribed by {@link ObjectOutput}.
 * @author Paul Ferraro
 */
public class ProtoStreamObjectOutput extends ProtoStreamDataOutput implements ObjectOutput {

    private final ImmutableSerializationContext context;
    private final RawProtoStreamWriter writer;

    public ProtoStreamObjectOutput(ImmutableSerializationContext context, RawProtoStreamWriter writer) {
        super(writer);
        this.context = context;
        this.writer = writer;
    }

    @Override
    public void writeObject(Object object) throws IOException {
        Any any = new Any(object);
        try (ByteBufferOutputStream output = new ByteBufferOutputStream(Predictable.computeSizeNoTag(this.context, any))) {
            ProtobufUtil.writeTo(this.context, output, any);
            ByteBuffer buffer = output.getBuffer();
            int offset = buffer.arrayOffset();
            int length = buffer.limit() - offset;
            this.writeChar(length); // unsigned varint
            this.write(buffer.array(), offset, length);
        }
    }

    @Override
    public void flush() throws IOException {
        this.writer.flush();
    }

    @Override
    public void close() throws IOException {
        this.flush();
    }
}
