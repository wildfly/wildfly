/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Various strategies for marshalling an array/collection index (i.e. an unsigned integer).
 * @author Paul Ferraro
 */
public enum IndexSerializer implements IntSerializer {

    UNSIGNED_BYTE() {
        @Override
        public int readInt(DataInput input) throws IOException {
            return input.readUnsignedByte();
        }

        @Override
        public void writeInt(DataOutput output, int index) throws IOException {
            if (index > (Byte.MAX_VALUE - Byte.MIN_VALUE)) {
                throw new IndexOutOfBoundsException(Integer.toString(index));
            }
            output.writeByte(index);
        }

        @Override
        public int size(int value) {
            return Byte.BYTES;
        }
    },
    UNSIGNED_SHORT() {
        @Override
        public int readInt(DataInput input) throws IOException {
            return input.readUnsignedShort();
        }

        @Override
        public void writeInt(DataOutput output, int index) throws IOException {
            if (index > (Short.MAX_VALUE - Short.MIN_VALUE)) {
                throw new IndexOutOfBoundsException(Integer.toString(index));
            }
            output.writeShort(index);
        }

        @Override
        public int size(int value) {
            return Short.BYTES;
        }
    },
    INTEGER(),
    /**
     * Reads/write an unsigned integer using a variable-length format.
     * Format requires between 1 and 5 bytes, depending on the index size.
     * Smaller values require fewer bytes.
     * Logic lifted directly from org.infinispan.commons.io.UnsignedNumeric.
     * @author Manik Surtani
     */
    VARIABLE() {
        @Override
        public int readInt(DataInput input) throws IOException {
            byte b = input.readByte();
            int i = b & 0x7F;
            for (int shift = 7; (b & 0x80) != 0; shift += 7) {
                b = input.readByte();
                i |= (b & 0x7FL) << shift;
            }
            return i;
        }

        @Override
        public void writeInt(DataOutput output, int index) throws IOException {
            int i = index;
            while ((i & ~0x7F) != 0) {
                output.writeByte((byte) ((i & 0x7f) | 0x80));
                i >>>= 7;
            }
            output.writeByte((byte) i);
        }

        @Override
        public int size(int index) {
            int size = 1;
            int i = index;
            while ((i & ~0x7F) != 0) {
                size += 1;
                i >>>= 7;
            }
            return size;
        }
    },
    ;

    /**
     * Returns the most efficient externalizer for a given index size.
     * @param size the size of the index
     * @return an index externalizer
     */
    public static IntSerializer select(int size) {
        if (size < 256) return UNSIGNED_BYTE;
        if (size < 65536) return UNSIGNED_SHORT;
        if (size < 268435456) return VARIABLE;
        return INTEGER;
    }
}
