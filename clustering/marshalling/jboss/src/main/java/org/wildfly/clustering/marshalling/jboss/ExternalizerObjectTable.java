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
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.ObjectTable;
import org.jboss.marshalling.Unmarshaller;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.IndexExternalizer;

/**
 * {@link ObjectTable} implementation that dynamically loads {@link Externalizer} instances available from a given {@link ClassLoader}.
 * @author Paul Ferraro
 */
public class ExternalizerObjectTable implements ObjectTable {

    private final Externalizer<?>[] externalizers;
    private final Map<Class<?>, Writer> writers = new IdentityHashMap<>();
    private final IndexExternalizer indexExternalizer;

    public ExternalizerObjectTable(ClassLoader loader) {
        this(Stream.concat(loadExternalizers(ExternalizerObjectTable.class.getClassLoader()), loadExternalizers(loader)).toArray(Externalizer[]::new));
    }

    @SuppressWarnings("rawtypes")
    private static Stream<Externalizer> loadExternalizers(ClassLoader loader) {
        return StreamSupport.stream(ServiceLoader.load(Externalizer.class, loader).spliterator(), false);
    }

    public ExternalizerObjectTable(Externalizer<?>... externalizers) {
        this(IndexExternalizer.select(externalizers.length), externalizers);
    }

    private ExternalizerObjectTable(IndexExternalizer indexExternalizer, Externalizer<?>... externalizers) {
        this.indexExternalizer = indexExternalizer;
        this.externalizers = externalizers;
        for (int i = 0; i < externalizers.length; ++i) {
            @SuppressWarnings("unchecked")
            final Externalizer<Object> externalizer = (Externalizer<Object>) externalizers[i];
            final int index = i;
            Class<?> targetClass = externalizer.getTargetClass();
            if (!this.writers.containsKey(targetClass)) {
                Writer writer = new Writer() {
                    @Override
                    public void writeObject(Marshaller marshaller, Object object) throws IOException {
                        indexExternalizer.writeData(marshaller, index);
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
        return this.writers.get(targetClass.isEnum() ? ((Enum<?>) object).getDeclaringClass() : targetClass);
    }

    @Override
    public Object readObject(Unmarshaller unmarshaller) throws IOException, ClassNotFoundException {
        int index = this.indexExternalizer.readData(unmarshaller);
        if (index >= this.externalizers.length) {
            throw new IllegalStateException();
        }
        return this.externalizers[index].readObject(unmarshaller);
    }
}
