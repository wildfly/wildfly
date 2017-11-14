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
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.EnumSet;
import java.util.function.BiConsumer;

import org.junit.Assert;
import org.wildfly.clustering.marshalling.Externalizer;

/**
 * @author Paul Ferraro
 */
public class ExternalizerTestUtil {

    public static <E extends Enum<E>> void test(Externalizer<E> externalizer) throws IOException, ClassNotFoundException {
        for (E value : EnumSet.allOf(externalizer.getTargetClass())) {
            test(externalizer, value);
        }
    }

    public static <T> void test(Externalizer<T> externalizer, T subject) throws IOException, ClassNotFoundException {
        test(externalizer, subject, Assert::assertEquals);
    }

    public static <T> void test(Externalizer<T> externalizer, T subject, BiConsumer<T, T> assertion) throws IOException, ClassNotFoundException {
        assertTrue(externalizer.getTargetClass().isInstance(subject));

        ByteArrayOutputStream externalizedOutput = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(externalizedOutput)) {
            externalizer.writeObject(output, subject);
        }

        byte[] externalizedBytes = externalizedOutput.toByteArray();

        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(externalizedBytes))) {
            T result = externalizer.readObject(input);
            assertion.accept(subject, result);
            assertTrue(externalizer.getTargetClass().isInstance(result));
        }

        // If object is serializable, make sure we've actually improved upon default serialization size
        if (subject instanceof java.io.Serializable) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ObjectOutputStream output = new ObjectOutputStream(out)) {
                output.writeObject(subject);
            }

            byte[] bytes = out.toByteArray();
            Assert.assertTrue(externalizedBytes.length < bytes.length);
        }
    }
}
