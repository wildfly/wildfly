/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.marshalling.spi.util;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntFunction;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.IndexExternalizer;

/**
 * Externalizers for implementations of {@link Set}.
 * @author Paul Ferraro
 */
public class SetExternalizer<T extends Set<Object>> implements Externalizer<T> {

    private final Class<T> targetClass;
    private final IntFunction<T> factory;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    SetExternalizer(Class targetClass, IntFunction<T> factory) {
        this.targetClass = targetClass;
        this.factory = factory;
    }

    @Override
    public void writeObject(ObjectOutput output, T set) throws IOException {
        writeSet(output, set);
    }

    static <T extends Set<Object>> void writeSet(ObjectOutput output, T set) throws IOException {
        IndexExternalizer.VARIABLE.writeData(output, set.size());
        for (Object e : set) {
            output.writeObject(e);
        }
    }

    @Override
    public T readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        int size = IndexExternalizer.VARIABLE.readData(input);
        return readSet(input, this.factory.apply(size), size);
    }

    static <T extends Set<Object>> T readSet(ObjectInput input, T set, int size) throws IOException, ClassNotFoundException {
        for (int i = 0; i < size; ++i) {
            set.add(input.readObject());
        }
        return set;
    }

    @Override
    public Class<? extends T> getTargetClass() {
        return this.targetClass;
    }

    @MetaInfServices(Externalizer.class)
    public static class ConcurrentHashSetExternalizer extends SetExternalizer<ConcurrentHashMap.KeySetView<Object, Boolean>> {
        public ConcurrentHashSetExternalizer() {
            super(ConcurrentHashMap.KeySetView.class, capacity -> ConcurrentHashMap.newKeySet(capacity));
        }
    }

    @MetaInfServices(Externalizer.class)
    public static class HashSetExternalizer extends SetExternalizer<HashSet<Object>> {
        public HashSetExternalizer() {
            super(HashSet.class, HashSet::new);
        }
    }

    @MetaInfServices(Externalizer.class)
    public static class LinkedHashSetExternalizer extends SetExternalizer<LinkedHashSet<Object>> {
        public LinkedHashSetExternalizer() {
            super(LinkedHashSet.class, LinkedHashSet::new);
        }
    }
}
