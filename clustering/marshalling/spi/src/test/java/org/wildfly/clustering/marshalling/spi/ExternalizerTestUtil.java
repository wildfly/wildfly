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
import java.util.function.BiConsumer;

import org.wildfly.clustering.marshalling.Externalizer;

/**
 * @author Paul Ferraro
 */
public class ExternalizerTestUtil {

    public static <T> void test(Externalizer<T> externalizer, T subject) throws IOException, ClassNotFoundException {
        test(externalizer, subject, (expected, actual) -> assertEquals(expected, actual));
    }

    public static <T> void test(Externalizer<T> externalizer, T subject, BiConsumer<T, T> assertion) throws IOException, ClassNotFoundException {
        assertTrue(externalizer.getTargetClass().isInstance(subject));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(out)) {
            externalizer.writeObject(output, subject);
        }

        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(out.toByteArray()))) {
            T result = externalizer.readObject(input);
            assertion.accept(subject, result);
        }
    }
}
