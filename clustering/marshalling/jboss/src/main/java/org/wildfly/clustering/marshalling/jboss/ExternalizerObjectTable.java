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

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.ObjectTable;
import org.jboss.marshalling.Unmarshaller;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.IndexSerializer;
import org.wildfly.clustering.marshalling.spi.IntSerializer;
import org.wildfly.clustering.marshalling.spi.DefaultExternalizer;

/**
 * {@link ObjectTable} implementation that dynamically loads {@link Externalizer} instances available from a given {@link ClassLoader}.
 * @author Paul Ferraro
 */
public class ExternalizerObjectTable implements ObjectTable {

    private final Externalizer<Object>[] externalizers;
    private final Map<Class<?>, Writer> writers = new IdentityHashMap<>();
    private final IntSerializer indexSerializer;

    public ExternalizerObjectTable(ClassLoader loader) {
        this(Stream.concat(EnumSet.allOf(DefaultExternalizer.class).stream(), StreamSupport.stream(ServiceLoader.load(Externalizer.class, loader).spliterator(), false))
                .toArray(Externalizer[]::new));
    }

    @SafeVarargs
    public ExternalizerObjectTable(Externalizer<Object>... externalizers) {
        this(IndexSerializer.select(externalizers.length), externalizers);
    }

    @SafeVarargs
    private ExternalizerObjectTable(IntSerializer indexSerializer, Externalizer<Object>... externalizers) {
        this.indexSerializer = indexSerializer;
        this.externalizers = externalizers;
        for (int i = 0; i < externalizers.length; ++i) {
            final Externalizer<Object> externalizer = externalizers[i];
            final int index = i;
            Class<?> targetClass = externalizer.getTargetClass();
            if (!this.writers.containsKey(targetClass)) {
                Writer writer = new Writer() {
                    @Override
                    public void writeObject(Marshaller marshaller, Object object) throws IOException {
                        indexSerializer.writeInt(marshaller, index);
                        externalizer.writeObject(marshaller, object);
                    }
                };
                this.writers.put(targetClass, writer);
            }
        }
    }

    @Override
    public Writer getObjectWriter(final Object object) throws IOException {
        Class<?> targetClass = object.getClass();
        Class<?> writerClass = targetClass.isEnum() ? ((Enum<?>) object).getDeclaringClass() : targetClass;
        Class<?> superClass = writerClass.getSuperclass();
        // If implementation class has no externalizer, search any abstract superclasses
        while (!this.writers.containsKey(writerClass) && (superClass != null) && Modifier.isAbstract(superClass.getModifiers())) {
            writerClass = superClass;
            superClass = writerClass.getSuperclass();
        }
        return this.writers.get(writerClass);
    }

    @Override
    public Object readObject(Unmarshaller unmarshaller) throws IOException, ClassNotFoundException {
        int index = this.indexSerializer.readInt(unmarshaller);
        if (index >= this.externalizers.length) {
            throw new IllegalStateException();
        }
        return this.externalizers[index].readObject(unmarshaller);
    }
}
