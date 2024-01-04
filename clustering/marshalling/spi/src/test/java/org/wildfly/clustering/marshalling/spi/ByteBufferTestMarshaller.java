/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.OptionalInt;

import org.junit.Assert;
import org.wildfly.clustering.marshalling.TestMarshaller;

/**
 * A {@link ByteBufferMarshaller} based {@link TestMarshaller}.
 * @author Paul Ferraro
 */
public class ByteBufferTestMarshaller<T> implements TestMarshaller<T> {

    private final ByteBufferMarshaller marshaller;

    public ByteBufferTestMarshaller(ByteBufferMarshaller marshaller) {
        this.marshaller = marshaller;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T read(ByteBuffer buffer) throws IOException {
        return (T) this.marshaller.read(buffer);
    }

    @Override
    public ByteBuffer write(T object) throws IOException {
        ByteBuffer buffer = this.marshaller.write(object);
        OptionalInt size = this.marshaller.size(object);
        if (size.isPresent()) {
            Assert.assertEquals(buffer.remaining(), size.getAsInt());
        }
        return buffer;
    }
}
