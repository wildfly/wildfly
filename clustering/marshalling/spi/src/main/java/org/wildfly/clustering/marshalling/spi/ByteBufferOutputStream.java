/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.OptionalInt;

/**
 * A specialized ByteArrayOutputStream that exposes the internal buffer.
 * @author Paul Ferraro
 */
public final class ByteBufferOutputStream extends ByteArrayOutputStream {

    public ByteBufferOutputStream() {
        this(OptionalInt.empty());
    }

    public ByteBufferOutputStream(OptionalInt size) {
        this(size.orElse(512));
    }

    public ByteBufferOutputStream(int size) {
        super(size);
    }

    /**
     * Returns the internal buffer of this output stream.
     * @return the internal byte buffer.
     */
    public ByteBuffer getBuffer() {
        return ByteBuffer.wrap(this.buf, 0, this.count);
    }
}
