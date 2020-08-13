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