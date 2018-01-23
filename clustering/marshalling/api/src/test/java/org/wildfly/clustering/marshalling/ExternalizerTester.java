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

package org.wildfly.clustering.marshalling;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.function.BiConsumer;

import org.junit.Assert;

/**
 * Tester for an {@link Externalizer}.
 * @author Paul Ferraro
 */
public class ExternalizerTester<T> {

    private final Externalizer<T> externalizer;
    private final BiConsumer<T, T> assertion;

    public ExternalizerTester(Externalizer<T> externalizer) {
        this(externalizer, Assert::assertEquals);
    }

    public ExternalizerTester(Externalizer<T> externalizer, BiConsumer<T, T> assertion) {
        this.externalizer = externalizer;
        this.assertion = assertion;
    }

    public void test(T subject) throws IOException, ClassNotFoundException {
        assertTrue(this.externalizer.getTargetClass().isInstance(subject));

        ByteArrayOutputStream externalizedOutput = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(externalizedOutput)) {
            this.externalizer.writeObject(output, subject);
        }

        byte[] externalizedBytes = externalizedOutput.toByteArray();

        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(externalizedBytes))) {
            T result = this.externalizer.readObject(input);
            assertTrue(this.externalizer.getTargetClass().isInstance(result));
            this.assertion.accept(subject, result);
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
