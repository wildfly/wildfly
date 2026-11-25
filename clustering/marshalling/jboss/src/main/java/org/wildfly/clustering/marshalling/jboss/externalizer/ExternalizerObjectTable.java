/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.jboss.externalizer;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.ObjectTable;
import org.jboss.marshalling.Unmarshaller;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.IndexSerializer;
import org.wildfly.clustering.marshalling.IntSerializer;

/**
 * {@link ObjectTable} implementation that dynamically loads {@link Externalizer} instances available from a given {@link ClassLoader}.
 * @author Paul Ferraro
 */
@Deprecated
public class ExternalizerObjectTable implements ObjectTable {

    private final List<Externalizer<?>> externalizers;
    private final Map<Class<?>, Integer> indexes = new IdentityHashMap<>();
    private final IntSerializer indexSerializer;

    public ExternalizerObjectTable(List<Externalizer<?>> externalizers) {
        this(IndexSerializer.select(externalizers.size()), externalizers);
    }

    @SafeVarargs
    public ExternalizerObjectTable(Externalizer<?>... externalizers) {
        this(List.of(externalizers));
    }

    private ExternalizerObjectTable(IntSerializer indexSerializer, List<Externalizer<?>> externalizers) {
        this.indexSerializer = indexSerializer;
        this.externalizers = externalizers;
        ListIterator<Externalizer<?>> iterator = externalizers.listIterator();
        while (iterator.hasNext()) {
            this.indexes.putIfAbsent(iterator.next().getTargetClass(), iterator.previousIndex());
        }
    }

    @Override
    public Writer getObjectWriter(final Object object) throws IOException {
        Class<?> targetClass = object.getClass().isEnum() ? ((Enum<?>) object).getDeclaringClass() : object.getClass();
        Class<?> superClass = targetClass.getSuperclass();
        // If implementation class has no externalizer, search any abstract superclasses
        while (!this.indexes.containsKey(targetClass) && (superClass != null) && Modifier.isAbstract(superClass.getModifiers())) {
            targetClass = superClass;
            superClass = targetClass.getSuperclass();
        }
        Integer index = this.indexes.get(targetClass);
        return (index != null) ? new ExternalizerWriter(index, this.indexSerializer, this.externalizers.get(index)) : null;
    }

    @Override
    public Object readObject(Unmarshaller unmarshaller) throws IOException, ClassNotFoundException {
        int index = this.indexSerializer.readInt(unmarshaller);
        if (index >= this.externalizers.size()) {
            throw new IllegalStateException();
        }
        return this.externalizers.get(index).readObject(unmarshaller);
    }

    private static class ExternalizerWriter implements ObjectTable.Writer {
        private final int index;
        private final IntSerializer serializer;
        private final Externalizer<Object> externalizer;

        @SuppressWarnings("unchecked")
        ExternalizerWriter(int index, IntSerializer serializer, Externalizer<?> externalizer) {
            this.index = index;
            this.serializer = serializer;
            this.externalizer = (Externalizer<Object>) externalizer;
        }

        @Override
        public void writeObject(Marshaller marshaller, Object object) throws IOException {
            this.serializer.writeInt(marshaller, this.index);
            this.externalizer.writeObject(marshaller, object);
        }
    }
}
