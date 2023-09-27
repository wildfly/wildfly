/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.wildfly.clustering.marshalling.Externalizer;

/**
 * Externalizer for a {@link ByteBufferMarshalledKey}.
 * @author Paul Ferraro
 */
public class ByteBufferMarshalledKeyExternalizer implements Externalizer<ByteBufferMarshalledKey<Object>> {

    @Override
    public void writeObject(ObjectOutput output, ByteBufferMarshalledKey<Object> value) throws IOException {
        ByteBufferMarshalledValueExternalizer.writeBuffer(output, value.getBuffer());
        output.writeInt(value.hashCode());
    }

    @Override
    public ByteBufferMarshalledKey<Object> readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        return new ByteBufferMarshalledKey<>(ByteBufferMarshalledValueExternalizer.readBuffer(input), input.readInt());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<ByteBufferMarshalledKey<Object>> getTargetClass() {
        return (Class<ByteBufferMarshalledKey<Object>>) (Class<?>) ByteBufferMarshalledKey.class;
    }
}
