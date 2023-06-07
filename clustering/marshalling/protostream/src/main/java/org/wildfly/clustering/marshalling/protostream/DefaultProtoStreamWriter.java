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
import java.util.function.Function;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.ProtobufTagMarshaller.WriteContext;
import org.infinispan.protostream.impl.TagWriterImpl;
import org.wildfly.clustering.marshalling.spi.ByteBufferOutputStream;

/**
 * {@link ProtoStreamWriter} implementation that writes to a {@link TagWriterImpl}.
 * @author Paul Ferraro
 */
public class DefaultProtoStreamWriter extends AbstractProtoStreamWriter implements Function<Object, OptionalInt> {

    private final ProtoStreamWriterContext context;

    /**
     * Creates a default ProtoStream writer.
     * @param context the write context
     */
    public DefaultProtoStreamWriter(WriteContext context) {
        this(context, new DefaultProtoStreamWriterContext());
    }

    private DefaultProtoStreamWriter(WriteContext context, ProtoStreamWriterContext writerContext) {
        super(context, writerContext);
        this.context = writerContext;
    }

    @Override
    public ProtoStreamOperation.Context getContext() {
        return this.context;
    }

    @Override
    public void writeObjectNoTag(Object value) throws IOException {
        ImmutableSerializationContext context = this.getSerializationContext();
        ProtoStreamMarshaller<Object> marshaller = this.findMarshaller(value.getClass());
        OptionalInt size = this.context.computeSize(value, this);
        if (size.isPresent()) {
            // If size is known, we can marshal directly to our output stream
            int length = size.getAsInt();
            this.writeVarint32(length);
            if (length > 0) {
                marshaller.writeTo(this, value);
            }
        } else {
            // If size is unknown, marshal to an expandable temporary buffer
            // This should only be the case if delegating to JBoss Marshalling or Java Serialization
            try (ByteBufferOutputStream output = new ByteBufferOutputStream()) {
                TagWriterImpl writer = TagWriterImpl.newInstanceNoBuffer(context, output);
                marshaller.writeTo(new DefaultProtoStreamWriter(writer, this.context), value);
                // Byte buffer is array backed
                ByteBuffer buffer = output.getBuffer();
                int offset = buffer.arrayOffset();
                int length = buffer.limit() - offset;
                this.writeVarint32(length);
                if (length > 0) {
                    this.writeRawBytes(buffer.array(), offset, length);
                }
            }
        }
    }

    @Override
    public OptionalInt apply(Object value) {
        ProtoStreamMarshaller<Object> marshaller = this.findMarshaller(value.getClass());
        // Retain reference integrity by using a copy of the current context during size operation
        return marshaller.size(new DefaultProtoStreamSizeOperation(this.getSerializationContext(), this.context.clone()), value);
    }
}