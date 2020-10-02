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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.infinispan.commons.io.ByteBufferImpl;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.IndexSerializer;

/**
 * Externalizer for a {@link ByteBufferImpl}.
 * @author Paul Ferraro
 */
public enum ByteBufferExternalizer implements Externalizer<ByteBufferImpl> {
    INSTANCE;

    @Override
    public void writeObject(ObjectOutput output, ByteBufferImpl buffer) throws IOException {
        IndexSerializer.VARIABLE.writeInt(output, (buffer != null) ? buffer.getLength() : 0);
        if (buffer != null) {
            output.write(buffer.getBuf(), buffer.getOffset(), buffer.getLength());
        }
    }

    @Override
    public ByteBufferImpl readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        int length = IndexSerializer.VARIABLE.readInt(input);
        if (length == 0) {
            return null;
        }
        byte[] bytes = new byte[length];
        input.readFully(bytes);
        return ByteBufferImpl.create(bytes);
    }

    @Override
    public Class<ByteBufferImpl> getTargetClass() {
        return ByteBufferImpl.class;
    }
}
