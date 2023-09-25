/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

/**
 * Marshaller that uses Java serialization.
 * @author Paul Ferraro
 */
public class SerializationTestMarshaller<T> implements TestMarshaller<T> {

    @SuppressWarnings("unchecked")
    @Override
    public T read(ByteBuffer buffer) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(buffer.array(), buffer.arrayOffset(), buffer.limit() - buffer.arrayOffset());
        try (ObjectInputStream input = new ObjectInputStream(in)) {
            return (T) input.readObject();
        } catch (ClassNotFoundException e) {
            InvalidClassException exception = new InvalidClassException(e.getMessage());
            exception.initCause(e);
            throw exception;
        }
    }

    @Override
    public ByteBuffer write(T object) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(out)) {
            output.writeObject(object);
        }

        return ByteBuffer.wrap(out.toByteArray());
    }
}
