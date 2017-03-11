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

package org.wildfly.clustering.marshalling.spi.util;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.IntFunction;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.IndexExternalizer;

/**
 * Externalizers for non-concurrent implementations of {@link Collection}.
 * @author Paul Ferraro
 */
public class CollectionExternalizer<T extends Collection<Object>> implements Externalizer<T> {

    private final Class<T> targetClass;
    private final IntFunction<T> factory;

    @SuppressWarnings("unchecked")
    CollectionExternalizer(Class<?> targetClass, IntFunction<T> factory) {
        this.targetClass = (Class<T>) targetClass;
        this.factory = factory;
    }

    @Override
    public void writeObject(ObjectOutput output, T collection) throws IOException {
        writeCollection(output, collection);
    }

    static <T extends Collection<Object>> void writeCollection(ObjectOutput output, T collection) throws IOException {
        IndexExternalizer.VARIABLE.writeData(output, collection.size());
        for (Object element : collection) {
            output.writeObject(element);
        }
    }

    @Override
    public T readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        int size = IndexExternalizer.VARIABLE.readData(input);
        return readCollection(input, this.factory.apply(size), size);
    }

    static <T extends Collection<Object>> T readCollection(ObjectInput input, T collection, int size) throws IOException, ClassNotFoundException {
        for (int i = 0; i < size; ++i) {
            collection.add(input.readObject());
        }
        return collection;
    }

    @Override
    public Class<T> getTargetClass() {
        return this.targetClass;
    }

    @MetaInfServices(Externalizer.class)
    public static class ArrayDequeExternalizer extends CollectionExternalizer<ArrayDeque<Object>> {
        public ArrayDequeExternalizer() {
            super(ArrayDeque.class, ArrayDeque::new);
        }
    }

    @MetaInfServices(Externalizer.class)
    public static class ArrayListExternalizer extends CollectionExternalizer<ArrayList<Object>> {
        public ArrayListExternalizer() {
            super(ArrayList.class, ArrayList::new);
        }
    }

    @MetaInfServices(Externalizer.class)
    public static class ConcurrentHashSetExternalizer extends CollectionExternalizer<ConcurrentHashMap.KeySetView<Object, Boolean>> {
        public ConcurrentHashSetExternalizer() {
            super(ConcurrentHashMap.KeySetView.class, capacity -> ConcurrentHashMap.newKeySet(capacity));
        }
    }

    @MetaInfServices(Externalizer.class)
    public static class ConcurrentLinkedDequeExternalizer extends CollectionExternalizer<ConcurrentLinkedDeque<Object>> {
        public ConcurrentLinkedDequeExternalizer() {
            super(ConcurrentLinkedDeque.class, size -> new ConcurrentLinkedDeque<>());
        }
    }

    @MetaInfServices(Externalizer.class)
    public static class ConcurrentLinkedQueueExternalizer extends CollectionExternalizer<ConcurrentLinkedQueue<Object>> {
        public ConcurrentLinkedQueueExternalizer() {
            super(ConcurrentLinkedQueue.class, size -> new ConcurrentLinkedQueue<>());
        }
    }

    @MetaInfServices(Externalizer.class)
    public static class HashSetExternalizer extends CollectionExternalizer<HashSet<Object>> {
        public HashSetExternalizer() {
            super(HashSet.class, HashSet::new);
        }
    }

    @MetaInfServices(Externalizer.class)
    public static class LinkedHashSetExternalizer extends CollectionExternalizer<LinkedHashSet<Object>> {
        public LinkedHashSetExternalizer() {
            super(LinkedHashSet.class, LinkedHashSet::new);
        }
    }

    @MetaInfServices(Externalizer.class)
    public static class LinkedListExternalizer extends CollectionExternalizer<LinkedList<Object>> {
        public LinkedListExternalizer() {
            super(LinkedList.class, size -> new LinkedList<>());
        }
    }
}
