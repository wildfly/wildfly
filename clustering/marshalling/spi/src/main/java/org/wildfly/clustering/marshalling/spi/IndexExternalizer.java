/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.wildfly.clustering.marshalling.Externalizer;

/**
 * Various strategies for marshalling an array/collection index (i.e. an unsigned integer).
 * @author Paul Ferraro
 */
public enum IndexExternalizer implements Externalizer<Integer> {

    UNSIGNED_BYTE() {
        @Override
        public int readData(DataInput input) throws IOException {
            return input.readUnsignedByte();
        }

        @Override
        public void writeData(DataOutput output, int index) throws IOException {
            if (index > (Byte.MAX_VALUE - Byte.MIN_VALUE)) {
                throw new IndexOutOfBoundsException(Integer.toString(index));
            }
            output.writeByte(index);
        }
    },
    UNSIGNED_SHORT() {
        @Override
        public int readData(DataInput input) throws IOException {
            return input.readUnsignedShort();
        }

        @Override
        public void writeData(DataOutput output, int index) throws IOException {
            if (index > (Short.MAX_VALUE - Short.MIN_VALUE)) {
                throw new IndexOutOfBoundsException(Integer.toString(index));
            }
            output.writeShort(index);
        }
    },
    INTEGER() {
        @Override
        public int readData(DataInput input) throws IOException {
            return input.readInt();
        }

        @Override
        public void writeData(DataOutput output, int index) throws IOException {
            output.writeInt(index);
        }
    },
    /**
     * Reads/write an unsigned integer using a variable-length format.
     * Format requires between 1 and 5 bytes, depending on the index size.
     * Smaller values require fewer bytes.
     * Logic lifted directly from org.infinispan.commons.io.UnsignedNumeric.
     * @author Manik Surtani
     */
    VARIABLE() {
        @Override
        public int readData(DataInput input) throws IOException {
            byte b = input.readByte();
            int i = b & 0x7F;
            for (int shift = 7; (b & 0x80) != 0; shift += 7) {
                b = input.readByte();
                i |= (b & 0x7FL) << shift;
            }
            return i;
        }

        @Override
        public void writeData(DataOutput output, int index) throws IOException {
            int i = index;
            while ((i & ~0x7F) != 0) {
                output.writeByte((byte) ((i & 0x7f) | 0x80));
                i >>>= 7;
            }
            output.writeByte((byte) i);
        }
    },
    ;

    /**
     * Returns the most efficient externalizer for a given index size.
     * @param size the size of the index
     * @return an index externalizer
     */
    public static final IndexExternalizer select(int size) {
        if (size <= (Byte.MAX_VALUE - Byte.MIN_VALUE)) return UNSIGNED_BYTE;
        if (size <= (Short.MAX_VALUE - Short.MIN_VALUE)) return UNSIGNED_SHORT;
        return VARIABLE;
    }

    /**
     * Read an index from the specified input stream.
     * @param input a data input stream
     * @return the index
     * @throws IOException if an I/O error occurs
     */
    public abstract int readData(DataInput input) throws IOException;

    /**
     * Writes the specified index to the specified output stream
     * @param output the data output stream
     * @param index the index
     * @throws IOException if an I/O error occurs
     */
    public abstract void writeData(DataOutput output, int index) throws IOException;

    @Override
    public Integer readObject(ObjectInput input) throws IOException {
        return Integer.valueOf(this.readData(input));
    }

    @Override
    public void writeObject(ObjectOutput output, Integer index) throws IOException {
        assert index != null;
        this.writeData(output, index.intValue());
    }

    @Override
    public Class<Integer> getTargetClass() {
        return Integer.class;
    }
}
