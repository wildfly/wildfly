/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.marshalling;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.io.ByteBufferImpl;
import org.infinispan.commons.io.LazyByteArrayOutputStream;
import org.infinispan.commons.marshall.BufferSizePredictor;
import org.infinispan.commons.marshall.MarshallableTypeHints;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.marshall.StreamAwareMarshaller;

/**
 * Abstract marshaller implementation.
 * @author Paul Ferraro
 */
public abstract class AbstractMarshaller implements Marshaller, StreamAwareMarshaller {

    private final MarshallableTypeHints hints = new MarshallableTypeHints();

    @Override
    public void stop() {
        this.hints.clear();
    }

    @Override
    public int sizeEstimate(Object object) {
        return this.getBufferSizePredictor(object).nextSize(object);
    }

    @Override
    public BufferSizePredictor getBufferSizePredictor(Object object) {
        return this.hints.getBufferSizePredictor(object);
    }

    @Override
    public Object objectFromByteBuffer(byte[] bytes) throws IOException, ClassNotFoundException {
       return this.objectFromByteBuffer(bytes, 0, bytes.length);
    }

    @Override
    public Object objectFromByteBuffer(byte[] buf, int offset, int length) throws IOException, ClassNotFoundException {
        ByteArrayInputStream input = new ByteArrayInputStream(buf, offset, length);
        return this.readObject(input);
    }

    @Override
    public byte[] objectToByteBuffer(Object object) throws IOException {
        if (object == null) {
            return this.objectToByteBuffer(null, 1);
        }
        int estimatedSize = this.sizeEstimate(object);
        byte[] bytes = this.objectToByteBuffer(object, estimatedSize);
        int actualSize = bytes.length - Byte.BYTES;
        this.getBufferSizePredictor(object).recordSize(actualSize);
        return bytes;
    }

    @Override
    public ByteBuffer objectToBuffer(Object object) throws IOException {
        if (object == null) {
            return this.objectToBuffer(null, 1);
        }
        int estimatedSize = this.sizeEstimate(object);
        ByteBuffer buffer = this.objectToBuffer(object, estimatedSize);
        int actualSize = buffer.getLength() - Byte.BYTES;
        // If the prediction is way off, then trim it
        if (estimatedSize > (actualSize * 4)) {
            byte[] bytes = trim(buffer);
            buffer = ByteBufferImpl.create(bytes);
        }
        this.getBufferSizePredictor(object).recordSize(actualSize);
        return buffer;
    }

    @Override
    public byte[] objectToByteBuffer(Object obj, int estimatedSize) throws IOException {
       ByteBuffer b = this.objectToBuffer(obj, estimatedSize);
       return trim(b);
    }

    private ByteBuffer objectToBuffer(Object object, int estimatedSize) throws IOException {
        LazyByteArrayOutputStream output = new LazyByteArrayOutputStream(estimatedSize + Byte.BYTES);
        this.writeObject(object, output);
        return ByteBufferImpl.create(output.getRawBuffer(), 0, output.size());
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
