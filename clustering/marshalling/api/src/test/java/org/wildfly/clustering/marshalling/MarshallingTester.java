/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.BiConsumer;

import org.junit.Assert;

/**
 * Validates correctness of the marshalling of an object.
 * @author Paul Ferraro
 */
public class MarshallingTester<T> implements Tester<T> {

    private final TestMarshaller<T> serializationMarshaller = new SerializationTestMarshaller<>();
    private final TestMarshaller<T> marshaller;

    public MarshallingTester(TestMarshaller<T> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public void test(T subject, BiConsumer<T, T> assertion) throws IOException {
        ByteBuffer buffer = this.marshaller.write(subject);
        int size = buffer.limit() - buffer.arrayOffset();

        if (subject != null) {
            // Uncomment to report payload size
            // System.out.println(String.format("%s\t%s\t%s", (subject instanceof Enum) ? ((Enum<?>) subject).getDeclaringClass().getCanonicalName() : subject.getClass().getCanonicalName(), (subject instanceof Character) ? (int) (Character) subject : subject, size));
        }

        T result = this.marshaller.read(buffer);

        assertion.accept(subject, result);

        // If object is serializable, verify that we have improved upon default serialization size
        if (subject instanceof java.io.Serializable) {
            ByteBuffer serializationBuffer = this.serializationMarshaller.write(subject);
            int serializationSize = serializationBuffer.limit() - serializationBuffer.arrayOffset();
            Assert.assertTrue(String.format("Marshaller size = %d, Default serialization size = %d", size, serializationSize), size < serializationSize);
        }
    }
}
