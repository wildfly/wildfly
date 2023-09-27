/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Marshaller using Java serialization via an externalizer.
 * @author Paul Ferraro
 */
public class ExternalizerMarshaller<T> implements TestMarshaller<T> {

    private final Map<Class<?>, Externalizer<?>> externalizers;
    private Externalizer<T> currentExternalizer;

    ExternalizerMarshaller(Externalizer<T> externalizer) {
        this.externalizers = Collections.singletonMap(externalizer.getTargetClass(), externalizer);
    }

    ExternalizerMarshaller(Iterable<? extends Externalizer<?>> externalizers) {
        this.externalizers = new HashMap<>();
        for (Externalizer<?> externalizer : externalizers) {
            this.externalizers.put(externalizer.getTargetClass(), externalizer);
        }
    }

    @Override
    public T read(ByteBuffer buffer) throws IOException {
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(buffer.array(), buffer.arrayOffset(), buffer.limit() - buffer.arrayOffset()))) {
            return this.currentExternalizer.readObject(input);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public ByteBuffer write(T object) throws IOException {
        Class<?> targetClass = object.getClass().isEnum() ? ((Enum<?>) object).getDeclaringClass() : object.getClass();
        Class<?> superClass = targetClass.getSuperclass();
        // If implementation class has no externalizer, search any abstract superclasses
        while (!this.externalizers.containsKey(targetClass) && (superClass != null) && Modifier.isAbstract(superClass.getModifiers())) {
            targetClass = superClass;
            superClass = targetClass.getSuperclass();
        }
        this.currentExternalizer = (Externalizer<T>) this.externalizers.get(targetClass);

        ByteArrayOutputStream externalizedOutput = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(externalizedOutput)) {
            this.currentExternalizer.writeObject(output, object);
        }
        return ByteBuffer.wrap(externalizedOutput.toByteArray());
    }
}