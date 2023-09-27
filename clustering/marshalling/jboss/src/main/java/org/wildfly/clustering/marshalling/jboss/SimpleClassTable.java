/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.marshalling.jboss;

import java.io.IOException;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.jboss.marshalling.ClassTable;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.Unmarshaller;
import org.wildfly.clustering.marshalling.spi.IndexSerializer;
import org.wildfly.clustering.marshalling.spi.IntSerializer;

/**
 * Simple {@link ClassTable} implementation based on an array of recognized classes.
 * @author Paul Ferraro
 */
public class SimpleClassTable implements ClassTable {

    private final List<Class<?>> classes;
    private final Map<Class<?>, Integer> indexes = new IdentityHashMap<>();
    private final IntSerializer indexSerializer;

    public SimpleClassTable(Class<?>... classes) {
        this(Arrays.asList(classes));
    }

    public SimpleClassTable(List<Class<?>> classes) {
        this(IndexSerializer.select(classes.size()), classes);
    }

    private SimpleClassTable(IntSerializer indexSerializer, List<Class<?>> classes) {
        this.indexSerializer = indexSerializer;
        this.classes = classes;
        ListIterator<Class<?>> iterator = classes.listIterator();
        while (iterator.hasNext()) {
            this.indexes.putIfAbsent(iterator.next(), iterator.previousIndex());
        }
    }

    @Override
    public Writer getClassWriter(Class<?> targetClass) {
        Integer index = this.indexes.get(targetClass);
        return (index != null) ? new ClassTableWriter(index, this.indexSerializer) : null;
    }

    @Override
    public Class<?> readClass(Unmarshaller input) throws IOException {
        return this.classes.get(this.indexSerializer.readInt(input));
    }

    private static class ClassTableWriter implements ClassTable.Writer {
        private final int index;
        private final IntSerializer serializer;

        ClassTableWriter(int index, IntSerializer serializer) {
            this.index = index;
            this.serializer = serializer;
        }

        @Override
        public void writeClass(Marshaller marshaller, Class<?> clazz) throws IOException {
            this.serializer.writeInt(marshaller, this.index);
        }
    }
}
