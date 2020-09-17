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
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.ObjectTable;
import org.jboss.marshalling.Unmarshaller;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.IndexSerializer;
import org.wildfly.clustering.marshalling.spi.IntSerializer;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * {@link ObjectTable} implementation that dynamically loads {@link Externalizer} instances available from a given {@link ClassLoader}.
 * @author Paul Ferraro
 */
public class ExternalizerObjectTable implements ObjectTable {

    private final List<Externalizer<Object>> externalizers;
    private final Map<Class<?>, Integer> indexes = new IdentityHashMap<>();
    private final IntSerializer indexSerializer;

    public ExternalizerObjectTable(ClassLoader... loader) {
        this(loadExternalizers(loader));
    }

    private static List<Externalizer<Object>> loadExternalizers(ClassLoader... loaders) {
        List<Externalizer<Object>> loadedExternalizers = WildFlySecurityManager.doUnchecked(new PrivilegedAction<List<Externalizer<Object>>>() {
            @Override
            public List<Externalizer<Object>> run() {
                List<Externalizer<Object>> externalizers = new LinkedList<>();
                for (ClassLoader loader : loaders) {
                    for (Externalizer<Object> externalizer : ServiceLoader.load(Externalizer.class, loader)) {
                        externalizers.add(externalizer);
                    }
                }
                return externalizers;
            }
        });

        Set<DefaultExternalizerProviders> providers = EnumSet.allOf(DefaultExternalizerProviders.class);
        int size = loadedExternalizers.size();
        for (DefaultExternalizerProviders provider : providers) {
            size += provider.get().size();
        }
        List<Externalizer<Object>> result = new ArrayList<>(size);
        for (DefaultExternalizerProviders provider : providers) {
            result.addAll(provider.get());
        }
        result.addAll(loadedExternalizers);
        return result;
    }

    public ExternalizerObjectTable(List<Externalizer<Object>> externalizers) {
        this(IndexSerializer.select(externalizers.size()), externalizers);
    }

    @SafeVarargs
    public ExternalizerObjectTable(Externalizer<Object>... externalizers) {
        this(Arrays.asList(externalizers));
    }

    private ExternalizerObjectTable(IntSerializer indexSerializer, List<Externalizer<Object>> externalizers) {
        this.indexSerializer = indexSerializer;
        this.externalizers = externalizers;
        ListIterator<Externalizer<Object>> iterator = externalizers.listIterator();
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

        ExternalizerWriter(int index, IntSerializer serializer, Externalizer<Object> externalizer) {
            this.index = index;
            this.serializer = serializer;
            this.externalizer = externalizer;
        }

        @Override
        public void writeObject(Marshaller marshaller, Object object) throws IOException {
            this.serializer.writeInt(marshaller, this.index);
            this.externalizer.writeObject(marshaller, object);
        }
    }
}
