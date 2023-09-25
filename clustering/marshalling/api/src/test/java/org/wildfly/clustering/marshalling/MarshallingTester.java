/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
