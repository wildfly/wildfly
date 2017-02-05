/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.remote._private;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link org.jboss.ejb.protocol.remote.PackedInteger} is a variable-length integer. The most-significant bit of each byte of a
 * {@link org.jboss.ejb.protocol.remote.PackedInteger} value indicates whether that byte is the final (lowest-order) byte of the value.
 * If the bit is 0, then this is the last byte; if the bit is 1, then there is at least one more subsequent
 * byte pending, and the current value should be shifted to the left by 7 bits to accommodate the next byte's data.
 * <p/>
 * Note: {@link org.jboss.ejb.protocol.remote.PackedInteger} cannot hold signed integer values.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class PackedInteger {


    /**
     * Reads a {@link org.jboss.ejb.protocol.remote.PackedInteger} value from the passed {@link java.io.DataInput input} and returns the
     * value of the integer.
     *
     * @param input The {@link java.io.DataInput} from which the {@link org.jboss.ejb.protocol.remote.PackedInteger} value will be read
     * @return
     * @throws java.io.IOException
     * @throws IllegalArgumentException If the passed <code>input</code> is null
     */
    public static int readPackedInteger(final DataInput input) throws IOException {
        int b = input.readByte();
        if ((b & 0x80) == 0x80) {
            return readPackedInteger(input) << 7 | (b & 0x7F);
        }
        return b;
    }

    /**
     * Converts the passed <code>value</code> into a {@link org.jboss.ejb.protocol.remote.PackedInteger} and writes it to the
     * {@link java.io.DataOutput output}
     *
     * @param output The {@link java.io.DataOutput} to which the {@link org.jboss.ejb.protocol.remote.PackedInteger} is written to
     * @param value  The integer value which will be converted to a {@link org.jboss.ejb.protocol.remote.PackedInteger}
     * @throws java.io.IOException
     * @throws IllegalArgumentException If the passed <code>value</code> is < 0. {@link org.jboss.ejb.protocol.remote.PackedInteger} doesn't
     *                                  allow signed integer
     * @throws IllegalArgumentException If the passed <code>output</code> is null
     */
    public static void writePackedInteger(final DataOutput output, int value) throws IOException {
        if (value < 0)
            throw new IllegalArgumentException("Only unsigned integer can be packed");
        if (value > 127) {
            output.writeByte(value & 0x7F | 0x80);
            writePackedInteger(output, value >> 7);
        } else {
            output.writeByte(value & 0xFF);
        }
    }

}
