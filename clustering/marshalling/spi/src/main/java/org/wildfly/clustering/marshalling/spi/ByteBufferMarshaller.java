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

package org.wildfly.clustering.marshalling.spi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.OptionalInt;

/**
 * Marshals an object to and from a {@link ByteBuffer}.
 * @author Paul Ferraro
 */
public interface ByteBufferMarshaller extends Marshaller<Object, ByteBuffer> {

    Object readFrom(InputStream input) throws IOException;

    void writeTo(OutputStream output, Object object) throws IOException;

    @Override
    default Object read(ByteBuffer buffer) throws IOException {
        try (InputStream input = new ByteBufferInputStream(buffer)) {
            return this.readFrom(input);
        }
    }

    @Override
    default ByteBuffer write(Object object) throws IOException {
        try (ByteBufferOutputStream output = new ByteBufferOutputStream(this.size(object))) {
            this.writeTo(output, object);
            return output.getBuffer();
        }
    }

    default OptionalInt size(Object object) {
        return OptionalInt.empty();
    }
}
