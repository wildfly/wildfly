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
