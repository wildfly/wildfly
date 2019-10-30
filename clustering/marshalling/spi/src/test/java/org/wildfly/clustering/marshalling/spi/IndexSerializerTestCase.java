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

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.stream.IntStream;

import org.junit.Test;

/**
 * Unit test for {@link IndexSerializer}.
 * @author Paul Ferraro
 */
public class IndexSerializerTestCase {

    @Test
    public void test() throws IOException {
        // Test marshalling of incrementing powers of 2
        for (int i = 0; i < Integer.SIZE - 2; ++i) {
            int index = 2 << i;
            test(index - 1);
            test(index);
        }
        test(Integer.MAX_VALUE);
    }

    private static void test(int index) throws IOException {
        IntStream.Builder builder = IntStream.builder();

        try {
            builder.add(size(IndexSerializer.UNSIGNED_BYTE, index));
        } catch (IndexOutOfBoundsException e) {
            assertTrue(index > Byte.MAX_VALUE - Byte.MIN_VALUE);
        }

        try {
            builder.add(size(IndexSerializer.UNSIGNED_SHORT, index));
        } catch (IndexOutOfBoundsException e) {
            assertTrue(index > Short.MAX_VALUE - Short.MIN_VALUE);
        }

        builder.add(size(IndexSerializer.VARIABLE, index));

        builder.add(size(IndexSerializer.INTEGER, index));

        // Ensure that our IndexExternalizer.select(...) chooses the optimal externalizer
        assertEquals(builder.build().min().getAsInt(), size(IndexSerializer.select(index), index));
    }

    public static int size(IntSerializer externalizer, int index) throws IOException {

        ByteArrayOutputStream externalizedOutput = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(externalizedOutput)) {
            externalizer.writeInt(output, index);
        }

        byte[] externalizedBytes = externalizedOutput.toByteArray();

        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(externalizedBytes))) {
            int result = externalizer.readInt(input);
            assertEquals(index, result);
        }

        return externalizedBytes.length;
    }
}
