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

package org.wildfly.clustering.infinispan.marshalling.protostream;

import java.io.IOException;

import org.infinispan.commons.io.ByteBufferImpl;
import org.infinispan.protostream.impl.WireFormat;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshaller for an Infinispan ByteBuffer.
 * @author Paul Ferraro
 */
public enum ByteBufferMarshaller implements ProtoStreamMarshaller<ByteBufferImpl> {
    INSTANCE;

    private static final int BUFFER_INDEX = 1;

    @Override
    public ByteBufferImpl readFrom(ProtoStreamReader reader) throws IOException {
        ByteBufferImpl buffer = ByteBufferImpl.EMPTY_INSTANCE;
        boolean reading = true;
        while (reading) {
            int tag = reader.readTag();
            switch (WireFormat.getTagFieldNumber(tag)) {
                case BUFFER_INDEX:
                    buffer = ByteBufferImpl.create(reader.readByteArray());
                    break;
                default:
                    reading = (tag != 0) && reader.skipField(tag);
            }
        }
        return buffer;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, ByteBufferImpl buffer) throws IOException {
        int length = buffer.getLength();
        if (length > 0) {
            writer.writeBytes(BUFFER_INDEX, buffer.getBuf(), buffer.getOffset(), length);
        }
    }

    @Override
    public Class<? extends ByteBufferImpl> getJavaClass() {
        return ByteBufferImpl.class;
    }
}
