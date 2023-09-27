/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Writes/reads an integer to/from a binary stream.
 * @author Paul Ferraro
 */
public interface IntSerializer {
    /**
     * Writes the specified integer to the specified output stream
     * @param output the data output stream
     * @param value an integer value
     * @throws IOException if an I/O error occurs
     */
    default void writeInt(DataOutput output, int value) throws IOException {
        output.writeInt(value);
    }

    /**
     * Read an integer from the specified input stream.
     * @param input a data input stream
     * @return the integer value
     * @throws IOException if an I/O error occurs
     */
    default int readInt(DataInput input) throws IOException {
        return input.readInt();
    }

    default int size(int value) {
        return Integer.BYTES;
    }
}
