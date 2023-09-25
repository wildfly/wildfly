/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * A {@ByteBufferMarshaller} that uses Java serialization.
 * @author Paul Ferraro
 */
public enum JavaByteBufferMarshaller implements ByteBufferMarshaller {
    INSTANCE;

    @Override
    public boolean isMarshallable(Object object) {
        return object instanceof Serializable;
    }

    @Override
    public Object readFrom(InputStream input) throws IOException {
        try {
            return new ObjectInputStream(input).readObject();
        } catch (ClassNotFoundException e) {
            InvalidClassException exception = new InvalidClassException(e.getMessage());
            exception.initCause(e);
            throw exception;
        }
    }

    @Override
    public void writeTo(OutputStream output, Object value) throws IOException {
        new ObjectOutputStream(output).writeObject(value);
    }
}
