/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import java.util.OptionalInt;

import org.wildfly.clustering.marshalling.Externalizer;

/**
 * Externalizer for a {@link ByteBufferMarshalledValue}.
 * @author Paul Ferraro
 */
public class ByteBufferMarshalledValueExternalizer implements Externalizer<ByteBufferMarshalledValue<Object>> {

    static ByteBuffer readBuffer(ObjectInput input) throws IOException {
        int size = IndexSerializer.VARIABLE.readInt(input);
        byte[] bytes = (size > 0) ? new byte[size] : null;
        if (bytes != null) {
            input.readFully(bytes);
        }
        return (bytes != null) ? ByteBuffer.wrap(bytes) : null;
    }

    static void writeBuffer(ObjectOutput output, ByteBuffer buffer) throws IOException {
        int length = (buffer != null) ? buffer.limit() - buffer.arrayOffset() : 0;
        IndexSerializer.VARIABLE.writeInt(output, length);
        if (length > 0) {
            output.write(buffer.array(), buffer.arrayOffset(), length);
        }
    }

    @Override
    public ByteBufferMarshalledValue<Object> readObject(ObjectInput input) throws IOException {
        return new ByteBufferMarshalledValue<>(readBuffer(input));
    }

    @Override
    public void writeObject(ObjectOutput output, ByteBufferMarshalledValue<Object> object) throws IOException {
        writeBuffer(output, object.getBuffer());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<ByteBufferMarshalledValue<Object>> getTargetClass() {
        return (Class<ByteBufferMarshalledValue<Object>>) (Class<?>) ByteBufferMarshalledValue.class;
    }

    @Override
    public OptionalInt size(ByteBufferMarshalledValue<Object> value) {
        return value.size();
    }
}
