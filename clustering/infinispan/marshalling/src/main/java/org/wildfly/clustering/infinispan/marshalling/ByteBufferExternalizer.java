/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
