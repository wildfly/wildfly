/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.OptionalInt;

/**
 * Service provider interface for custom externalizers.
 * @author Paul Ferraro
 * @deprecated Use {@link org.jboss.marshalling.Externalizer} instead.
 */
@Deprecated(forRemoval = true)
public interface Externalizer<T> {

    /**
     * Writes the object reference to the stream.
     *
     * @param output the object output to write to
     * @param object the object reference to write
     * @throws IOException if an I/O error occurs
     */
    void writeObject(ObjectOutput output, T object) throws IOException;

    /**
     * Read an instance from the stream.  The instance will have been written by the
     * {@link #writeObject(ObjectOutput, Object)} method.  Implementations are free
     * to create instances of the object read from the stream in any way that they
     * feel like. This could be via constructor, factory or reflection.
     *
     * @param input the object input from which to read
     * @return the object instance
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if a class could not be found
     */
    T readObject(ObjectInput input) throws IOException, ClassNotFoundException;

    /**
     * Returns the target class of the object to externalize.
     * @return a class to be externalized
     */
    Class<T> getTargetClass();

    /**
     * Returns the size of the buffer to use for marshalling the specified object, if known.
     * @return the buffer size (in bytes), or empty if unknown.
     */
    default OptionalInt size(T object) {
        return OptionalInt.empty();
    }
}
