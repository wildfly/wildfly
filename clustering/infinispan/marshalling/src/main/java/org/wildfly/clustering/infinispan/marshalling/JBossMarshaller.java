/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.infinispan.marshalling;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.io.ByteBufferImpl;
import org.infinispan.commons.io.ExposedByteArrayOutputStream;
import org.infinispan.commons.marshall.AbstractMarshaller;
import org.infinispan.commons.marshall.BufferSizePredictor;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.SimpleDataInput;
import org.jboss.marshalling.SimpleDataOutput;
import org.jboss.marshalling.Unmarshaller;
import org.wildfly.clustering.marshalling.jboss.MarshallingContext;
import org.wildfly.clustering.marshalling.spi.IndexSerializer;

/**
 * @author Paul Ferraro
 */
public class JBossMarshaller extends AbstractMarshaller {

    private final MarshallingContext context;

    public JBossMarshaller(MarshallingContext context) {
        this.context = context;
    }

    @Override
    public MediaType mediaType() {
        return MediaType.APPLICATION_JBOSS_MARSHALLING;
    }

    @Override
    public boolean isMarshallable(Object object) {
        return this.context.isMarshallable(object);
    }

    @Override
    public Object objectFromByteBuffer(byte[] buf, int offset, int length) throws IOException, ClassNotFoundException {
        ByteArrayInputStream input = new ByteArrayInputStream(buf, offset, length);
        try (SimpleDataInput data = new SimpleDataInput(Marshalling.createByteInput(input))) {
            int version = IndexSerializer.UNSIGNED_BYTE.readInt(data);
            try (Unmarshaller unmarshaller = this.context.createUnmarshaller(version)) {
                unmarshaller.start(data);
                Object result = unmarshaller.readObject();
                unmarshaller.finish();
                return result;
            }
        }
    }

    @Override
    public byte[] objectToByteBuffer(Object object) throws IOException, InterruptedException {
        if (object == null) {
            return this.objectToByteBuffer(null, 1);
        }
        BufferSizePredictor predictor = this.getBufferSizePredictor(object.getClass());
        byte[] bytes = this.objectToByteBuffer(object, predictor.nextSize(object));
        int actualSize = bytes.length - Byte.BYTES;
        predictor.recordSize(actualSize);
        return bytes;
    }

    @Override
    public ByteBuffer objectToBuffer(Object object) throws IOException, InterruptedException {
        if (object == null) {
            return this.objectToBuffer(null, 1);
        }
        BufferSizePredictor predictor = this.getBufferSizePredictor(object.getClass());
        int estimatedSize = predictor.nextSize(object);
        ByteBuffer buffer = this.objectToBuffer(object, estimatedSize);
        int actualSize = buffer.getLength() - Byte.BYTES;
        // If the prediction is way off, then trim it
        if (estimatedSize > (actualSize * 4)) {
            byte[] bytes = trim(buffer);
            buffer = new ByteBufferImpl(bytes, 0, bytes.length);
        }
        predictor.recordSize(actualSize);
        return buffer;
    }

    @Override
    protected ByteBuffer objectToBuffer(Object object, int estimatedSize) throws IOException, InterruptedException {
        int version = this.context.getCurrentVersion();
        ExposedByteArrayOutputStream output = new ExposedByteArrayOutputStream(estimatedSize + Byte.BYTES);
        try (SimpleDataOutput data = new SimpleDataOutput(Marshalling.createByteOutput(output))) {
            IndexSerializer.UNSIGNED_BYTE.writeInt(data, version);
            try (Marshaller marshaller = this.context.createMarshaller(version)) {
                marshaller.start(data);
                marshaller.writeObject(object);
                marshaller.finish();
                return new ByteBufferImpl(output.getRawBuffer(), 0, output.size());
            }
        }
    }

    private static byte[] trim(ByteBuffer buffer) {
        byte[] bytes = buffer.getBuf();
        int offset = buffer.getOffset();
        int length = buffer.getLength();
        // Nothing to trim?
        if ((offset == 0) && (bytes.length == length)) return bytes;
        byte[] result = new byte[length];
        System.arraycopy(bytes, offset, result, 0, length);
        return result;
    }
}
