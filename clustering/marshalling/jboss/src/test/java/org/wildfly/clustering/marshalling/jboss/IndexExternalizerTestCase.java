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

package org.wildfly.clustering.marshalling.jboss;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Test;
import org.wildfly.clustering.marshalling.Externalizer;

/**
 * Unit test for {@link IndexExternalizer}.
 * @author Paul Ferraro
 */
public class IndexExternalizerTestCase {

    @Test
    public void test() throws IOException, ClassNotFoundException {
        test(IndexExternalizer.UNSIGNED_BYTE, Byte.MAX_VALUE - Byte.MIN_VALUE);
        illegal(IndexExternalizer.UNSIGNED_BYTE, Byte.MAX_VALUE - Byte.MIN_VALUE + 1);
        test(IndexExternalizer.UNSIGNED_SHORT, Byte.MAX_VALUE - Byte.MIN_VALUE + 1);
        test(IndexExternalizer.UNSIGNED_SHORT, Short.MAX_VALUE - Short.MIN_VALUE);
        illegal(IndexExternalizer.UNSIGNED_SHORT, Short.MAX_VALUE - Short.MIN_VALUE + 1);
        test(IndexExternalizer.INTEGER, Short.MAX_VALUE - Short.MIN_VALUE + 1);
        for (int i = 0; i < Integer.SIZE - 2; ++i) {
            int value = 2 << i;
            test(IndexExternalizer.VARIABLE, value - 1);
            test(IndexExternalizer.VARIABLE, value);
        }
        test(IndexExternalizer.VARIABLE, Integer.MAX_VALUE);
    }

    private static void test(Externalizer<Integer> externalizer, int index) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(out)) {
            externalizer.writeObject(output, index);
        }

        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(out.toByteArray()))) {
            assertEquals(index, externalizer.readObject(input).intValue());
        }
    }

    private static void illegal(Externalizer<Integer> externalizer, int index) {
        try (ObjectOutputStream output = new ObjectOutputStream(new ByteArrayOutputStream())) {
            externalizer.writeObject(output, index);
            fail(String.format("%d should not be marshallable by %s", index, externalizer.getClass().getName()));
        } catch (Throwable e) {
            assertTrue(e.toString(), e instanceof IndexOutOfBoundsException);
        }
    }
}
